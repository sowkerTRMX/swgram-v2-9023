package sw.gram.core;

import org.drinkless.tdlib.TdApi;

public final class Updates {
    public static Runnable onChatsChanged;
    public static Runnable onMessagesChanged;

    public static void handle(TdApi.Update u) {
        if (u instanceof TdApi.UpdateNewMessage) {
            TdApi.Message m = ((TdApi.UpdateNewMessage) u).message;
            Vault.capture(m);
            if (Mods.replyNeeded(m)) Mods.reply(m);
            if (onMessagesChanged != null) onMessagesChanged.run();
        } else if (u instanceof TdApi.UpdateDeleteMessages) {
            TdApi.UpdateDeleteMessages d = (TdApi.UpdateDeleteMessages) u;
            Vault.deleted(d.chatId, d.messageIds);
            if (onMessagesChanged != null) onMessagesChanged.run();
        } else if (u instanceof TdApi.UpdateMessageContent) {
            TdApi.UpdateMessageContent e = (TdApi.UpdateMessageContent) u;
            Vault.edited(e.chatId, e.messageId, Vault.textOf(e.newContent));
            if (onMessagesChanged != null) onMessagesChanged.run();
        } else if (u instanceof TdApi.UpdateMessageEdited) {
            if (onMessagesChanged != null) onMessagesChanged.run();
        } else if (u instanceof TdApi.UpdateUserStatus) {
            TdApi.UpdateUserStatus s = (TdApi.UpdateUserStatus) u;
            Vault.seen(s.userId, s.status instanceof TdApi.UserStatusOnline ? 1 : 0);
        } else if (u instanceof TdApi.UpdateNewChat || u instanceof TdApi.UpdateChatPosition
                || u instanceof TdApi.UpdateChatLastMessage) {
            if (onChatsChanged != null) onChatsChanged.run();
        }
    }
}
