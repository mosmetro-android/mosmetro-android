package pw.thedrhax.mosmetro;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.data.StringFormat;

import pw.thedrhax.mosmetro.acra.HockeySenderFactory;

@AcraCore(buildConfigClass = BuildConfig.class,
          reportSenderFactoryClasses = {HockeySenderFactory.class},
          reportFormat = StringFormat.JSON)
public class MosMetroApp extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ACRA.init(this);
    }
}
