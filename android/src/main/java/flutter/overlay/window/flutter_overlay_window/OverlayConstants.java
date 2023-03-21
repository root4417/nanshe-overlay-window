package flutter.overlay.window.flutter_overlay_window;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import io.flutter.plugin.common.MethodChannel;

final public class OverlayConstants {
    static final String APP_PACKAGE = "io.hexasoft.themisse";
//    static final String APP_PACKAGE = "com.example.flutter_overlay_window_example";
    static final String CACHED_TAG = "myCachedEngine";
    static final String CHANNEL_TAG = "x-slayer/overlay_channel";
    static final String OVERLAY_TAG = "x-slayer/overlay";
    static final String MESSENGER_TAG = "x-slayer/overlay_messenger";
    static final String CHANNEL_ID = "Overlay Channel";
    static final int NOTIFICATION_ID = 4579;
    static MethodChannel customChannel;
    static View.OnTouchListener  stopRecordingTouchListener;
    static void launchApp(Context appContext){
        try{
            Intent i = appContext.getPackageManager().getLaunchIntentForPackage(OverlayConstants.APP_PACKAGE);
            appContext.startActivity(i);

        }catch (Exception e){

        }
    }
}
