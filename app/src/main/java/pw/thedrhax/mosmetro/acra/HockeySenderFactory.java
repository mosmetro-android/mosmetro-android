package pw.thedrhax.mosmetro.acra;

import android.content.Context;
import android.support.annotation.NonNull;

import org.acra.config.CoreConfiguration;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderFactory;

public class HockeySenderFactory implements ReportSenderFactory {

    @Override
    public boolean enabled(@NonNull CoreConfiguration coreConfiguration) {
        return true;
    }

    @NonNull
    @Override
    public ReportSender create(@NonNull Context context, @NonNull CoreConfiguration config) {
        return new HockeySender();
    }
}