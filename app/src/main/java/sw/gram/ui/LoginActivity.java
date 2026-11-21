package sw.gram.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.drinkless.tdlib.TdApi;
import java.io.File;
import sw.gram.R;
import sw.gram.core.Guard;
import sw.gram.core.Mods;
import sw.gram.core.Plugins;
import sw.gram.core.Prefs;
import sw.gram.core.Td;
import sw.gram.core.Ui;

public class LoginActivity extends Activity {
    private EditText phone, code, pass;
    private TextView title, sub, state;
    private Button next, signin;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.column(this);
        root.setBackgroundResource(R.drawable.grad);
        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(64, 140, 64, 80);
        sv.addView(box);
        root.addView(sv, Ui.weight());
        setContentView(root);

        title = Ui.text(this, getString(R.string.app_name), 34, 0xFFFFFFFF);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        sub = Ui.text(this, "Private messenger · Ghost · Vault · Plugins", 13, 0xCCFFFFFF);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 8, 0, 70);
        box.addView(sub);

        phone = field("Phone number  +...", InputType.TYPE_CLASS_PHONE);
        code = field("Verification code", InputType.TYPE_CLASS_NUMBER);
        pass = field("Cloud password (2FA, if set)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        code.setVisibility(View.GONE);
        pass.setVisibility(View.GONE);
        box.addView(phone); box.addView(code); box.addView(pass);

        next = button("Continue");
        next.setOnClickListener(v -> submitPhone());
        box.addView(next);

        signin = button("Sign in");
        signin.setVisibility(View.GONE);
        signin.setOnClickListener(v -> submitCode());
        box.addView(signin);

        state = Ui.text(this, "", 12, 0xCCFFFFFF);
        state.setPadding(0, 24, 0, 0);
        box.addView(state);

        Button cfg = button("Settings");
        cfg.setBackgroundResource(R.drawable.chip);
        cfg.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        box.addView(cfg);

        File db = new File(getFilesDir(), "td/db"); db.mkdirs();
        File fl = new File(getFilesDir(), "td/files"); fl.mkdirs();

        Td.onReady = () -> Ui.run(() -> {
            Mods.applyGhost();
            Plugins.loadAll(getApplicationContext());
            startActivity(new Intent(this, ChatsActivity.class));
            finish();
        });
        Td.onAuthUpdate = () -> Ui.run(this::describeState);

        try {
            Td.start(getFilesDir() + "/td", getString(R.string.api_id), getString(R.string.api_hash));
        } catch (Throwable t) {
            state.setText("TDLib init failed: " + Td.root(t));
        }
    }

    private void describeState() {
        TdApi.AuthorizationState s = null;
        try { s = (TdApi.AuthorizationState) Td.sync(new TdApi.GetAuthorizationState()); } catch (Throwable ignored) { }
        if (s == null) return;
        if (s instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
            state.setText("Waiting for phone number");
        } else if (s instanceof TdApi.AuthorizationStateWaitCode) {
            phone.setVisibility(View.GONE); next.setVisibility(View.GONE);
            code.setVisibility(View.VISIBLE); pass.setVisibility(View.VISIBLE);
            signin.setVisibility(View.VISIBLE);
            title.setText("Enter code"); sub.setText("Sent to your Telegram app");
            state.setText("Waiting for code");
        } else if (s instanceof TdApi.AuthorizationStateWaitPassword) {
            code.setVisibility(View.GONE);
            pass.setVisibility(View.VISIBLE);
            signin.setVisibility(View.VISIBLE);
            title.setText("Cloud password"); sub.setText("Two-step verification");
            state.setText("Waiting for 2FA password");
        } else if (s instanceof TdApi.AuthorizationStateWaitTdlibParameters) {
            state.setText("Initialising database…");
        } else if (s instanceof TdApi.AuthorizationStateClosed) {
            state.setText("Client closed");
        }
    }

    private EditText field(String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(type);
        e.setSingleLine(true);
        e.setTextColor(0xFFFFFFFF);
        e.setHintTextColor(0xAAFFFFFF);
        e.setTextSize(16);
        e.setBackgroundResource(R.drawable.input);
        e.setPadding(40, 34, 40, 34);
        LinearLayout.LayoutParams lp = Ui.match();
        lp.setMargins(0, 14, 0, 14);
        e.setLayoutParams(lp);
        return e;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(0xFFFFFFFF);
        b.setBackgroundResource(R.drawable.btn);
        b.setStateListAnimator(null);
        LinearLayout.LayoutParams lp = Ui.match();
        lp.setMargins(0, 16, 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void submitPhone() {
        final String p = phone.getText().toString().trim();
        if (p.length() < 6) { Ui.toast("Enter a valid phone number"); return; }
        state.setText("Requesting code…");
        new Thread(() -> {
            try {
                Td.sync(new TdApi.SetAuthenticationPhoneNumber(p, null));
                Ui.run(this::describeState);
            } catch (final Throwable t) {
                Ui.run(() -> state.setText("Error: " + Td.root(t)));
            }
        }).start();
    }

    private void submitCode() {
        final String c = code.getText().toString().trim();
        final String p = pass.getText().toString();
        state.setText("Signing in…");
        new Thread(() -> {
            try {
                if (!p.isEmpty() && c.isEmpty()) Td.sync(new TdApi.CheckAuthenticationPassword(p));
                else Td.sync(new TdApi.CheckAuthenticationCode(c));
                Ui.run(this::describeState);
            } catch (final Throwable t) {
                Ui.run(() -> state.setText("Error: " + Td.root(t)));
            }
        }).start();
    }

    @Override protected void onResume() {
        super.onResume();
        if (Td.ready) { startActivity(new Intent(this, ChatsActivity.class)); finish(); }
    }
}
