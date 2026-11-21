package sw.gram.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.drinkless.tdlib.TdApi;
import java.util.ArrayList;
import java.util.Collections;
import sw.gram.R;
import sw.gram.core.Mods;
import sw.gram.core.Prefs;
import sw.gram.core.Td;
import sw.gram.core.Ui;
import sw.gram.core.Updates;
import sw.gram.core.Vault;

public class ChatActivity extends Activity {
    private long chat;
    private final ArrayList<TdApi.Message> msgs = new ArrayList<>();
    private Adapter adapter;
    private EditText box;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        chat = getIntent().getLongExtra("chat", 0);
        String title = getIntent().getStringExtra("title");

        LinearLayout root = Ui.column(this);
        setContentView(root);

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(36, 34, 36, 26);
        TextView back = Ui.text(this, "\u2039", 30, Ui.accent());
        back.setPadding(0, 0, 26, 0);
        back.setOnClickListener(v -> finish());
        head.addView(back);
        head.addView(Ui.text(this, title == null ? "Chat" : title, 18, Ui.fg()),
            new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(head, Ui.match());
        root.addView(Ui.divider(this));

        RecyclerView rv = new RecyclerView(this);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromBottom(true);
        rv.setLayoutManager(lm);
        adapter = new Adapter();
        rv.setAdapter(adapter);
        root.addView(rv, Ui.weight());

        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(24, 18, 24, 24);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        box = new EditText(this);
        box.setHint("Message");
        box.setTextColor(Ui.fg());
        box.setHintTextColor(Ui.dim());
        box.setBackgroundResource(R.drawable.input);
        box.setPadding(32, 26, 32, 26);
        box.setMaxLines(4);
        bar.addView(box, new LinearLayout.LayoutParams(0, -2, 1f));
        Button send = new Button(this);
        send.setText("\u27A4");
        send.setTextColor(0xFFFFFFFF);
        send.setBackgroundResource(R.drawable.btn);
        send.setStateListAnimator(null);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(Ui.dp(this, 60), Ui.dp(this, 52));
        slp.leftMargin = 14;
        send.setLayoutParams(slp);
        send.setOnClickListener(v -> {
            String t = box.getText().toString().trim();
            if (t.isEmpty()) return;
            Mods.send(chat, t);
            box.setText("");
            load();
        });
        bar.addView(send);
        root.addView(bar, Ui.match());

        Updates.onMessagesChanged = () -> Ui.run(this::load);
        load();
    }

    private void load() {
        new Thread(() -> {
            final ArrayList<TdApi.Message> out = new ArrayList<>();
            try {
                TdApi.Messages ms = (TdApi.Messages) Td.sync(new TdApi.GetChatHistory(chat, 0, 0, 60, false));
                Collections.addAll(out, ms.messages);
                Collections.reverse(out);
            } catch (final Throwable t) {
                Ui.run(() -> Ui.toast("History: " + Td.root(t)));
            }
            Ui.run(() -> { msgs.clear(); msgs.addAll(out); adapter.notifyDataSetChanged(); });
        }).start();
    }

    private class Adapter extends RecyclerView.Adapter<H> {
        @Override public H onCreateViewHolder(ViewGroup p, int v) {
            TextView t = Ui.text(ChatActivity.this, "", 15, Ui.fg());
            t.setBackgroundResource(R.drawable.card);
            t.setPadding(30, 22, 30, 22);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-2, -2);
            lp.setMargins(24, 6, 24, 6);
            t.setLayoutParams(lp);
            return new H(t);
        }
        @Override public void onBindViewHolder(H h, int i) {
            TdApi.Message m = msgs.get(i);
            String body = Vault.textOf(m.content);
            String mark = "";
            for (Vault.Row r : Vault.query("", chat, 400)) {
                if (r.date == m.date && r.txt.equals(body)) {
                    if (r.del) mark = "  [deleted]";
                    else if (r.edit) mark = "  [edited]";
                    break;
                }
            }
            h.t.setText((m.isOutgoing ? "\u2192 " : "\u2190 ") + body + mark);
            h.t.setTextColor(mark.contains("deleted") ? 0xFFE5484D : Ui.fg());
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) h.t.getLayoutParams();
            lp.gravity = m.isOutgoing ? Gravity.END : Gravity.START;
            h.t.setLayoutParams(lp);
        }
        @Override public int getItemCount() { return msgs.size(); }
    }

    private static class H extends RecyclerView.ViewHolder {
        final TextView t;
        H(View v) { super(v); t = (TextView) v; }
    }

    @Override protected void onDestroy() {
        Updates.onMessagesChanged = null;
        Mods.stopTroll();
        super.onDestroy();
    }
}
