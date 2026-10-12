package santodan.patches;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import software.santodan.extension.nuviolauncher.LauncherModels;

/** Execute the beta5 production menu against native Compose boundary fixtures. */
public final class VerifyNuvioLauncherUi {
    public static void main(String[] args) throws Exception {
        Path root=Path.of(args[0]); Files.createDirectories(root);
        Path directory=Files.createTempDirectory(root,"launcher-ui-");
        Map<String,String> sources=new LinkedHashMap<>();
        sources.put("android.content.SharedPreferences", "package android.content; import java.util.*; public interface SharedPreferences { interface OnSharedPreferenceChangeListener {void onSharedPreferenceChanged(SharedPreferences p,String k);} Set<String> getStringSet(String k,Set<String> d); Editor edit(); interface Editor {Editor putStringSet(String k,Set<String> v);void apply();} class Memory implements SharedPreferences {public Set<String> selected=new HashSet<>();public Set<String> getStringSet(String k,Set<String> d){return new HashSet<>(selected);}public Editor edit(){return new Editor(){public Editor putStringSet(String k,Set<String> v){selected=new HashSet<>(v);return this;}public void apply(){}};} } }");
        sources.put("android.content.Context", "package android.content; import java.util.*; public class Context { private final Map<String,SharedPreferences> prefs=new HashMap<>(); public SharedPreferences getSharedPreferences(String n,int mode){return prefs.computeIfAbsent(n,k->new SharedPreferences.Memory());} public String getPackageName(){return \"com.nuvio.tv\";} public android.content.pm.PackageManager getPackageManager(){return new android.content.pm.PackageManager();} }");
        sources.put("android.content.pm.PackageInfo", "package android.content.pm; public class PackageInfo {public String versionName=\"1.1.0-beta.5\";}");
        sources.put("android.content.pm.PackageManager", "package android.content.pm; public class PackageManager {public PackageInfo getPackageInfo(String n,int flags){return new PackageInfo();}}");
        sources.put("android.app.Application", "package android.app; public class Application extends android.content.Context {}");
        sources.put("android.app.ActivityThread", "package android.app; public class ActivityThread {public static final Application app=new Application();public static Application currentApplication(){return app;}}");
        sources.put("android.util.Log", "package android.util; public class Log { public static int e(String t,String m,Throwable e){throw new AssertionError(m,e);} public static int w(String t,String m){return 0;} }");
        sources.put("g1.j", "package g1; public class j {public static class Composer {} public static class State {private Object value; public State(Object v){value=v;} public Object getValue(){return value;} public void setValue(Object v){value=v;}} public static State r(Object v){return new State(v);} }");
        sources.put("w1.n", "package w1; public class n {public static final Object b=new Object();}");
        sources.put("q1.s", "package q1; public class s {public final Object content;public s(int key,Object content,boolean track){this.content=content;}}");
        sources.put("sa.db", "package sa; import java.util.*; public class db {public static final List<String> events=new ArrayList<>(); public static final Map<String,Object> toggles=new HashMap<>(); public static void m(String title,String desc,Object value,Object toggle,Object modifier,Object noop,boolean b,Object icon,long l,boolean c,Object composer,int flags,int mask){events.add(title);toggles.put(title,toggle);} }");
        sources.put("sa.kc", "package sa; import java.util.*; public class kc {public static final Map<String,Object> toggles=new HashMap<>(); public static void a(String title,String desc,Object expanded,Object toggle,Object modifier,Object x,Object y,Object noop,Object body,Object composer,int flags){db.events.add(title);toggles.put(title,toggle);if(Boolean.TRUE.equals(expanded))((kotlin.jvm.functions.Function3)((q1.s)body).content).invoke(null,composer,0);} }");
        sources.put("fixture.Cached", "package fixture; public class Cached {public boolean n=true;public boolean future;}");
        sources.put("software.santodan.extension.nuvioairing.NuvioAiringSeries", "package software.santodan.extension.nuvioairing; public class NuvioAiringSeries {public static boolean effectiveHasAired(Object cached)throws Exception{return !cached.getClass().getField(\"future\").getBoolean(cached);}}");
        List<String> compile=new ArrayList<>(List.of("-classpath",System.getProperty("java.class.path"),"-d",directory.toString()));
        for(var entry:sources.entrySet()) {Path file=directory.resolve(entry.getKey().replace('.','/')+".java");Files.createDirectories(file.getParent());Files.writeString(file,entry.getValue());compile.add(file.toString());}
        if(ToolProvider.getSystemJavaCompiler().run(null,null,null,compile.toArray(String[]::new))!=0)throw new AssertionError("Fixture compilation failed");
        List<URL> urls=new ArrayList<>(List.of(directory.toUri().toURL()));
        for(String path:System.getProperty("java.class.path").split(java.io.File.pathSeparator))urls.add(Path.of(path).toUri().toURL());
        try(URLClassLoader loader=new URLClassLoader(urls.toArray(URL[]::new),VerifyNuvioLauncherUi.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(name.startsWith("android.")||name.startsWith("g1.")||name.startsWith("w1.")||name.startsWith("q1.")||name.startsWith("sa.")||name.startsWith("software.santodan.extension.nuviolauncher.NuvioLauncherChannels")) {
                    Class<?> value=findLoadedClass(name);if(value==null)value=findClass(name);if(resolve)resolveClass(value);return value;
                }
                return super.loadClass(name,resolve);
            }
        }) {
            Class<?> runtime=loader.loadClass("software.santodan.extension.nuviolauncher.NuvioLauncherChannels");
            var airedPolicy=runtime.getMethod("effectiveCachedHasAired",Object.class);
            Object cached=loader.loadClass("fixture.Cached").getConstructor().newInstance();
            if(!Boolean.TRUE.equals(airedPolicy.invoke(null,cached)))throw new AssertionError("Aired title was filtered");
            cached.getClass().getField("future").setBoolean(cached,true);
            if(!Boolean.FALSE.equals(airedPolicy.invoke(null,cached)))throw new AssertionError("Upcoming finale leaked into native Continue Watching");
            cached.getClass().getField("future").setBoolean(cached,false);
            cached.getClass().getField("n").setBoolean(cached,false);
            if(!Boolean.FALSE.equals(airedPolicy.invoke(null,cached)))throw new AssertionError("Unaired episode leaked into Continue Watching");
            MutableStateFlow<Object> profile=StateFlowKt.MutableStateFlow(1);
            var profileField=runtime.getDeclaredField("profileFlow");profileField.setAccessible(true);profileField.set(null,profile);
            var choicesField=runtime.getDeclaredField("choices");choicesField.setAccessible(true);
            choicesField.set(null,List.of(
                new LauncherModels.Source("a1","Popular · Addon A (movie)","catalog",List.of(),"addon-a","Addon A"),
                new LauncherModels.Source("a2","New · Addon A (series)","catalog",List.of(),"addon-a","Addon A"),
                new LauncherModels.Source("b1","Other · Addon B (movie)","catalog",List.of(),"addon-b","Addon B"),
                new LauncherModels.Source("t1","Watchlist · TRAKT","tracking",List.of(),"TRAKT","TRAKT"),
                new LauncherModels.Source("t2","Favorites · TRAKT","tracking",List.of(),"TRAKT","TRAKT"),
                new LauncherModels.Source("m1","MDB only · MDBLIST","tracking",List.of(),"MDBLIST","MDBLIST")
            ));
            Object composer=loader.loadClass("g1.j$Composer").getConstructor().newInstance();
            Class<?> rows=loader.loadClass("sa.db"),sections=loader.loadClass("sa.kc");
            @SuppressWarnings("unchecked") List<String> events=(List<String>)rows.getField("events").get(null);
            @SuppressWarnings("unchecked") Map<String,Object> toggles=(Map<String,Object>)sections.getField("toggles").get(null);
            var render=runtime.getMethod("renderSettings",Object.class);
            render.invoke(null,composer);
            equal(events,List.of("Library","Upcoming","Catalogs","Tracking lists"));
            invoke(toggles.get("Catalogs"));events.clear();render.invoke(null,composer);
            equal(events,List.of("Library","Upcoming","Catalogs","Addon A","Addon B","Tracking lists"));
            invoke(toggles.get("Addon A"));events.clear();render.invoke(null,composer);
            if(!events.containsAll(List.of("New (series)","Popular (movie)"))||events.contains("Other (movie)"))throw new AssertionError("Source expansion leaked other addon entries: "+events);
            invoke(toggles.get("Tracking lists"));events.clear();render.invoke(null,composer);
            if(!events.containsAll(List.of("TRAKT","MDBLIST"))||events.contains("Watchlist"))throw new AssertionError("Tracking providers start expanded");
            invoke(toggles.get("TRAKT"));events.clear();render.invoke(null,composer);
            if(!events.containsAll(List.of("Watchlist","Favorites"))||events.contains("MDB only"))throw new AssertionError("Provider expansion leaked other lists: "+events);
            @SuppressWarnings("unchecked") Map<String,Object> rowToggles=(Map<String,Object>)rows.getField("toggles").get(null);
            invoke(rowToggles.get("Popular (movie)"));
            Object app=loader.loadClass("android.app.ActivityThread").getField("app").get(null);
            Object prefs=app.getClass().getMethod("getSharedPreferences",String.class,int.class).invoke(app,"santodan_launcher_1",0);
            if(!Set.of("a1").equals(prefs.getClass().getField("selected").get(prefs)))throw new AssertionError("Individual catalog selection was not saved");
            invoke(toggles.get("Catalogs"));events.clear();render.invoke(null,composer);
            if(events.contains("Addon A")||events.contains("Popular (movie)"))throw new AssertionError("Collapsed group remains focusable");
            profile.setValue(2);events.clear();render.invoke(null,composer);
            equal(events,List.of("Library","Upcoming","Catalogs","Tracking lists"));
            System.out.println("PASS: beta5 production menu: single top-level entries, collapsed addon/provider groups, individual choices and profile isolation");
        }
    }
    private static void invoke(Object callback){((kotlin.jvm.functions.Function0<?>)callback).invoke();}
    private static void equal(Object actual,Object expected){if(!expected.equals(actual))throw new AssertionError("Expected "+expected+", got "+actual);}
}
