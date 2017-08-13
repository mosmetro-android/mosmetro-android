package pw.thedrhax.captcharecognition;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import pw.thedrhax.util.Util;

public class RecognitionService extends IntentService {
    private CaptchaRecognition cr = null;

    public RecognitionService() {
        super("CaptchaRecognition");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) return;

        if (cr == null) {
            cr = new CaptchaRecognition(RecognitionService.this);
        }

        Bitmap bitmap = Util.base64ToBitmap(intent.getStringExtra("bitmap_base64"));
        String code = cr.recognize(bitmap);

        sendBroadcast(new Intent("pw.thedrhax.captcharecognition.RESULT").putExtra("code", code));
    }

    @Override
    public void onDestroy() {
        cr.close();
    }
}
