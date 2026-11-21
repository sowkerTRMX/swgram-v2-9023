package sw.gram.core;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static SharedPreferences sp;
    public static void init(Context c) { if (sp == null) sp = c.getApplicationContext().getSharedPreferences("swgram", Context.MODE_PRIVATE); }
    public static SharedPreferences raw() { return sp; }
    public static boolean on(String k) { return on(k, false); }
    public static boolean on(String k, boolean d) { return sp.getBoolean(k, d); }
    public static void set(String k, boolean v) { sp.edit().putBoolean(k, v).apply(); }
    public static String str(String k, String d) { return sp.getString(k, d); }
    public static void set(String k, String v) { sp.edit().putString(k, v).apply(); }
    public static int num(String k, int d) { return sp.getInt(k, d); }
    public static void set(String k, int v) { sp.edit().putInt(k, v).apply(); }
    public static long lng(String k, long d) { return sp.getLong(k, d); }
    public static void set(String k, long v) { sp.edit().putLong(k, v).apply(); }

    public static final String[] NAMES = {"swgram","CodeGram","100GRAM","MiliGram","KiloGram",
        "YourMom1000KiloGram","Pavel.gram","GiveGram","MaxOrGram","SoldGram","hahagram","NotGram",
        "Telegram","USDT>GRAM","IHAVEGRAM"};
    public static final String[] THEMES = {"dark","light","gradient","amoled","custom"};
    public static final String[] TROLL = {"typing","voice","video","round","photo"};
    public static final String[] FEATURES = {
        "Ghost Mode — read without read receipts",
        "Ghost Stories — view stories unseen",
        "Hide my typing status",
        "Message Vault — every message stored on arrival",
        "Deleted messages retained and marked",
        "Edit history with original text",
        "View-once media bypass from vault",
        "UnlimitedMode — local accounts and pins",
        "Visual pins for any chat",
        "FreeTool auto responder",
        "FreeTool last-seen tracker",
        "FreeTool voice-to-text",
        "FreeTool tabbed favourites",
        "FreeTool ad blocking",
        "sKey hidden archive",
        "Password All — 6 scopes plus per-chat",
        "PIN / pattern / password / fingerprint",
        "Trolling — 5 fake actions",
        "TestMode Jelly Button",
        "TestMode VoiceCommand orb",
        "5 themes and custom palette",
        "15 built-in app names",
        "JS and DEX plugin engine",
        "Global search over live chats and vault",
        "Feature index screen"
    };
}
