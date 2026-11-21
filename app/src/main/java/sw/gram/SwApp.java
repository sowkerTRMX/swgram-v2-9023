package sw.gram;

import android.app.Application;
import sw.gram.core.Guard;
import sw.gram.core.Plugins;
import sw.gram.core.Prefs;
import sw.gram.core.Ui;
import sw.gram.core.Vault;

public class SwApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        Ui.app = this;
        Prefs.init(this);
        Vault.init(this);
        Guard.init(this);
        Plugins.init(this);
    }
}
