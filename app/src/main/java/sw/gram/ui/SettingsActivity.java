package sw.gram.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Arrays;
import sw.gram.R;
import sw.gram.core.Guard;
import sw.gram.core.Mods;
import sw.gram.core.Plugins;
import sw.gram.core.Prefs;
import sw.gram.core.SKey;
import sw.gram.core.Ui;
import sw.gram.core.Vault;
import sw.gram.view.JellyButton;

public class SettingsActivity extends Activity {
    private LinearLayout body;
    private ScrollView scroll;
    private String tab = "home";

    private static final String[][] TABS = {
        {"privacy", "Privacy", "Ghost mode, Password All, sKey"},
        {"messages", "Messages", "Vault, deleted, edited, view-once"},
        {"freetool", "FreeTool", "Auto-reply, presence, voice, tabs, ads"},
        {"trolling", "Trolling", "Fake typing / voice / video / round / photo"},
        {"testmode", "TestMode", "Jelly Button, VoiceCommand orb"},
        {"look", "Appearance", "Themes, palette, 15 names"},
        {"plugins", "Plugins", ".js.codegram and .dex"},
        {"index", "All features", "Complete feature index"},
        {"about", "About", "Version, storage, reset"}
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.column(this);
        setContentView(root);
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(36, 34, 36, 26);
        TextView back = Ui.text(this, "\u2039", 30, Ui.accent());
        back.setPadding(0, 0, 26, 0);
        back.setOnClickListener(v -> { if ("home".equals(tab)) finish(); else open("home"); });
        head.addView(back);
        TextView t = Ui.text(this, Prefs.str("app_name", getString(R.string.app_name)), 18, Ui.fg());
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        head.addView(t);
        root.addView(head, Ui.match());
        root.addView(Ui.divider(this));
        scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, 0, 0, 80);
        scroll.addView(body);
        root.addView(scroll, Ui.weight());
        open("home");
    }

    private void open(String t) {
        tab = t;
        body.removeAllViews();
        scroll.scrollTo(0, 0);
        switch (t) {
            case "privacy": privacy(); break;
            case "messages": messages(); break;
            case "freetool": freetool(); break;
            case "trolling": trolling(); break;
            case "testmode": testmode(); break;
            case "look": look(); break;
            case "plugins": plugins(); break;
            case "index": index(); break;
            case "about": about(); break;
            default: home();
        }
    }

    private void home() {
        body.addView(Ui.header(this, "Settings"));
        for (final String[] x : TABS) row(x[1], x[2], v -> open(x[0]));
    }

    private void row(String title, String sub, View.OnClickListener cl) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(44, 26, 44, 26);
        r.addView(Ui.text(this, title, 16, Ui.fg()));
        if (sub != null && !sub.isEmpty()) {
            TextView s = Ui.text(this, sub, 12, Ui.dim());
            s.setPadding(0, 5, 0, 0);
            r.addView(s);
        }
        if (cl != null) r.setOnClickListener(cl);
        body.addView(r);
        body.addView(Ui.divider(this));
    }

    private void toggle(final String key, String title, String sub, boolean def) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(44, 22, 34, 22);
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.addView(Ui.text(this, title, 16, Ui.fg()));
        if (sub != null) {
            TextView s = Ui.text(this, sub, 12, Ui.dim());
            s.setPadding(0, 5, 0, 0);
            tx.addView(s);
        }
        r.addView(tx, new LinearLayout.LayoutParams(0, -2, 1f));
        final Switch sw = new Switch(this);
        sw.setChecked(Prefs.on(key, def));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean c) {
                Prefs.set(key, c);
                if ("ghost".equals(key)) Mods.applyGhost();
                if ("trolling".equals(key)) Mods.startTroll(Prefs.lng("troll_chat", 0));
            }
        });
        r.addView(sw);
        r.setOnClickListener(v -> sw.toggle());
        body.addView(r);
        body.addView(Ui.divider(this));
    }

    private void field(String label, final String key, String def, boolean numeric) {
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(44, 14, 44, 14);
        w.addView(Ui.text(this, label, 12, Ui.dim()));
        final EditText e = new EditText(this);
        e.setText(Prefs.str(key, def));
        e.setTextColor(Ui.fg());
        e.setTextSize(15);
        e.setBackgroundResource(R.drawable.input);
        e.setPadding(28, 22, 28, 22);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setOnFocusChangeListener((v, f) -> { if (!f) Prefs.set(key, e.getText().toString()); });
        w.addView(e);
        body.addView(w);
    }

    private void chips(String[] labels, String[] values, final String key, String def) {
        LinearLayout w = new LinearLayout(this);
        w.setPadding(34, 10, 34, 16);
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        String cur = Prefs.str(key, def);
        for (int i = 0; i < labels.length; i++) {
            final String val = values[i];
            TextView c = Ui.text(this, labels[i], 13, val.equals(cur) ? 0xFFFFFFFF : Ui.dim());
            c.setBackgroundResource(val.equals(cur) ? R.drawable.chip_on : R.drawable.chip);
            c.setPadding(30, 16, 30, 16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = 10;
            c.setLayoutParams(lp);
            c.setOnClickListener(v -> { Prefs.set(key, val); open(tab); });
            w.addView(c);
        }
        hs.addView(w);
        body.addView(hs);
    }

    private void button(String s, boolean danger, View.OnClickListener cl) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(0xFFFFFFFF);
        b.setBackgroundResource(danger ? R.drawable.chip : R.drawable.btn);
        if (danger) b.setBackgroundColor(0xFFE5484D);
        b.setStateListAnimator(null);
        LinearLayout.LayoutParams lp = Ui.match();
        lp.setMargins(44, 16, 44, 6);
        b.setLayoutParams(lp);
        b.setOnClickListener(cl);
        body.addView(b);
    }

    private void privacy() {
        body.addView(Ui.header(this, "Ghost mode"));
        toggle("ghost", "Ghost mode", "Read without receipts, appear offline", false);
        toggle("ghost_stories", "Ghost stories", "View stories unseen", true);
        toggle("ghost_typing", "Hide my typing", "Suppress outgoing typing status", false);
        body.addView(Ui.note(this, "Messages are rendered from the local vault. Read confirmations are never sent."));

        body.addView(Ui.header(this, "Password All"));
        body.addView(Ui.note(this, getString(R.string.pw_warn)));
        pw(Guard.APP, "App lock");
        pw(Guard.SETTINGS, "Settings lock");
        pw(Guard.GIFTS, "Gift sending");
        pw(Guard.STARS, "Star reactions");
        pw(Guard.TRANSFER, "Gift transfer");
        pw(Guard.SKEY, "sKey archive");
        button("Recover / erase passwords", true, v -> Guard.recovery(this, getString(R.string.pw_warn)));

        body.addView(Ui.header(this, "sKey archive"));
        toggle("skey_enabled", "Enable sKey", "Hidden, muted, excluded from search", false);
        row("Stored chats", SKey.list().size() + " chats hidden", null);
        toggle("skey_reveal", "Temporarily reveal", "Show archived chats in the list", false);
    }

    private void pw(final String scope, String label) {
        row(label, Guard.has(scope) ? Guard.kind(scope) + " configured" : "not set", v -> {
            final String[] kinds = {"pin", "pattern", "password"};
            new AlertDialog.Builder(this).setTitle(label).setItems(kinds, (d, which) -> {
                final EditText in = new EditText(this);
                in.setTextColor(Ui.fg());
                in.setHint(which == 1 ? "1-2-3-6-9" : "secret");
                in.setInputType(which == 0
                    ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                FrameLayout box = new FrameLayout(this);
                box.setPadding(60, 24, 60, 0);
                box.addView(in);
                new AlertDialog.Builder(this).setTitle(label)
                    .setMessage(getString(R.string.pw_warn)).setView(box)
                    .setPositiveButton("Set", (d2, w2) -> {
                        Guard.assign(scope, kinds[which], in.getText().toString());
                        Ui.toast("Password set");
                        open(tab);
                    })
                    .setNeutralButton("Remove", (d2, w2) -> { Guard.clear(scope); open(tab); })
                    .setNegativeButton("Cancel", null).show();
            }).show();
        });
    }

    private void messages() {
        body.addView(Ui.header(this, "Message vault"));
        toggle("vault", "Save every message", "Stored the instant it arrives", true);
        toggle("keep_deleted", "Keep deleted messages", "Marked, never removed", true);
        toggle("keep_edited", "Keep edit history", "Original text preserved", true);
        row("Open vault search", "Everything ever received", v -> startActivity(new Intent(this, SearchActivity.class)));
        body.addView(Ui.header(this, "One-time media"));
        toggle("view_once", "View-once bypass", "Expired media stays readable from vault", false);
        body.addView(Ui.header(this, "UnlimitedMode"));
        toggle("unlimited", "UnlimitedMode", "Local account slots and visual pins", false);
        row("Visual pins", Vault.pins().size() + " chats pinned locally", null);
        body.addView(Ui.header(this, "Storage"));
        button("Erase vault", true, v -> new AlertDialog.Builder(this)
            .setTitle("Erase vault").setMessage("All locally stored messages will be removed.")
            .setPositiveButton("Erase", (d, w) -> { Vault.wipe(); Ui.toast("Vault erased"); })
            .setNegativeButton("Cancel", null).show());
    }

    private void freetool() {
        body.addView(Ui.header(this, "Auto responder"));
        toggle("auto_reply", "Auto reply", "Answer the first unread chat", false);
        field("Reply text", "reply_text", "I will get back to you shortly.", false);
        field("Delay, seconds", "reply_delay", "5", true);
        body.addView(Ui.note(this, "Skips sKey and password-protected chats. One reply per chat per hour."));
        body.addView(Ui.header(this, "Presence"));
        toggle("seen_track", "Last-seen tracker", "Log every presence change", false);
        body.addView(Ui.header(this, "Voice"));
        toggle("voice_text", "Voice to text", "Transcribe voice notes and rounds", false);
        body.addView(Ui.header(this, "Saved messages"));
        StringBuilder tabs = new StringBuilder();
        for (String s : Vault.tabs()) tabs.append(s).append("  ");
        row("Tabbed favourites", tabs.toString(), null);
        body.addView(Ui.header(this, "Ads"));
        toggle("no_ads", "Block ads", "Hide bot-injected sponsored content", false);
    }

    private void trolling() {
        body.addView(Ui.header(this, "Trolling"));
        toggle("trolling", "Enable Trolling", "Everyone sees you typing, forever", false);
        body.addView(Ui.note(this, "Sends a real chat action every 4.5 s. No message is ever sent."));
        chips(new String[]{"Typing", "Voice", "Video", "Round", "Photo"}, Prefs.TROLL, "troll_mode", "typing");
        field("Target chat id (0 = last opened)", "troll_chat_str", "0", true);
        button("Apply now", false, v -> {
            long id = 0;
            try { id = Long.parseLong(Prefs.str("troll_chat_str", "0")); } catch (Exception ignored) { }
            Prefs.set("troll_chat", id);
            Mods.startTroll(id);
            Ui.toast("Trolling applied");
        });
    }

    private void testmode() {
        body.addView(Ui.header(this, "Jelly Button"));
        toggle("jelly", "Jelly Button", "Springy translucent control", true);
        JellyButton j = new JellyButton(this, "Press me — I am jelly");
        LinearLayout.LayoutParams lp = Ui.match();
        lp.setMargins(44, 18, 44, 18);
        j.setLayoutParams(lp);
        j.setOnClickListener(v -> Ui.toast("Spring released"));
        body.addView(j);
        body.addView(Ui.note(this, "Compresses on press, overshoots on release, settles back. Frosted translucent fill."));
        body.addView(Ui.header(this, "VoiceCommand"));
        toggle("voice_orb", "VoiceCommand orb", "Draggable orb, hold and speak", false);
        body.addView(Ui.note(this, "Commands: open settings · enable ghost · start trolling · write to <name> <text>. Restart chats screen to attach the orb."));
    }

    private void look() {
        body.addView(Ui.header(this, "Theme"));
        chips(new String[]{"Dark", "Light", "Gradient", "AMOLED", "Custom"}, Prefs.THEMES, "theme", "dark");
        field("Accent colour", "accent", "#2AABEE", false);
        field("Custom background", "bg_custom", "#0B0F17", false);
        button("Apply theme", false, v -> recreate());
        body.addView(Ui.header(this, "App name"));
        chips(Prefs.NAMES, Prefs.NAMES, "app_name", getString(R.string.app_name));
        body.addView(Ui.note(this, "Name applies across the interface immediately. Launcher label is baked at build time."));
    }

    private void plugins() {
        body.addView(Ui.header(this, "Plugin engine"));
        body.addView(Ui.note(this, "Folder: " + Plugins.folder().getAbsolutePath()
            + "\n\nJavaScript: name.js.codegram\nJava: name.dex / name.jar with class Plugin exposing\npublic static void onLoad(Plugins.Api api)\n\n"
            + "AyuGram / ExtraGram python plugins require Chaquopy and cannot execute here. "
            + "Their helper names (get_setting, set_setting, log, toast, send_message, is_premium) are exposed to JS plugins."));
        for (final String n : Plugins.list()) {
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(44, 18, 34, 18);
            r.addView(Ui.text(this, n, 15, Ui.fg()), new LinearLayout.LayoutParams(0, -2, 1f));
            Switch s = new Switch(this);
            s.setChecked(!Prefs.on("plug_off_" + n));
            s.setOnCheckedChangeListener((b, c) -> Prefs.set("plug_off_" + n, !c));
            r.addView(s);
            body.addView(r);
        }
        if (Plugins.list().isEmpty()) body.addView(Ui.note(this, "No plugins installed yet."));
        button("Reload plugins", false, v -> {
            Plugins.loadAll(getApplicationContext());
            StringBuilder sb = new StringBuilder();
            for (String s : Plugins.report()) sb.append(s).append('\n');
            new AlertDialog.Builder(this).setTitle("Plugin report")
                .setMessage(sb.length() == 0 ? "Nothing loaded" : sb.toString())
                .setPositiveButton("Close", null).show();
        });
    }

    private void index() {
        body.addView(Ui.header(this, "All features"));
        for (String f : Prefs.FEATURES) row("\u2713  " + f, null, null);
    }

    private void about() {
        body.addView(Ui.header(this, "About"));
        body.addView(Ui.note(this, Prefs.str("app_name", getString(R.string.app_name))
            + " 2.0\nOwn client code on the official TDLib library.\nAll data stays on this device.\nNo protocol abuse — documented client APIs only."));
        button("Lock everything now", false, v -> { Guard.lockAll(); Ui.toast("Locked"); finish(); });
        button("Erase all local data", true, v -> Guard.recovery(this, getString(R.string.pw_warn)));
    }
}
