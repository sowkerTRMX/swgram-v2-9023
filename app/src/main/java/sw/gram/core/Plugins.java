package sw.gram.core;

import android.content.Context;
import android.webkit.WebView;
import dalvik.system.DexClassLoader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

public final class Plugins {
    public interface Api {
        String getSetting(String k, String d);
        void setSetting(String k, String v);
        void log(String s);
        void toast(String s);
        void sendMessage(long chat, String text);
        boolean ghost();
        void setGhost(boolean v);
    }

    private static File dir;
    private static WebView js;
    private static final ArrayList<String> report = new ArrayList<>();

    public static void init(Context c) {
        dir = new File(c.getExternalFilesDir(null), "plugins");
        dir.mkdirs();
    }
    public static File folder() { return dir; }
    public static ArrayList<String> report() { return report; }

    public static ArrayList<String> list() {
        ArrayList<String> o = new ArrayList<>();
        File[] f = dir.listFiles();
        if (f != null) for (File x : f) {
            String n = x.getName();
            if (n.endsWith(".js.codegram") || n.endsWith(".js") || n.endsWith(".dex") || n.endsWith(".jar")) o.add(n);
        }
        Collections.sort(o);
        return o;
    }

    public static void loadAll(final Context c) {
        report.clear();
        for (String n : list()) {
            if (Prefs.on("plug_off_" + n)) { report.add("skip " + n); continue; }
            try {
                File f = new File(dir, n);
                if (n.endsWith(".dex") || n.endsWith(".jar")) loadDex(c, f, n);
                else loadJs(c, f);
                report.add("ok " + n);
            } catch (Throwable t) { report.add("FAIL " + n + ": " + t.getMessage()); }
        }
    }

    private static void loadDex(Context c, File f, String n) throws Exception {
        File opt = new File(c.getCodeCacheDir(), "pl_" + Math.abs(n.hashCode()));
        opt.mkdirs();
        DexClassLoader cl = new DexClassLoader(f.getAbsolutePath(), opt.getAbsolutePath(), null, c.getClassLoader());
        Class<?> k = cl.loadClass(Prefs.str("plug_entry_" + n, "Plugin"));
        Method m = k.getMethod("onLoad", Api.class);
        m.invoke(null, api());
    }

    private static void loadJs(final Context c, File f) throws Exception {
        final StringBuilder b = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        String l;
        while ((l = r.readLine()) != null) b.append(l).append('\n');
        r.close();
        final String code = "(function(){try{" + b + "}catch(e){CG.log(''+e)}})();";
        android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        h.post(() -> {
            if (js == null) {
                js = new WebView(c);
                js.getSettings().setJavaScriptEnabled(true);
                js.addJavascriptInterface(api(), "CG");
                js.loadDataWithBaseURL("https://swgram.local/", "<html><body><script>" + SHIM + "</script></body></html>", "text/html", "UTF-8", null);
            }
            js.evaluateJavascript(code, null);
        });
    }

    private static final String SHIM =
        "window.CodeGram=CG;window.codegram=CG;"
      + "window.get_setting=function(k,d){return CG.getSetting(k,d||'')};"
      + "window.set_setting=function(k,v){CG.setSetting(k,String(v))};"
      + "window.log=function(s){CG.log(String(s))};"
      + "window.toast=function(s){CG.toast(String(s))};"
      + "window.send_message=function(c,t){CG.sendMessage(c,String(t))};"
      + "window.is_premium=function(){return true};";

    private static Api api() {
        return new Api() {
            @Override public String getSetting(String k, String d) { return Prefs.str(k, d); }
            @Override public void setSetting(String k, String v) { Prefs.set(k, v); }
            @Override public void log(String s) { android.util.Log.d("swgram-plugin", s); }
            @Override public void toast(String s) { Ui.toast(s); }
            @Override public void sendMessage(long chat, String text) { Mods.send(chat, text); }
            @Override public boolean ghost() { return Prefs.on("ghost"); }
            @Override public void setGhost(boolean v) { Prefs.set("ghost", v); Mods.applyGhost(); }
        };
    }
}
