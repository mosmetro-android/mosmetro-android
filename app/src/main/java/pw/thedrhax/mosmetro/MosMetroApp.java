package pw.thedrhax.mosmetro;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraHttpSender;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;

@AcraCore(buildConfigClass = BuildConfig.class,
          reportFormat = StringFormat.JSON)
@AcraHttpSender(uri = BuildConfig.API_URL_DEFAULT + BuildConfig.API_REL_ACRA,
                httpMethod = HttpSender.Method.POST)
public class MosMetroApp extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ACRA.init(this);

        ErrorReporter reporter = ACRA.getErrorReporter();
        reporter.putCustomData("BRANCH_NAME", BuildConfig.BRANCH_NAME);
        reporter.putCustomData("BUILD_NUMBER", "" + BuildConfig.BUILD_NUMBER);
    }
}
