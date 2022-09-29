import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

class OverlayChannel {
  static final OverlayChannel _instance = OverlayChannel._internal();
  factory OverlayChannel() => _instance;
  OverlayChannel._internal();
  late MethodChannel channel;

  void configureChannel() {
    channel = MethodChannel("OVERLAY_WINDOW");
    channel.setMethodCallHandler(methodHandler);
    print("cannelConfigured");
  }

  Future<void> methodHandler(MethodCall call) async {
    print("received StopRecord");
    switch (call.method) {
      case "stopRecording":
        print("received stopecording");
        break;
      default:
        break;
    }
  }
}
