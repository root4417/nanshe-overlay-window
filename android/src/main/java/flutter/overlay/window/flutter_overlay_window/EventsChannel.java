package flutter.overlay.window.flutter_overlay_window;

import android.os.Handler;
import android.os.Looper;

import java.util.Map;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodChannel;

public class EventsChannel{
    public static EventsChannel instant = new EventsChannel();
    final public String CHANNEL_NAME = "OVERLAY_WINDOW";

    public MethodChannel methodChannel;

    public void configureChannel(BinaryMessenger binaryMessenger) {
        methodChannel = new MethodChannel(binaryMessenger, CHANNEL_NAME);
    }

    public void stopScreenRecording(){
//        final Handler handler = new Handler(Looper.getMainLooper());
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {

                methodChannel.invokeMethod("stopRecording","");
//            }
//        }, 1500);

    }

}