package sw.gram.core;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import sw.gram.R;

public final class Ui {
    private static final Handler H = new Handler(Looper.getMainLooper());
    public static Context app;

    public static void toast(final String s) {
        H.post(() -> { if (app != null) Toast.makeText(app, s, Toast.LENGTH_SHORT).show(); });
    }
    public static void run(Runnable r) { H.post(r); }

    public static int bg() {
        String t = Prefs.str("theme", "dark");
        if ("light".equals(t)) return 0xFFF6F7F9;
        if ("amoled".equals(t)) return 0xFF000000;
        if ("custom".equals(t)) return parse(Prefs.str("bg_custom", "#0B0F17"), 0xFF0B0F17);
        return 0xFF0B0F17;
    }
    public static int fg() { return "light".equals(Prefs.str("theme", "dark")) ? 0xFF101418 : 0xFFFFFFFF; }
    public static int dim() { return "light".equals(Prefs.str("theme", "dark")) ? 0xFF6B7280 : 0xFF8B95A5; }
    public static int accent() { return parse(Prefs.str("accent", "#2AABEE"), 0xFF2AABEE); }
    public static int parse(String s, int def) { try { return Color.parseColor(s); } catch (Exception e) { return def; } }

    public static TextView text(Context c, String s, float size, int color) {
        TextView t = new TextView(c);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        return t;
    }
    public static TextView header(Context c, String s) {
        TextView t = text(c, s.toUpperCase(), 12, accent());
        t.setTypeface(null, Typeface.BOLD);
        t.setPadding(44, 40, 44, 12);
        return t;
    }
    public static TextView note(Context c, String s) {
        TextView t = text(c, s, 12, dim());
        t.setPadding(44, 4, 44, 18);
        return t;
    }
    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundColor(bg());
        return l;
    }
    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(0x14808080);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 1);
        lp.leftMargin = 44;
        v.setLayoutParams(lp);
        return v;
    }
    public static void pad(View v, int l, int t, int r, int b) { v.setPadding(l, t, r, b); }
    public static LinearLayout.LayoutParams match() { return new LinearLayout.LayoutParams(-1, -2); }
    public static LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(-1, 0, 1f); }
    public static int dp(Context c, int v) { return (int) (v * c.getResources().getDisplayMetrics().density); }
}
