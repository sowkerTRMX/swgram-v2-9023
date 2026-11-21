package sw.gram.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.drinkless.tdlib.TdApi;
import java.util.ArrayList;
import sw.gram.core.SKey;
import sw.gram.core.Td;
import sw.gram.core.Ui;
import sw.gram.core.Vault;

public class SearchActivity extends Activity {
    private LinearLayout out;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.column(this);
        root.setPadding(36, 40, 36, 30);
        setContentView(root);
        root.addView(Ui.header(this, "Search everything"));

        EditText q = new EditText(this);
        q.setHint("chats, messages, deleted, edited");
        q.setTextColor(Ui.fg()); q.setHintTextColor(Ui.dim());
        q.setBackgroundResource(sw.gram.R.drawable.input);
        q.setPadding(32, 26, 32, 26);
        q.setSingleLine(true);
        root.addView(q, Ui.match());

        ScrollView sv = new ScrollView(this);
        out = new LinearLayout(this);
        out.setOrientation(LinearLayout.VERTICAL);
        sv.addView(out);
        root.addView(sv, Ui.weight());

        q.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { render(e.toString()); }
        });
        render("");
    }

    private void render(String q) {
        out.removeAllViews();
        if (q.length() >= 2) {
            new Thread(() -> {
                final ArrayList<String> hits = new ArrayList<>();
                try {
                    TdApi.Chats cs = (TdApi.Chats) Td.sync(new TdApi.GetChats(new TdApi.ChatListMain(), 100));
                    for (long id : cs.chatIds) {
                        if (SKey.hidden(id)) continue;
                        TdApi.Chat ch = (TdApi.Chat) Td.sync(new TdApi.GetChat(id));
                        if (ch.title.toLowerCase().contains(q.toLowerCase())) hits.add("\uD83D\uDCAC " + ch.title);
                    }
                } catch (Throwable ignored) { }
                Ui.run(() -> {
                    for (String h : hits) {
                        TextView t = Ui.text(this, h, 15, Ui.accent());
                        t.setPadding(20, 16, 20, 16);
                        out.addView(t, 0);
                    }
                });
            }).start();
        }
        for (Vault.Row r : Vault.query(q, 0, 300)) {
            String mark = r.del ? "[deleted] " : (r.edit ? "[edited] " : "");
            TextView t = Ui.text(this, mark + r.txt, 14, r.del ? 0xFFE5484D : Ui.fg());
            t.setPadding(20, 14, 20, 14);
            out.addView(t);
        }
        if (out.getChildCount() == 0) out.addView(Ui.note(this, "Nothing found. The vault fills as messages arrive."));
    }
}
