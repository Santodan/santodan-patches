package santodan.patches

import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import javax.tools.ToolProvider

/** Run the production native UI bridge with real callbacks and an isolated fake HTTPS transport. */
fun main(args: Array<String>) {
    val root = Files.createTempDirectory(File(args[1]).apply { mkdirs() }.toPath(), "introdb-native-runtime-").toFile()
    val sources = mapOf(
        "android.content.SharedPreferences" to """package android.content; public class SharedPreferences {
            public java.util.Map<String,Object> values=new java.util.HashMap<>();
            public boolean getBoolean(String k,boolean d){return (Boolean)values.getOrDefault(k,d);}
            public String getString(String k,String d){return (String)values.getOrDefault(k,d);}
            public Editor edit(){return new Editor();} public class Editor {
                public Editor putBoolean(String k,boolean v){values.put(k,v);return this;}
                public Editor putString(String k,String v){values.put(k,v);return this;} public void apply(){} }
        }""",
        "android.app.Application" to """package android.app; public class Application {
            public android.content.SharedPreferences prefs=new android.content.SharedPreferences();
            public android.content.SharedPreferences getSharedPreferences(String n,int m){return prefs;}
        }""",
        "android.app.Activity" to """package android.app; public class Activity {
            public static Application app=new Application(); public Application getApplicationContext(){return app;}
        }""",
        "android.app.ActivityThread" to """package android.app; public class ActivityThread {public static Application currentApplication(){return Activity.app;}}""",
        "android.util.Log" to """package android.util; public class Log {
            public static int d(String tag,String message){fixture.Ui.logs.add(message);return 0;}
            public static int w(String tag,String message){fixture.Ui.logs.add(message);return 0;}
            public static int e(String tag,String message,Throwable error){throw new AssertionError(message,error);}
        }""",
        "android.os.Looper" to """package android.os; public class Looper {public static Looper getMainLooper(){return new Looper();}}""",
        "android.os.Handler" to """package android.os; public class Handler {
            public static java.util.concurrent.BlockingQueue<Runnable> queue=new java.util.concurrent.LinkedBlockingQueue<>();
            public Handler(Looper l){} public boolean post(Runnable r){queue.add(r);return true;}
        }""",
        "kotlin.jvm.functions.Function0" to """package kotlin.jvm.functions; public interface Function0 {Object invoke();}""",
        "kotlin.jvm.functions.Function1" to """package kotlin.jvm.functions; public interface Function1 {Object invoke(Object a);}""",
        "kotlin.jvm.functions.Function3" to """package kotlin.jvm.functions; public interface Function3 {Object invoke(Object a,Object b,Object c);}""",
        "kotlin.Unit" to """package kotlin; public class Unit {public static final Unit INSTANCE=new Unit();}""",
        "h1.d3" to """package h1; public interface d3 {Object getValue();}""",
        "h1.k1" to """package h1; public interface k1 extends d3 {void setValue(Object v);}""",
        "h1.t1" to """package h1; public class t1 implements k1 {Object v;public t1(Object v){this.v=v;}public Object getValue(){return v;}public void setValue(Object v){this.v=v;}}""",
        "h1.h" to """package h1; public class h {public static t1 s(Object v){return new t1(v);}}""",
        "h1.l0" to """package h1; public class l0 {public int depth;public Object j(Object key){return new y2.n2();}public void d0(int k){depth++;}public void p(boolean v){if(--depth<0)throw new AssertionError();}}""",
        "x1.n" to """package x1; public class n {public static final Object b=new Object();}""",
        "c2.a0" to """package c2; public class a0 {}""",
        "m3.n" to """package m3; public class n {}""",
        "r1.s" to """package r1; public class s {Object callback;public s(int k,Object c,boolean tracked){callback=c;}
            public void render(Object composer){((kotlin.jvm.functions.Function3)callback).invoke(new Object(),composer,0);}}""",
        "za.d" to """package za;public class d {public final String a,b;public d(String label,String value){a=label;b=value;}}""",
        "za.a" to """package za;import fixture.Ui;import kotlin.jvm.functions.*;public class a {
            public static void b(Object modifier,String title,String value,String selected,boolean expanded,java.util.List<d> options,Function1 expand,Function1 select,boolean blocked,Object composer,int flags){
                if(!title.equals("Segment type")||!value.equals(selected)||options.size()!=3)throw new AssertionError();
                Ui.expand=expand;Ui.expanded=expanded;Ui.select=select;Ui.dropdownBlocked=blocked;}
        }""",
        "fixture.Ui" to """package fixture; import kotlin.jvm.functions.*;public class Ui {
            public static java.util.Map<String,Function0> actions=new java.util.HashMap<>();
            public static java.util.Map<String,Boolean> enabled=new java.util.HashMap<>();
            public static java.util.Map<String,Integer> actionRows=new java.util.HashMap<>();public static int row,nextRow;
            public static java.util.List<String> labels=new java.util.ArrayList<>();
            public static java.util.List<String> logs=new java.util.ArrayList<>();
            public static Object icon;public static String nativeText;
            public static Function0 toggle,keyRow,button,dismiss; public static Function1 select,expand;public static boolean expanded,dropdownBlocked;
            public static String buttonLabel,title,subtitle,keyStatus;public static boolean setting;
            public static void clear(){actions.clear();enabled.clear();labels.clear();button=null;title=null;select=null;}
            public static void click(String name){if(Boolean.TRUE.equals(enabled.get(name)))actions.get(name).invoke();}
        }""",
        "ab.uh" to """package ab; import kotlin.jvm.functions.*;import fixture.Ui; public class uh {
            public static void m(String t,String s,boolean v,Function0 c,Object mod,Function0 f,boolean x,Object i,long l,boolean z,Object co,int flags,int defaults){Ui.toggle=c;Ui.setting=v;}
            public static void a(String t,String s,String v,Function0 c,Object m,Object f,boolean e,Object i,Object j,long l,Object k,Object n,float x,long color,Object co,int a,int b,int mask){
                if(mask!=16368)throw new AssertionError();Ui.keyRow=c;Ui.keyStatus=v;}
            public static void d(String t,Function0 c,boolean primary,boolean enabled,Object co,int a,int b){Ui.actions.put(t,c);Ui.enabled.put(t,enabled);Ui.actionRows.put(t,Ui.row);}
            public static void e(Object a,r1.s content,Object co,int flags,int defaults){int previous=Ui.row;Ui.row=++Ui.nextRow;content.render(co);Ui.row=previous;}
            public static void j(String t,java.util.List<?> opts,Object value,Function1 select,Function0 dismiss,Object sub,float w,float h,Object co,int flags,int defaults){Ui.select=select;Ui.dismiss=dismiss;}
        }""",
        "ab.kj" to """package ab; public class kj {public static void b(String t,Object m,Object style,Object co,int a,int b){fixture.Ui.labels.add(t);}}""",
        "ab.n0" to """package ab;import kotlin.jvm.functions.*;public class n0 implements Function3 {
            public static h1.k1 text;public static c2.a0 focus;
            public n0(c2.a0 f,y2.n2 keyboard,h1.k1 v,h1.k1 focused,int branch){if(branch!=0||keyboard==null)throw new AssertionError();text=v;focus=f;}
            public Object invoke(Object scope,Object composer,Object flags){return kotlin.Unit.INSTANCE;}
        }""",
        "ab.d" to """package ab;public class d implements kotlin.jvm.functions.Function1 {public d(int branch,h1.k1 state){if(branch!=2)throw new AssertionError();}public Object invoke(Object focus){return kotlin.Unit.INSTANCE;}}""",
        "ab.m0" to """package ab;public class m0 implements kotlin.jvm.functions.Function0 {public m0(c2.a0 f,int branch){if(branch!=0)throw new AssertionError();}public Object invoke(){return kotlin.Unit.INSTANCE;}}""",
        "ab.q1" to """package ab;public class q1 {
            static r1.s text(String label){return new r1.s(1,(kotlin.jvm.functions.Function3)(x,y,z)->{fixture.Ui.nativeText=label;return kotlin.Unit.INSTANCE;},true);}
            public static final r1.s a=text("Cancel"),b=text("Clear");
        }""",
        "ab.p0" to """package ab;public class p0 implements kotlin.jvm.functions.Function3 {
            public p0(h1.d3 value,int branch){if(branch!=0||(Boolean)value.getValue())throw new AssertionError();}
            public Object invoke(Object scope,Object composer,Object flags){fixture.Ui.nativeText="Save";return kotlin.Unit.INSTANCE;}
        }""",
        "y2.m1" to """package y2;public class m1 {public static final Object q=new Object();}""",
        "y2.n2" to """package y2;public class n2 {}""",
        "androidx.compose.foundation.layout.b" to """package androidx.compose.foundation.layout;public class b {public static Object f(Object m,float fraction){return m;}}""",
        "c2.d" to """package c2;public class d {public static Object i(Object m,Object callback){return m;}}""",
        "a.a" to """package a;public class a {public static db.e y(Object composer){return new db.e();}public static db.m A(Object composer){return new db.m();}}""",
        "db.e" to """package db;public class e {public long b=1,c=2,l=3,x=4;}""",
        "db.l0" to """package db;public class l0 {public static final l0 a=new l0();public float b=1,c=2;}""",
        "db.m" to """package db;public class m {public y.v a(float width,float alpha){return new y.v();}}""",
        "y.n" to """package y;public class n {public static v a(long color,float width){return new v();}}""",
        "y.v" to """package y;public class v {}""",
        "e2.w0" to """package e2;public interface w0 {}""",
        "l0.f" to """package l0;public class f implements e2.w0 {}""",
        "l0.g" to """package l0;public class g {public static f b(float radius){if(radius!=10)throw new AssertionError();return new f();}}""",
        "y5.c" to """package y5;public class c {public c(y.v stroke,e2.w0 shape,int flags){if(flags!=2)throw new AssertionError();}}""",
        "y5.m" to """package y5;public class m {
            public static Object b(long bg,long focused,Object composer,int flags,int defaults){if(bg!=1||focused!=1||defaults!=58)throw new AssertionError();return new Object();}
            public static Object a(c border,c focused,Object composer,int flags){return new Object();}
            public static Object e(Object shape,int flags){return shape;}public static Object d(float scale,int flags){return new Object();}
        }""",
        "y5.f" to """package y5;public class f {public static Object b(long bg,long fg,long a,long b,long c,long d,Object composer,int defaults){if(fg!=3||defaults!=252)throw new AssertionError();return new Object();}}""",
        "y5.g1" to """package y5;import kotlin.jvm.functions.*;public class g1 {
            public static void c(Function0 click,Object modifier,Object longClick,Object shape,Object colors,Object scale,Object border,Object glow,Object interaction,r1.s body,Object composer,int flags,int defaults){
                if(border==null||defaults!=388)throw new AssertionError();body.render(composer);}
            public static void a(Function0 click,Object modifier,boolean enabled,Object shape,Object scale,Object colors,Object border,Object padding,r1.s text,Object composer,int flags,int flags2,int defaults){
                if(defaults!=3966)throw new AssertionError();text.render(composer);fixture.Ui.actions.put(fixture.Ui.nativeText,click);fixture.Ui.enabled.put(fixture.Ui.nativeText,true);}
        }""",
        "ha.m2" to """package ha;import kotlin.jvm.functions.*;public class m2 {
            public static void z(Function0 d,String title,String sub,float width,int align,boolean suppress,boolean platform,Object brush,Object border,float a,float b,float c,float e,Object bg,r1.s content,Object composer,int f,int f2,int defaults){
                if(title.equals("IntroDB API key")?(defaults!=16368||width!=700):defaults!=16336)throw new AssertionError();fixture.Ui.title=title;fixture.Ui.subtitle=sub;fixture.Ui.dismiss=d;content.render(composer);}
        }""",
        "k2.e" to """package k2;public class e {public final String name;public e(String n){name=n;}}""",
        "k2.d" to """package k2;public class d {String name;public d(String n,float a,float b,float c,float e,long color,int tint,boolean mirror,int flags){name=n;}
            public static void a(d vector,java.util.ArrayList<?> path,int fill,e2.y0 brush){if(path.size()<20)throw new AssertionError();}public e b(){return new e(name);}}
        """,
        "e2.z" to """package e2;public class z {public static final long b=0L;}""",
        "e2.y0" to """package e2;public class y0 {public y0(long color){}}""",
        "io.sentry.d2" to """package io.sentry;public class d2 {public java.util.ArrayList<Object> b=new java.util.ArrayList<>();public d2(int mode){}
            public void o(float x,float y){b.add(new float[]{x,y});}public void m(float x,float y){b.add(new float[]{x,y});}public void e(){b.add("close");}}
        """,
        "va.yd" to """package va;import kotlin.jvm.functions.*;public class yd {
            public static void b(Object i,Object painter,String label,Function0 click,Object focus,Object up,boolean enabled,Object down,Object focused,long color,Object composer,int flags,int defaults){
                if(up==null||down==null||focused==null||defaults!=530)throw new AssertionError();fixture.Ui.icon=i;fixture.Ui.button=enabled?click:null;fixture.Ui.buttonLabel=label;}
        }""",
        "kotlinx.coroutines.Job" to """package kotlinx.coroutines;public class Job {public boolean cancelled;
            public static class DefaultImpls {public static void cancel${'$'}default(Job j,java.util.concurrent.CancellationException e,int flags,Object m){j.cancelled=true;}
                public static boolean cancel${'$'}default(Job j,Throwable e,int flags,Object m){throw new AssertionError("Wrong legacy cancellation overload");}}
        }""",
        "kotlinx.coroutines.flow.StateFlow" to """package kotlinx.coroutines.flow;public class StateFlow {
            public Object value;public StateFlow(Object v){value=v;}public Object getValue(){return value;}
        }""",
        "va.g3" to """package va;public class g3 {public boolean d;}""",
        "va.k8" to """package va;public class k8 {
            public Long position=0L;public int reads,timers;public kotlinx.coroutines.Job T0=new kotlinx.coroutines.Job();
            public kotlinx.coroutines.flow.StateFlow H0=new kotlinx.coroutines.flow.StateFlow(new g3());
        }""",
        "va.z9" to """package va;public class z9 {public static Long e(k8 c){c.reads++;return c.position;}}""",
        "va.sa" to """package va;public class sa {public static void F(k8 c){c.timers++;c.T0=new kotlinx.coroutines.Job();}}""",
        "va.ue" to """package va;public class ue {public k8 Y=new k8();}""",
        "va.re" to """package va;public class re {public Integer x0=2,y0=3;public String z0="tt12345:2:3",i="https://video.invalid/cut1";}""",
        "va.bd" to """package va;public class bd {public ue c=new ue();public re e=new re();public Object o=new Object();
            public kotlin.jvm.functions.Function0 p=()->kotlin.Unit.INSTANCE,q=()->kotlin.Unit.INSTANCE;}
        """,
        "fixture.Runner" to """package fixture;
            import android.app.*;import android.os.*;import software.santodan.extension.nuviointro.NuvioIntroDb;
            import java.net.*;import java.io.*;import java.util.concurrent.*;
            public class Runner {
                static int requests;static volatile int response=200;static volatile boolean block,disconnected,background;
                static volatile String payload;static volatile CountDownLatch entered;static h1.l0 composer=new h1.l0();
                static void check(boolean v){if(!v)throw new AssertionError();}
                static void settings()throws Exception{Ui.clear();NuvioIntroDb.renderSettings(composer);check(composer.depth==0);}
                static void render(va.bd owner){Ui.clear();NuvioIntroDb.renderControl(owner,composer);check(composer.depth==0);}
                static void complete()throws Exception{Runnable r=Handler.queue.poll(5,TimeUnit.SECONDS);check(r!=null);r.run();}
                static void capture(va.bd owner){owner.c.Y.position=12345L;render(owner);check(((k2.e)Ui.icon).name.endsWith(".0"));Object first=Ui.icon;
                    Ui.button.invoke();render(owner);check(Ui.title==null&&Ui.buttonLabel.contains("mark end")&&Ui.icon!=first&&((k2.e)Ui.icon).name.endsWith(".1"));
                    owner.c.Y.position=23456L;Ui.button.invoke();render(owner);check(Ui.title.equals("Submit segment to IntroDB")&&((k2.e)Ui.icon).name.endsWith(".2"));}
                public static void run()throws Exception{
                    Thread ui=Thread.currentThread();URL.setURLStreamHandlerFactory(protocol->protocol.equals("https")?new URLStreamHandler(){
                        protected URLConnection openConnection(URL u){return new HttpURLConnection(u){ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                            public void connect(){}public boolean usingProxy(){return false;}public void disconnect(){disconnected=true;block=false;}
                            public OutputStream getOutputStream(){background=Thread.currentThread()!=ui;return bytes;}
                            public InputStream getErrorStream(){return new ByteArrayInputStream("{\"error\":\"intro duration out of range\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
                            public int getResponseCode()throws IOException{requests++;payload=bytes.toString("UTF-8");if(entered!=null)entered.countDown();
                                long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(block&&System.nanoTime()<until)try{Thread.sleep(5);}catch(InterruptedException e){throw new IOException(e);}return response;}
                        };}
                    }:null);
                    Activity activity=new Activity();NuvioIntroDb.resume(activity);va.bd owner=new va.bd();
                    settings();check(!Ui.setting&&Ui.keyStatus.equals("Not set"));render(owner);check(Ui.button==null&&owner.c.Y.reads==0&&requests==0);
                    settings();Ui.keyRow.invoke();settings();check(Ui.title.equals("IntroDB API key")&&Ui.actions.keySet().containsAll(java.util.List.of("Cancel","Clear","Save")));
                    ab.n0.text.setValue("test-key");Ui.click("Save");
                    settings();check(Ui.keyStatus.equals("Configured"));Ui.toggle.invoke();settings();check(Ui.setting);
                    capture(owner);check(owner.c.Y.reads==2&&owner.c.Y.T0.cancelled);check(Ui.labels.contains("Start: 00:00:12.345")&&Ui.labels.contains("End: 00:00:23.456"));
                    check(Ui.actionRows.get("Submit").equals(Ui.actionRows.get("Cancel")));
                    Ui.expand.invoke(true);render(owner);check(Ui.expanded&&Ui.title.equals("Submit segment to IntroDB"));
                    Ui.expand.invoke(false);render(owner);check(!Ui.expanded&&Ui.title.equals("Submit segment to IntroDB"));
                    Ui.expand.invoke(true);render(owner);Ui.select.invoke(new za.d("outro","outro"));render(owner);check(!Ui.expanded&&Ui.labels.contains("Segment: outro"));
                    Ui.click("Submit");Ui.click("Submit");complete();render(owner);check(requests==1&&background&&payload.contains("\"start_ms\":12345")&&payload.contains("\"end_sec\":23.456"));
                    check(payload.contains("\"segment_type\":\"outro\"")&&!Ui.enabled.get("Submit"));Ui.click("Submit");check(requests==1);Ui.click("Close");check(owner.c.Y.timers==1);
                    capture(owner);response=401;Ui.click("Submit");complete();render(owner);check(Ui.labels.stream().anyMatch(x->x.contains("rejected the API key"))&&Ui.enabled.get("Submit"));
                    response=500;Ui.click("Submit");complete();render(owner);check(Ui.labels.stream().anyMatch(x->x.contains("HTTP 500")));
                    response=400;Ui.click("Submit");complete();render(owner);check(Ui.labels.stream().anyMatch(x->x.contains("intro duration out of range"))&&Ui.enabled.get("Submit"));
                    block=true;disconnected=false;entered=new CountDownLatch(1);Ui.click("Submit");check(entered.await(5,TimeUnit.SECONDS));NuvioIntroDb.pause(activity);complete();check(disconnected);
                    NuvioIntroDb.resume(activity);render(owner);check(Ui.title==null&&Ui.buttonLabel.contains("mark start"));
                    Ui.button.invoke();render(owner);check(Ui.buttonLabel.contains("mark end"));owner.e.z0="tt12345:2:4";owner.e.y0=4;render(owner);check(Ui.buttonLabel.contains("mark start"));
                    Ui.button.invoke();owner.e.i="https://video.invalid/cut2";render(owner);check(Ui.buttonLabel.contains("mark start"));
                    Ui.button.invoke();settings();Ui.toggle.invoke();render(owner);check(Ui.button==null);
                    settings();Ui.keyRow.invoke();settings();ab.n0.text.setValue("unsaved-key");Ui.click("Cancel");settings();check(Ui.keyStatus.equals("Configured"));
                    Ui.keyRow.invoke();settings();Ui.click("Clear");settings();check(Ui.keyStatus.equals("Not set"));
                    Ui.toggle.invoke();render(owner);Ui.button.invoke();render(owner);check(Ui.title.equals("Submit segment to IntroDB")&&Ui.labels.stream().anyMatch(x->x.contains("Settings"))&&!Ui.enabled.get("Submit"));
                    Ui.click("Cancel");((va.g3)owner.c.Y.H0.value).d=true;render(owner);check(Ui.button==null);
                    NuvioIntroDb.pause(activity);check(composer.depth==0);
                    check(Ui.logs.stream().noneMatch(x->x.contains("test-key")||x.contains("unsaved-key")));
                }
            }
        """
    )
    val files = sources.map { (name, code) -> File(root, name.replace('.', '/') + ".java").apply { parentFile.mkdirs(); writeText(code) } } +
        File(args[0]).listFiles()!!.filter { it.extension == "java" }
    val output = File(root, "classes").apply { mkdirs() }
    val jsonJar = org.json.JSONObject::class.java.protectionDomain.codeSource.location
    check(ToolProvider.getSystemJavaCompiler().run(null, null, null, "--release", "11", "-classpath", File(jsonJar.toURI()).path, "-d", output.path, *files.map { it.path }.toTypedArray()) == 0)
    URLClassLoader(arrayOf(output.toURI().toURL(), jsonJar), ClassLoader.getSystemClassLoader().parent).use {
        it.loadClass("fixture.Runner").getMethod("run").invoke(null)
    }
    println("PASS: Anime Skip single-line key editor and Cancel/Clear/Save, three distinct segment icons, exact cancellation overload with legacy overload trap, two-press review, native actions, timer lifecycle, HTTPS failures/cancellation, credential-safe logging and balanced Compose groups")
}
