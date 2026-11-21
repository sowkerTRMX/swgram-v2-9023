package sw.gram.core;

import android.os.Handler;
import android.os.Looper;
import org.drinkless.tdlib.TdApi;

public final class Mods {
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static Runnable trollTask;

    public static void applyGhost() {
        if (Td.client == null) return;
        Td.send(new TdApi.SetOption("online", new TdApi.OptionValueBoolean(!Prefs.on("ghost"))));
    }

    public static void startTroll(final long chatId) {
        stopTroll();
        if (!Prefs.on("trolling") || chatId == 0) return;
        trollTask = new Runnable() {
            @Override public void run() {
                if (!Prefs.on("trolling")) return;
                String m = Prefs.str("troll_mode", "typing");
                TdApi.ChatAction a;
                switch (m) {
                    case "voice": a = new TdApi.ChatActionRecordingVoiceNote(); break;
                    case "video": a = new TdApi.ChatActionRecordingVideo(); break;
                    case "round": a = new TdApi.ChatActionRecordingVideoNote(); break;
                    case "photo": a = new TdApi.ChatActionUploadingPhoto(); break;
                    default: a = new TdApi.ChatActionTyping();
                }
                Td.send(new TdApi.SendChatAction(chatId, 0, a));
                H.postDelayed(this, 4500);
            }
        };
        H.post(trollTask);
    }
    public static void stopTroll() { if (trollTask != null) { H.removeCallbacks(trollTask); trollTask = null; } }

    public static boolean replyNeeded(TdApi.Message m) {
        return Prefs.on("auto_reply") && !m.isOutgoing && m.chatId > 0
            && !SKey.has(m.chatId) && !Guard.has(Guard.chatKey(m.chatId));
    }
    public static void reply(final TdApi.Message m) {
        final String txt = Prefs.str("reply_text", "");
        if (txt.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (now - Prefs.lng("last_reply_" + m.chatId, 0) < 3600000L) return;
        Prefs.set("last_reply_" + m.chatId, now);
        final long chat = m.chatId;
        H.postDelayed(() -> send(chat, txt), Math.max(1, Prefs.num("reply_delay", 5)) * 1000L);
    }

    public static void send(long chatId, String text) {
        TdApi.FormattedText ft = new TdApi.FormattedText(text, new TdApi.TextEntity[0]);
        TdApi.InputMessageText im = new TdApi.InputMessageText(ft, false, true);
        Td.send(new TdApi.SendMessage(chatId, 0, null, null, null, im));
    }

    public static boolean isAd(TdApi.Message m) {
        return Prefs.on("no_ads") && m != null && m.viaBotUserId != 0;
    }
}
