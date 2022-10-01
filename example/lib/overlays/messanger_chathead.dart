import 'package:flutter/material.dart';
import 'package:flutter_overlay_window/flutter_overlay_window.dart';

import '../expandable_fab.dart';

class MessangerChatHead extends StatefulWidget {
  const MessangerChatHead({Key? key}) : super(key: key);

  @override
  State<MessangerChatHead> createState() => _MessangerChatHeadState();
}

class _MessangerChatHeadState extends State<MessangerChatHead> {
  Color color = const Color(0xFFFFFFFF);
  BoxShape shape = BoxShape.rectangle;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((timeStamp) async {
      // await FlutterOverlayWindow.updateFlag(OverlayFlag.clickThrough);
    });
    FlutterOverlayWindow.overlayListener.listen((event) {
      if (event == 'HEY') {
        // setState(() {
        //  Update Timer
        // });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: ExpandableFab(
        distance: 60.0,
        children: [
          ActionButton(
            onPressed: () => {},
            icon: const Icon(Icons.format_size),
          ),
          ActionButton(
            onPressed: () => {},
            icon: const Icon(Icons.insert_photo),
          ),
        ],
      ),
    );

    // return FabCircularMenu(ringDiameter: 160, ringWidth: 50, children: <Widget>[
    //   IconButton(
    //       icon: Icon(Icons.home),
    //       onPressed: () {
    //         print('Home');
    //       }),
    //   IconButton(
    //       icon: Icon(Icons.favorite),
    //       onPressed: () {
    //         print('Favorite');
    //       }),
    //   IconButton(
    //       icon: Icon(Icons.favorite),
    //       onPressed: () {
    //         print('Favorite');
    //       })
    // ]);
    // return FloatingPage();
    // return FloatingActionButton(
    //   onPressed: () {},
    //   child: Text("hi"),
    // );
    return Material(
      color: Colors.transparent,
      elevation: 0.0,
      child: GestureDetector(
        onTap: () async {
          if (shape == BoxShape.circle) {
            // await FlutterOverlayWindow.resizeOverlay(matchParent, matchParent);
            // setState(() {
            //   shape = BoxShape.rectangle;
            // });
            await FlutterOverlayWindow.resizeOverlay(250, 250);
            setState(() {
              shape = BoxShape.rectangle;
            });
          } else {
            await FlutterOverlayWindow.resizeOverlay(350, 350);
            setState(() {
              shape = BoxShape.circle;
            });
          }
        },
        child: FloatingActionButton(
          onPressed: () {},
          child: Text('Hi'),
        ),

        // child: Container(
        //   height: MediaQuery.of(context).size.height,
        //   decoration: BoxDecoration(color: color, shape: shape),
        //   child: const Center(
        //     child: FlutterLogo(),
        //   ),
        // ),
      ),
    );
  }
}
