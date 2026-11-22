package software.santodan.extension.nuviomenu;

import android.app.Application;
import software.santodan.nuvio.NuvioNative;
import android.content.SharedPreferences;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Runtime for the separately selectable label patch. Hooks are scoped to reviewed badge renderers. */
public final class NuvioLabelStyles {
    private static final ThreadLocal<Frame> FRAME = new ThreadLocal<>();
    private static final Map<String,Map<String,Object>> SAVED = new ConcurrentHashMap<>();
    private static final Map<String,long[]> COLORS = new ConcurrentHashMap<>();
    private static final Map<String,String> SAMPLES = new ConcurrentHashMap<>();
    private static final LinkedHashMap<String,String> LABELS = new LinkedHashMap<>();
    private static volatile Map<String,String> aliases = Collections.emptyMap();
    private static volatile List<Map.Entry<Pattern,String>> templates = Collections.emptyList();
    private static volatile String language = "";
    private static volatile SharedPreferences preferences;
    private static volatile Object revision;
    private static final class Frame {
        final String id; final Map<String,Object> values; boolean fontApplied; int part;
        Frame(String id,Map<String,Object> values) { this.id=id;this.values=values; }
    }
    private NuvioLabelStyles() {}
    // Only the new public patch changes this body, after the shared extension is merged.
    public static boolean available() { return false; }
    static SharedPreferences prefs() throws Exception {
        if (preferences==null) {
            Application app=(Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
            preferences=app.getSharedPreferences(LabelCustomization.SCOPE,0);
        }
        return preferences;
    }
    static void invalidate() { SAVED.clear(); }
    static Map<String,Object> saved(String id) throws Exception {
        Map<String,Object> existing=SAVED.get(id); if(existing!=null)return existing;
        Map<String,Object> values=new LinkedHashMap<>(); SharedPreferences p=prefs();
        for(String field:LabelCustomization.FIELDS) {
            String key=LabelCustomization.key(id,field); if(!p.contains(key))continue;
            try {
                Object value=field.endsWith("_color")?p.getLong(key,0L):field.equals("text")?p.getString(key,""):p.getInt(key,100);
                values.put(field,LabelCustomization.valid(field,value));
            } catch (ClassCastException|IllegalArgumentException invalid) { /* Ignore invalid imported/old values. */ }
        }
        values=Collections.unmodifiableMap(values); SAVED.put(id,values); return values;
    }
    static synchronized Map<String,String> labels() throws Exception {
        if(LABELS.isEmpty()) {
            for(int i=0;i<LabelCustomization.IDS.length;i++)LABELS.put(LabelCustomization.IDS[i],LabelCustomization.NAMES[i]);
            for(var entry:prefs().getAll().entrySet())if(entry.getKey().matches("name_[a-z][a-z0-9_]{0,95}")&&entry.getValue() instanceof String&&LABELS.size()<128)
                LABELS.put(entry.getKey().substring(5),(String)entry.getValue());
        }
        return new LinkedHashMap<>(LABELS);
    }
    private static String normalized(String text) { return text==null?"":text.trim().toUpperCase(Locale.ROOT); }
    private static synchronized void aliases() throws Exception {
        Application app=(Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
        String locale=app.getResources().getConfiguration().locale.toString();
        if(locale.equals(language)&&!aliases.isEmpty())return;
        Map<String,String> names=new LinkedHashMap<>(); List<Map.Entry<Pattern,String>> patterns=new ArrayList<>();
        for(int i=0;i<LabelCustomization.IDS.length;i++)for(int resource:LabelCustomization.RESOURCES[i]) {
            String text=normalized(app.getString(resource)); String id=LabelCustomization.IDS[i];
            addAlias(text,id,names,patterns);
        }
        for(int resource:new int[]{0x7f0f0001,0x7f0f0002})for(int quantity:new int[]{0,1,2,3,5,11})
            addAlias(normalized(app.getResources().getQuantityString(resource,quantity)),"airs_days",names,patterns);

        aliases=names; templates=patterns; language=locale;
    }
    private static void addAlias(String text,String id,Map<String,String> names,List<Map.Entry<Pattern,String>> patterns) {
        if(!text.contains("%")){names.putIfAbsent(text,id);return;}
        StringBuilder regex=new StringBuilder("^");int start=0;
        java.util.regex.Matcher m=Pattern.compile("%(?:[0-9]+\\$)?[sdf]|%%",Pattern.CASE_INSENSITIVE).matcher(text);
        while(m.find()){regex.append(Pattern.quote(text.substring(start,m.start()))).append(m.group().equals("%%")?"%":".+?");start=m.end();}
        regex.append(Pattern.quote(text.substring(start))).append('$');
        patterns.add(new AbstractMap.SimpleImmutableEntry<>(Pattern.compile(regex.toString()),id));
    }
    private static String identify(String text,boolean metadata) throws Exception {
        aliases();String value=normalized(text),id=aliases.get(value);if(id!=null)return id;
        for(var candidate:templates)if(candidate.getKey().matcher(value).matches())return candidate.getValue();
        if(metadata&&value.matches("(?:19|20)[0-9]{2}"))return observe("metadata_year","Year",text);
        if(metadata&&value.matches("[0-9]+(?:H|M| MIN| MINUTES).*"))return observe("metadata_runtime","Runtime",text);
        if(metadata&&value.matches("(?:TV-.*|PG(?:-13)?|R|G|NC-17|NR|UNRATED)"))return observe("metadata_certification","Age rating",text);
        if(text==null||text.trim().isEmpty()||text.length()>100)return null;
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder key=new StringBuilder(metadata?"metadata_":"badge_");for(int i=0;i<12;i++)key.append(String.format(Locale.ROOT,"%02x",digest[i]));
        return observe(key.toString(),(metadata?"Metadata: ":"Badge: ")+text,text);
    }
    private static synchronized String observe(String id,String title,String sample) throws Exception {
        labels();if(!LABELS.containsKey(id)) {
            if(LABELS.size()>=128)return null;
            LABELS.put(id,title);prefs().edit().putString("name_"+id,title).apply();
        }
        if(sample!=null)SAMPLES.put(id,sample);return id;
    }
    private static void begin(String text,boolean metadata) {
        FRAME.remove(); if(!available())return;
        try {
            String id=identify(text,metadata);if(id==null)return;
            SAMPLES.put(id,text);
            FRAME.set(new Frame(id,prefs().getBoolean("enabled",false)?saved(id):Collections.emptyMap()));
        } catch(Exception ignored) { /* A badge remains native if settings cannot be read. */ }
    }
    public static void begin(String text) { begin(text,false); }
    public static void beginMetadata(String text) { begin(text,true); }
    public static void beginCard(Object card) {
        try {
            String reference=NuvioNative.REF_LABEL_CW_TEXT;
            String name=reference.substring(reference.indexOf("->")+2,reference.indexOf(':'));
            Field field=card.getClass().getDeclaredField(name);field.setAccessible(true);begin((String)field.get(card),false);
        } catch(Exception ignored) { FRAME.remove(); }
    }
    public static void beginFileSize() {
        FRAME.remove();if(!available())return;
        try { FRAME.set(new Frame("file_size",prefs().getBoolean("enabled",false)?saved("file_size"):Collections.emptyMap())); }
        catch(Exception ignored) { /* Native fallback. */ }
    }
    public static long background(long original) {
        Frame frame=FRAME.get();if(frame==null)return original;
        COLORS.computeIfAbsent(frame.id,id->new long[]{original,0xffffffff00000000L})[0]=original;
        return (Long)frame.values.getOrDefault("background_color",original);
    }
    public static long foreground(long original) {
        Frame frame=FRAME.get();if(frame==null)return original;
        COLORS.computeIfAbsent(frame.id,id->new long[]{0xff1976d200000000L,original})[1]=original;
        return (Long)frame.values.getOrDefault("foreground_color",original);
    }
    public static float padding(float original) {
        Frame frame=FRAME.get();return frame==null||!frame.values.containsKey("size_percent")?original:original*((Integer)frame.values.get("size_percent"))/100f;
    }
    public static Object modifier(Object modifier) {
        Frame frame=FRAME.get();if(frame==null||!frame.values.containsKey("position"))return modifier;
        try {
            ClassLoader loader=modifier.getClass().getClassLoader();Object box=loader.loadClass("d0.v").getField("a").get(null);
            Object alignment=loader.loadClass("w1.b").getField(DateLabelStyle.alignment((Integer)frame.values.get("position"))).get(null);
            return NuvioDateLabelStyle.method(box.getClass(),"a",2).invoke(box,modifier,alignment);
        } catch(Exception ignored) { return modifier; }
    }
    public static long fontSize(long original,Object style) {
        Frame frame=FRAME.get();if(frame==null||!frame.values.containsKey("font_size_percent"))return original;
        frame.fontApplied=false;
        try {
            long value=original;
            if(value==0L) { Object span=NuvioDateLabelStyle.field(style.getClass(),"a").get(style);value=NuvioDateLabelStyle.field(span.getClass(),"b").getLong(span); }
            float size=Float.intBitsToFloat((int)value)*((Integer)frame.values.get("font_size_percent"))/100f;
            frame.fontApplied=true;
            return (value&0xffffffff00000000L)|(Float.floatToRawIntBits(size)&0xffffffffL);
        } catch(Exception ignored) { return original; }
    }
    public static String text(String original) {
        Frame frame=FRAME.get();if(frame==null)return original;
        if(frame.part++>0)return original;
        if(original!=null)SAMPLES.put(frame.id,original);
        return LabelCustomization.text((String)frame.values.get("text"),original);
    }
    public static int defaults(int original) {
        Frame frame=FRAME.get();if(frame==null)return original;
        if(frame.fontApplied)original&=~8;
        if(frame.values.containsKey("foreground_color"))original&=~4;
        return original;
    }
    public static void subscribe(Object composer) {
        try {
            if(revision==null)revision=NuvioDateLabelStyle.state(composer.getClass().getClassLoader(),"label-styles:revision",0);
            NuvioDateLabelStyle.get(revision);
        } catch(Exception ignored) { /* Does not alter the native rendering path. */ }
    }
    static void changed(ClassLoader loader) throws Exception {
        invalidate();revision=NuvioDateLabelStyle.state(loader,"label-styles:revision",0);
        NuvioDateLabelStyle.set(revision,((Number)NuvioDateLabelStyle.get(revision)).intValue()+1);
    }
    public static void end() { FRAME.remove(); }
    static LabelCustomization.Draft draft(String id) throws Exception {
        Map<String,String> labels=labels();long[] colors=COLORS.get(id);
        long background=colors==null?(id.equals("new_season")?0xffb4530900000000L:0xff1976d200000000L):colors[0];
        long foreground=colors==null?0xffffffff00000000L:colors[1];
        return new LabelCustomization.Draft(id,saved(id),background,foreground,SAMPLES.getOrDefault(id,labels.getOrDefault(id,id)));
    }
    static void save(Collection<LabelCustomization.Draft> drafts) throws Exception {
        SharedPreferences.Editor editor=prefs().edit();
        for(var draft:drafts)if(draft.dirty) {
            for(String field:LabelCustomization.FIELDS)editor.remove(LabelCustomization.key(draft.id,field));
            for(var value:draft.values().entrySet()) {
                String key=LabelCustomization.key(draft.id,value.getKey());Object v=value.getValue();
                if(v instanceof String)editor.putString(key,(String)v);else if(v instanceof Long)editor.putLong(key,(Long)v);else editor.putInt(key,(Integer)v);
            }
        }
        editor.apply();invalidate();
    }
}
