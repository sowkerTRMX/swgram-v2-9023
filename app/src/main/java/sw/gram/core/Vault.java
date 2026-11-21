package sw.gram.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.drinkless.tdlib.TdApi;
import java.util.ArrayList;

public final class Vault extends SQLiteOpenHelper {
    private static Vault I;
    public static void init(Context c) { if (I == null) I = new Vault(c.getApplicationContext()); }
    private Vault(Context c) { super(c, "vault.db", null, 1); }

    @Override public void onCreate(SQLiteDatabase d) {
        d.execSQL("CREATE TABLE msg(id INTEGER, chat INTEGER, sender INTEGER, date INTEGER, txt TEXT, media TEXT, del INTEGER DEFAULT 0, edit INTEGER DEFAULT 0, orig TEXT, PRIMARY KEY(id,chat))");
        d.execSQL("CREATE INDEX ix ON msg(chat,date)");
        d.execSQL("CREATE TABLE seen(uid INTEGER, ts INTEGER, online INTEGER)");
        d.execSQL("CREATE TABLE pin(chat INTEGER PRIMARY KEY, pos INTEGER)");
        d.execSQL("CREATE TABLE tab(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)");
        d.execSQL("CREATE TABLE fav(id INTEGER PRIMARY KEY AUTOINCREMENT, tab INTEGER, txt TEXT, ts INTEGER)");
        d.execSQL("INSERT INTO tab(name) VALUES('Notes'),('Links'),('Code'),('Media')");
    }
    @Override public void onUpgrade(SQLiteDatabase d, int a, int b) { }

    public static String textOf(TdApi.MessageContent c) {
        if (c instanceof TdApi.MessageText) return ((TdApi.MessageText) c).text.text;
        if (c instanceof TdApi.MessagePhoto) return "[photo] " + ((TdApi.MessagePhoto) c).caption.text;
        if (c instanceof TdApi.MessageVideo) return "[video] " + ((TdApi.MessageVideo) c).caption.text;
        if (c instanceof TdApi.MessageVoiceNote) return "[voice]";
        if (c instanceof TdApi.MessageVideoNote) return "[round]";
        if (c instanceof TdApi.MessageDocument) return "[file]";
        if (c instanceof TdApi.MessageSticker) return "[sticker]";
        return c == null ? "" : "[" + c.getClass().getSimpleName().replace("Message", "") + "]";
    }

    public static void capture(TdApi.Message m) {
        if (I == null || m == null || !Prefs.on("vault", true)) return;
        try {
            String txt = textOf(m.content);
            SQLiteDatabase d = I.getWritableDatabase();
            Cursor c = d.rawQuery("SELECT txt FROM msg WHERE id=? AND chat=?", new String[]{"" + m.id, "" + m.chatId});
            String old = c.moveToFirst() ? c.getString(0) : null;
            c.close();
            if (old != null && !old.equals(txt) && txt.length() > 0) {
                ContentValues u = new ContentValues();
                u.put("edit", 1); u.put("orig", old);
                d.update("msg", u, "id=? AND chat=?", new String[]{"" + m.id, "" + m.chatId});
            }
            ContentValues v = new ContentValues();
            v.put("id", m.id); v.put("chat", m.chatId);
            v.put("sender", sender(m.senderId));
            v.put("date", m.date); v.put("txt", txt);
            v.put("media", m.content == null ? "" : m.content.getClass().getSimpleName());
            if (old == null) { v.put("del", 0); v.put("edit", 0); }
            d.insertWithOnConflict("msg", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Throwable ignored) { }
    }

    private static long sender(TdApi.MessageSender s) {
        if (s instanceof TdApi.MessageSenderUser) return ((TdApi.MessageSenderUser) s).userId;
        if (s instanceof TdApi.MessageSenderChat) return -((TdApi.MessageSenderChat) s).chatId;
        return 0;
    }

    public static void deleted(long chat, long[] ids) {
        if (I == null || ids == null || !Prefs.on("keep_deleted", true)) return;
        SQLiteDatabase d = I.getWritableDatabase();
        for (long id : ids) {
            ContentValues v = new ContentValues(); v.put("del", 1);
            d.update("msg", v, "id=? AND chat=?", new String[]{"" + id, "" + chat});
        }
    }

    public static void edited(long chat, long id, String txt) {
        if (I == null || !Prefs.on("keep_edited", true)) return;
        SQLiteDatabase d = I.getWritableDatabase();
        Cursor c = d.rawQuery("SELECT txt FROM msg WHERE id=? AND chat=?", new String[]{"" + id, "" + chat});
        if (c.moveToFirst()) {
            ContentValues v = new ContentValues();
            v.put("edit", 1); v.put("orig", c.getString(0)); v.put("txt", txt);
            d.update("msg", v, "id=? AND chat=?", new String[]{"" + id, "" + chat});
        }
        c.close();
    }

    public static void seen(long uid, int online) {
        if (I == null || !Prefs.on("seen_track")) return;
        ContentValues v = new ContentValues();
        v.put("uid", uid); v.put("ts", System.currentTimeMillis() / 1000); v.put("online", online);
        I.getWritableDatabase().insert("seen", null, v);
    }

    public static class Row {
        public long chat, date; public String txt; public boolean del, edit;
    }

    public static ArrayList<Row> query(String q, long chatFilter, int limit) {
        ArrayList<Row> out = new ArrayList<>();
        if (I == null) return out;
        String sql = "SELECT chat,date,txt,del,edit FROM msg WHERE txt LIKE ?";
        ArrayList<String> args = new ArrayList<>();
        args.add("%" + (q == null ? "" : q) + "%");
        if (chatFilter != 0) { sql += " AND chat=?"; args.add("" + chatFilter); }
        sql += " ORDER BY date DESC LIMIT " + limit;
        Cursor c = I.getReadableDatabase().rawQuery(sql, args.toArray(new String[0]));
        while (c.moveToNext()) {
            Row r = new Row();
            r.chat = c.getLong(0); r.date = c.getLong(1); r.txt = c.getString(2);
            r.del = c.getInt(3) == 1; r.edit = c.getInt(4) == 1;
            out.add(r);
        }
        c.close();
        return out;
    }

    public static void pin(long chat) {
        if (I != null) I.getWritableDatabase().execSQL("INSERT OR REPLACE INTO pin(chat,pos) VALUES(?,?)",
            new Object[]{chat, System.currentTimeMillis()});
    }
    public static void unpin(long chat) {
        if (I != null) I.getWritableDatabase().execSQL("DELETE FROM pin WHERE chat=?", new Object[]{chat});
    }
    public static ArrayList<Long> pins() {
        ArrayList<Long> o = new ArrayList<>();
        if (I == null) return o;
        Cursor c = I.getReadableDatabase().rawQuery("SELECT chat FROM pin ORDER BY pos DESC", null);
        while (c.moveToNext()) o.add(c.getLong(0));
        c.close(); return o;
    }
    public static ArrayList<String> tabs() {
        ArrayList<String> o = new ArrayList<>();
        if (I == null) return o;
        Cursor c = I.getReadableDatabase().rawQuery("SELECT name FROM tab ORDER BY id", null);
        while (c.moveToNext()) o.add(c.getString(0));
        c.close(); return o;
    }
    public static void wipe() {
        if (I == null) return;
        SQLiteDatabase d = I.getWritableDatabase();
        d.execSQL("DELETE FROM msg"); d.execSQL("DELETE FROM seen");
        d.execSQL("DELETE FROM pin"); d.execSQL("DELETE FROM fav");
    }
}
