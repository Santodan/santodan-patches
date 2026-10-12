package santodan.patches;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import software.santodan.extension.nuviolauncher.LauncherModels;

/** Executes the production publisher against a TvProvider fixture, including native/foreign rows. */
public final class VerifyNuvioLauncherPublisher {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        Files.createDirectories(root);
        Path directory = Files.createTempDirectory(root, "launcher-provider-");
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("android.net.Uri", "package android.net; public class Uri { public final String value; private Uri(String v){value=v;} public static Uri parse(String v){return new Uri(v);} public static Uri withAppendedPath(Uri u,String p){return parse(u.value+\"/\"+p);} public String toString(){return value;} }");
        sources.put("android.content.ContentUris", "package android.content; import android.net.Uri; public class ContentUris { public static Uri withAppendedId(Uri u,long id){return Uri.parse(u+\"/\"+id);} public static long parseId(Uri u){String s=u.toString();return Long.parseLong(s.substring(s.lastIndexOf('/')+1));} }");
        sources.put("android.content.ContentValues", "package android.content; import java.util.*; public class ContentValues { public final Map<String,String> data=new LinkedHashMap<>(); public void put(String k,String v){data.put(k,v);} public void put(String k,Long v){data.put(k,String.valueOf(v));} public void put(String k,Integer v){data.put(k,String.valueOf(v));} public String getAsString(String k){return data.get(k);} }");
        sources.put("android.database.Cursor", "package android.database; public interface Cursor extends java.io.Closeable { boolean moveToNext(); String getString(int i); long getLong(int i); void close(); }");
        sources.put("android.content.Intent", "package android.content; import java.util.*; public class Intent { public String action; public int flags; public final Map<String,String> extras=new TreeMap<>(); public Intent setAction(String a){action=a;return this;} public Intent addFlags(int f){flags|=f;return this;} public Intent putExtra(String k,String v){extras.put(k,v);return this;} public String toUri(int f){return action+\";flags=\"+flags+\";\"+extras;} }");
        sources.put("android.content.pm.PackageManager", "package android.content.pm; public class PackageManager { public boolean hasSystemFeature(String f){return true;} public android.content.Intent getLaunchIntentForPackage(String p){return new android.content.Intent();} public android.graphics.drawable.Drawable getApplicationIcon(String p){return new android.graphics.drawable.Drawable();} }");
        sources.put("android.graphics.drawable.Drawable", "package android.graphics.drawable; public class Drawable { public void setBounds(int a,int b,int c,int d){} public void draw(android.graphics.Canvas c){} }");
        sources.put("android.graphics.Canvas", "package android.graphics; public class Canvas { public Canvas(Bitmap b){} }");
        sources.put("android.graphics.Bitmap", "package android.graphics; public class Bitmap { public enum Config {ARGB_8888} public enum CompressFormat {PNG} public static Bitmap createBitmap(int w,int h,Config c){return new Bitmap();} public boolean compress(CompressFormat f,int q,java.io.OutputStream s){return true;} public void recycle(){} }");
        sources.put("android.os.Build", "package android.os; public class Build { public static class VERSION { public static int SDK_INT=26; } }");
        sources.put("android.media.tv.TvContract", "package android.media.tv; public class TvContract { public static final java.util.List<Long> requested=new java.util.ArrayList<>(); public static void requestChannelBrowsable(android.content.Context c,long id){requested.add(id);} }");
        sources.put("android.content.Context", "package android.content; public class Context { public final ContentResolver resolver=new ContentResolver(); public ContentResolver getContentResolver(){return resolver;} public String getPackageName(){return \"com.nuvio.tv\";} public android.content.pm.PackageManager getPackageManager(){return new android.content.pm.PackageManager();} }");
        sources.put("android.content.ContentResolver", "package android.content;\n" +
            "import java.util.*; import android.net.Uri; import android.database.Cursor;\n" +
            "public class ContentResolver {\n" +
            "public final Map<Long,Map<String,String>> channels=new LinkedHashMap<>(), programs=new LinkedHashMap<>();\n" +
            "public int inserts, updates, deletes; private long next=100;\n" +
            "private Map<Long,Map<String,String>> table(Uri u){return u.toString().contains(\"preview_program\")?programs:channels;}\n" +
            "public void row(boolean program,long id,String... pairs){Map<String,String> m=new LinkedHashMap<>();m.put(\"_id\",String.valueOf(id));for(int i=0;i<pairs.length;i+=2)m.put(pairs[i],pairs[i+1]);(program?programs:channels).put(id,m);}\n" +
            "public Cursor query(Uri u,String[] columns,String selection,String[] arguments,String order){\n" +
            "if(selection!=null)throw new AssertionError(\"SQL selection forbidden\");\n" +
            "List<Map<String,String>> rows=new ArrayList<>(table(u).values());\n" +
            "return new Cursor(){int index=-1;public boolean moveToNext(){return ++index<rows.size();}public String getString(int i){return rows.get(index).get(columns[i]);}public long getLong(int i){return Long.parseLong(getString(i));}public void close(){}};\n" +
            "}\n" +
            "public Uri insert(Uri u,ContentValues v){long id=next++;Map<String,String> m=new LinkedHashMap<>(v.data);m.put(\"_id\",String.valueOf(id));m.put(\"package_name\",\"com.nuvio.tv\");table(u).put(id,m);inserts++;return ContentUris.withAppendedId(u,id);}\n" +
            "public int update(Uri u,ContentValues v,String s,String[] a){table(u).get(ContentUris.parseId(u)).putAll(v.data);updates++;return 1;}\n" +
            "public int delete(Uri u,String s,String[] a){long id=ContentUris.parseId(u);table(u).remove(id);if(table(u)==channels)programs.values().removeIf(m->String.valueOf(id).equals(m.get(\"channel_id\")));deletes++;return 1;}\n" +
            "public java.io.OutputStream openOutputStream(Uri u){return new java.io.ByteArrayOutputStream();}\n" +
            "}\n" +
            "\n");
        List<String> compile = new ArrayList<>(List.of("-d", directory.toString()));
        for (var entry : sources.entrySet()) {
            Path file = directory.resolve(entry.getKey().replace('.', '/') + ".java");
            Files.createDirectories(file.getParent()); Files.writeString(file, entry.getValue()); compile.add(file.toString());
        }
        if (ToolProvider.getSystemJavaCompiler().run(null, null, null, compile.toArray(String[]::new)) != 0)
            throw new AssertionError("Android fixture compilation failed");
        List<URL> urls = new ArrayList<>(List.of(directory.toUri().toURL()));
        for (String path : System.getProperty("java.class.path").split(java.io.File.pathSeparator)) urls.add(Path.of(path).toUri().toURL());
        try (URLClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new), VerifyNuvioLauncherPublisher.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("android.") || name.startsWith("software.santodan.extension.nuviolauncher.LauncherPublisher")) {
                    Class<?> value=findLoadedClass(name); if(value==null)value=findClass(name); if(resolve)resolveClass(value); return value;
                }
                return super.loadClass(name,resolve);
            }
        }) {
            Class<?> contextType = loader.loadClass("android.content.Context");
            Object context = contextType.getConstructor().newInstance();
            Object resolver = contextType.getField("resolver").get(context);
            Class<?> resolverType = resolver.getClass();
            var row = resolverType.getMethod("row", boolean.class, long.class, String[].class);
            row.invoke(resolver,false,1L,new String[]{"internal_provider_id","com.nuvio.tv","package_name","com.nuvio.tv"});
            row.invoke(resolver,false,2L,new String[]{"internal_provider_id","santodan-launcher/library","package_name","other.app"});
            row.invoke(resolver,true,3L,new String[]{"channel_id","1","internal_provider_id","native-cw"});
            Class<?> publisherType = loader.loadClass("software.santodan.extension.nuviolauncher.LauncherPublisher");
            Object publisher = publisherType.getConstructor(contextType).newInstance(context);
            var reconcile = publisherType.getMethod("reconcile", List.class);
            var tile = new LauncherModels.Tile("tt1","movie","Movie","poster",null,null);
            var source = new LauncherModels.Source("library","Library","library",List.of(tile));
            reconcile.invoke(publisher,List.of(source));
            @SuppressWarnings("unchecked") var channels = (Map<Long,Map<String,String>>) resolverType.getField("channels").get(resolver);
            @SuppressWarnings("unchecked") var programs = (Map<Long,Map<String,String>>) resolverType.getField("programs").get(resolver);
            if(channels.size()!=3||programs.size()!=2||!channels.containsKey(1L)||!channels.containsKey(2L)||!programs.containsKey(3L))throw new AssertionError("Native or foreign rows changed");
            var program = programs.values().stream().filter(m->!"native-cw".equals(m.get("internal_provider_id"))).findFirst().orElseThrow();
            String intent = program.get("intent_uri");
            if(!intent.contains("contentId=tt1")||!intent.contains("contentType=movie")||!intent.contains("launchMode=stream")||intent.contains("videoId=")||intent.contains("name="))throw new AssertionError("Detail launch extras changed: "+intent);
            int beforeUpdates=resolverType.getField("updates").getInt(resolver);
            reconcile.invoke(publisher,List.of(source));
            if(resolverType.getField("updates").getInt(resolver)!=beforeUpdates||resolverType.getField("inserts").getInt(resolver)!=2)throw new AssertionError("Refresh recreated rows or rewrote unchanged channel/program values");
            var requests=(List<?>)loader.loadClass("android.media.tv.TvContract").getField("requested").get(null);
            if(requests.size()!=1)throw new AssertionError("Refresh overwrote launcher visibility preference");
            var dated=new LauncherModels.Tile("tt1","movie","Movie","poster",null,null,"19-Nov");
            kotlin.jvm.functions.Function1<LauncherModels.Tile,String> artwork=t->"content://com.nuvio.tv.fileprovider/cache/date.png";
            Object datePublisher=publisherType.getConstructor(contextType,kotlin.jvm.functions.Function1.class).newInstance(context,artwork);
            reconcile.invoke(datePublisher,List.of(new LauncherModels.Source("library","Library","library",List.of(dated))));
            if(!"Movie · 19-Nov".equals(program.get("title"))||!"19-Nov".equals(program.get("short_description"))||!program.get("poster_art_uri").endsWith("date.png"))throw new AssertionError("Date text/artwork not published");
            reconcile.invoke(publisher,List.of(source));
            if(!"Movie".equals(program.get("title"))||!"poster".equals(program.get("poster_art_uri"))||program.get("short_description")!=null)throw new AssertionError("Disabling a date badge left stale text/artwork");
            long ownChannel=channels.keySet().stream().filter(id->id!=1&&id!=2).findFirst().orElseThrow();
            row.invoke(resolver,false,9L,new String[]{"internal_provider_id","santodan-launcher/library","package_name","com.nuvio.tv"});
            reconcile.invoke(publisher,List.of(new LauncherModels.Source("library","Renamed library","library",List.of())));
            if(channels.containsKey(9L)||!channels.containsKey(ownChannel)||programs.size()!=1)throw new AssertionError("Empty row/duplicate cleanup failed");
            reconcile.invoke(publisher,List.of());
            if(channels.size()!=2||programs.size()!=1||!programs.containsKey(3L))throw new AssertionError("Deselect removed native/foreign channels");
            System.out.println("PASS: production TvProvider publisher: stable IDs, unchanged programs, detail intents, duplicate/stale cleanup, visibility preservation, native/foreign isolation");
        }
    }
}
