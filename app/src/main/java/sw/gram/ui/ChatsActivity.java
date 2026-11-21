package sw.gram.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.drinkless.tdlib.TdApi;
import java.util.ArrayList;
import sw.gram.R;
import sw.gram.core.Guard;
import sw.gram.core.Mods;
import sw.gram.core.Prefs;
import sw.gram.core.SKey;
import sw.gram.core.Td;
import sw.gram.core.Ui;
import sw.gram.core.Updates;
import sw.gram.core.Vault;
import sw.gram.view.VoiceOrb;

public class ChatsActivity extends Activity {
    private final ArrayList<TdApi.Chat> items = new ArrayList<>();
    private Adapter adapter;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.column(this);
        setContentView(root);

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(44, 40, 30, 30);
        TextView t = Ui.text(this, Prefs.str("app_name", getString(R.string.app_name)), 22, Ui.fg());
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        head.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        head.addView(icon("\uD83D\uDD0D", v -> startActivity(new Intent(this, SearchActivity.class))));
        head.addView(icon("\u2699", v -> startActivity(new Intent(this, SettingsActivity.class))));
        root.addView(head, Ui.match());
        root.addView(Ui.divider(this));

        RecyclerView rv = new RecyclerView(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter();
        rv.setAdapter(adapter);
        root.addView(rv, Ui.weight());

        Updates.onChatsChanged = () -> Ui.run(this::load);
        load();

        if (Prefs.on("voice_orb")) {
            FrameLayout decor = (FrameLayout) getWindow().getDecorView();
            VoiceOrb orb = new VoiceOrb(this);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Ui.dp(this, 62), Ui.dp(this, 62));
            lp.gravity = Gravity.START | Gravity.BOTTOM;
            lp.leftMargin = Ui.dp(this, 18);
            lp.bottomMargin = Ui.dp(this, 90);
            decor.addView(orb, lp);
        }
    }

    private TextView icon(String s, View.OnClickListener l) {
        TextView v = Ui.text(this, s, 20, Ui.accent());
        v.setPadding(22, 0, 22, 0);
        v.setOnClickListener(l);
        return v;
    }

    private void load() {
        new Thread(() -> {
            final ArrayList<TdApi.Chat> out = new ArrayList<>();
            try {
                Td.sync(new TdApi.LoadChats(new TdApi.ChatListMain(), 40));
                TdApi.Chats cs = (TdApi.Chats) Td.sync(new TdApi.GetChats(new TdApi.ChatListMain(), 100));
                for (long id : cs.chatIds) {
                    if (SKey.hidden(id)) continue;
                    try {
                        TdApi.Chat ch = (TdApi.Chat) Td.sync(new TdApi.GetChat(id));
                        out.add(ch);
                    } catch (Throwable ignored) { }
                }
            } catch (final Throwable t) {
                Ui.run(() -> Ui.toast("Chats: " + Td.root(t)));
            }
            Ui.run(() -> { items.clear(); items.addAll(out); adapter.notifyDataSetChanged(); });
        }).start();
    }

    private void menu(final TdApi.Chat ch) {
        String[] opts = {"Toggle sKey archive", "Toggle visual pin", "Toggle chat password", "Open vault search"};
        new AlertDialog.Builder(this).setTitle(ch.title).setItems(opts, (d, w) -> {
            if (w == 0) { if (SKey.has(ch.id)) SKey.remove(ch.id); else SKey.add(ch.id); load(); }
            else if (w == 1) { if (Vault.pins().contains(ch.id)) Vault.unpin(ch.id); else Vault.pin(ch.id); Ui.toast("Pin toggled"); }
            else if (w == 2) {
                String k = Guard.chatKey(ch.id);
                if (Guard.has(k)) { Guard.clear(k); Ui.toast("Chat unlocked"); }
                else { Guard.assign(k, "pin", "1234"); Ui.toast("Locked with 1234 — change it in Settings"); }
            } else startActivity(new Intent(this, SearchActivity.class));
        }).show();
    }

    private class Adapter extends RecyclerView.Adapter<Holder> {
        @Override public Holder onCreateViewHolder(ViewGroup p, int v) {
            LinearLayout row = new LinearLayout(ChatsActivity.this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackgroundResource(R.drawable.card);
            row.setPadding(36, 28, 36, 28);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
            lp.setMargins(24, 8, 24, 8);
            row.setLayoutParams(lp);
            TextView a = Ui.text(ChatsActivity.this, "", 16, Ui.fg());
            TextView b = Ui.text(ChatsActivity.this, "", 12, Ui.dim());
            b.setPadding(0, 6, 0, 0);
            row.addView(a); row.addView(b);
            return new Holder(row, a, b);
        }
        @Override public void onBindViewHolder(Holder h, int i) {
            final TdApi.Chat ch = items.get(i);
            h.title.setText((Vault.pins().contains(ch.id) ? "\uD83D\uDCCC " : "") + ch.title);
            String last = ch.lastMessage != null ? Vault.textOf(ch.lastMessage.content) : "";
            h.sub.setText(last.isEmpty() ? "no messages" : last);
            h.row.setOnClickListener(v -> {
                new Thread(() -> {
                    if (!Guard.gate(ChatsActivity.this, Guard.chatKey(ch.id), "Chat locked")) {
                        Ui.run(() -> Ui.toast("Cancelled"));
                        return;
                    }
                    Ui.run(() -> {
                        Intent in = new Intent(ChatsActivity.this, ChatActivity.class);
                        in.putExtra("chat", ch.id);
                        in.putExtra("title", ch.title);
                        startActivity(in);
                        Mods.startTroll(ch.id);
                    });
                }).start();
            });
            h.row.setOnLongClickListener(v -> { menu(ch); return true; });
        }
        @Override public int getItemCount() { return items.size(); }
    }

    private static class Holder extends RecyclerView.ViewHolder {
        final LinearLayout row; final TextView title, sub;
        Holder(View v, TextView a, TextView b) { super(v); row = (LinearLayout) v; title = a; sub = b; }
    }

    @Override protected void onDestroy() { Updates.onChatsChanged = null; super.onDestroy(); }
}
