package sw.gram.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import java.security.MessageDigest;
import java.util.HashMap;

public final class Guard {
    public static final String APP = "app", SKEY = "skey", SETTINGS = "settings",
            GIFTS = "gifts", STARS = "stars", TRANSFER = "transfer";
    private static SharedPreferences sp;
    private static final HashMap<String, Boolean> open = new HashMap<>();

    public static void init(Context c) {
        if (sp == null) sp = c.getApplicationContext().getSharedPreferences("guard", Context.MODE_PRIVATE);
    }
    private static String hash(String s) {
        try {
            byte[] b = MessageDigest.getInstance("SHA-256").digest(("swgram$" + s).getBytes("UTF-8"));
            StringBuilder o = new StringBuilder();
            for (byte x : b) { String h = Integer.toHexString(x & 0xff); if (h.length() < 2) o.append('0'); o.append(h); }
            return o.toString();
        } catch (Exception e) { return s; }
    }
    public static boolean has(String k) { return sp.getString("h_" + k, null) != null; }
    public static String kind(String k) { return sp.getString("k_" + k, "pin"); }
    public static void assign(String k, String kind, String secret) {
        if (secret == null || secret.isEmpty()) { clear(k); return; }
        sp.edit().putString("h_" + k, hash(secret)).putString("k_" + k, kind).apply();
        open.put(k, true);
    }
    public static void clear(String k) { sp.edit().remove("h_" + k).remove("k_" + k).apply(); open.remove(k); }
    public static boolean check(String k, String s) {
        boolean ok = has(k) && hash(s).equals(sp.getString("h_" + k, ""));
        if (ok) open.put(k, true);
        return ok;
    }
    public static void lockAll() { open.clear(); }
    public static boolean isOpen(String k) { return !has(k) || Boolean.TRUE.equals(open.get(k)); }
    public static String chatKey(long chat) { return "chat_" + chat; }

    /** Blocking prompt. Call from a background thread. */
    public static boolean gate(final Activity a, final String scope, final String title) {
        if (isOpen(scope)) return true;
        final String[] res = {null};
        final boolean[] done = {false};
        a.runOnUiThread(() -> {
            EditText in = new EditText(a);
            in.setInputType("pin".equals(kind(scope))
                ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            in.setHint("pattern".equals(kind(scope)) ? "1-2-3-6-9" : "password");
            in.setTextColor(0xFFFFFFFF); in.setHintTextColor(0xFF8B95A5);
            FrameLayout box = new FrameLayout(a);
            box.setPadding(60, 30, 60, 0); box.addView(in);
            new AlertDialog.Builder(a).setTitle(title).setView(box).setCancelable(false)
                .setPositiveButton("Unlock", (d, w) -> { res[0] = in.getText().toString(); done[0] = true; })
                .setNegativeButton("Cancel", (d, w) -> { res[0] = null; done[0] = true; })
                .show();
        });
        long t0 = System.currentTimeMillis();
        while (!done[0] && System.currentTimeMillis() - t0 < 120000) {
            try { Thread.sleep(80); } catch (InterruptedException e) { break; }
        }
        return res[0] != null && check(scope, res[0]);
    }

    public static void recovery(Activity a, String warn) {
        new AlertDialog.Builder(a).setTitle("Password recovery").setMessage(warn)
            .setPositiveButton("Erase local data", (d, w) -> {
                Vault.wipe(); sp.edit().clear().apply(); Prefs.raw().edit().clear().apply(); open.clear();
                Ui.toast("Local data erased");
            })
            .setNegativeButton("Cancel", null).show();
    }
}
