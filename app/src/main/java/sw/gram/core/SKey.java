package sw.gram.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public final class SKey {
    private static LinkedHashSet<Long> ids() {
        LinkedHashSet<Long> s = new LinkedHashSet<>();
        for (String p : Prefs.str("skey_ids", "").split(",")) {
            if (!p.trim().isEmpty()) { try { s.add(Long.parseLong(p.trim())); } catch (Exception ignored) { } }
        }
        return s;
    }
    private static void save(LinkedHashSet<Long> s) {
        StringBuilder b = new StringBuilder();
        for (Long l : s) { if (b.length() > 0) b.append(','); b.append(l); }
        Prefs.set("skey_ids", b.toString());
    }
    public static boolean has(long chat) { return ids().contains(chat); }
    public static void add(long chat) { LinkedHashSet<Long> s = ids(); s.add(chat); save(s); }
    public static void remove(long chat) { LinkedHashSet<Long> s = ids(); s.remove(chat); save(s); }
    public static ArrayList<Long> list() { return new ArrayList<>(ids()); }
    public static boolean hidden(long chat) { return has(chat) && !Prefs.on("skey_reveal"); }
}
