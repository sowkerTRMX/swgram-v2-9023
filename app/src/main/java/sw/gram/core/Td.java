package sw.gram.core;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class Td {
    public static Client client;
    public static volatile boolean ready;
    public static Runnable onReady;
    public static Runnable onAuthUpdate;

    public static void start(String dir, String apiId, String apiHash) {
        Client.execute(new TdApi.SetLogVerbosityLevel(1));
        client = Client.create(result -> {
            if (result instanceof TdApi.UpdateAuthorizationState) {
                TdApi.AuthorizationState st = ((TdApi.UpdateAuthorizationState) result).authorizationState;
                ready = st instanceof TdApi.AuthorizationStateReady;
                if (ready && onReady != null) onReady.run();
                if (onAuthUpdate != null) onAuthUpdate.run();
            }
            if (result instanceof TdApi.Update) Updates.handle((TdApi.Update) result);
        }, null, null);

        TdApi.TdlibParameters p = new TdApi.TdlibParameters();
        p.useTestDc = false;
        p.databaseDirectory = dir + "/db";
        p.filesDirectory = dir + "/files";
        p.databaseEncryptionKey = new byte[0];
        p.useFileDatabase = true;
        p.useChatInfoDatabase = true;
        p.useMessageDatabase = true;
        p.useSecretChats = false;
        p.apiId = Integer.parseInt(apiId.trim());
        p.apiHash = apiHash.trim();
        p.systemLanguageCode = "en";
        p.deviceModel = "swgram";
        p.systemVersion = "Android";
        p.applicationVersion = "2.0";
        p.enableStorageOptimizer = true;
        p.ignoreFileNames = false;
        send(new TdApi.SetTdlibParameters(p));
        send(new TdApi.CheckDatabaseEncryptionKey(new byte[0]));
    }

    public static CompletableFuture<TdApi.Object> send(TdApi.Function<?> f) {
        CompletableFuture<TdApi.Object> cf = new CompletableFuture<>();
        client.send(f, r -> {
            if (r instanceof TdApi.Error) {
                TdApi.Error e = (TdApi.Error) r;
                cf.completeExceptionally(new RuntimeException(e.code + " " + e.message));
            } else cf.complete(r);
        });
        return cf;
    }

    public static TdApi.Object sync(TdApi.Function<?> f) {
        try { return send(f).get(45, TimeUnit.SECONDS); }
        catch (Exception e) { throw new RuntimeException(root(e)); }
    }

    public static String root(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) c = c.getCause();
        return c.getMessage() == null ? c.toString() : c.getMessage();
    }
}
