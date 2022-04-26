import 'dart:convert';
import 'dart:io';

import 'package:adwaita/adwaita.dart';
import 'package:bitsdojo_window/bitsdojo_window.dart';
import 'package:flutter/material.dart';
import 'package:libadwaita/libadwaita.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

void main() {
  runApp(const MyApp());
  doWhenWindowReady(() {
    const initialSize = Size(600, 450);
    appWindow.minSize = initialSize;
    appWindow.size = initialSize;
    appWindow.alignment = Alignment.center;
    print("Hello");
    appWindow.show();
  });
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: "Leadpogrommer's chat",
      theme: AdwaitaThemeData.light(),
      darkTheme: AdwaitaThemeData.dark(),
      debugShowCheckedModeBanner: false,
      home: LoginPage(),
    );
  }
}

class LoginPage extends StatefulWidget {
  const LoginPage({Key? key}) : super(key: key);

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController();
  }

  @override
  Widget build(BuildContext context) {
    return AdwScaffold(
      headerbar: (_) => AdwHeaderBar.bitsdojo(
        appWindow: appWindow,
        title: const Text("Leadpogrommer's chat"),
      ),
      body: Center(
        child: SizedBox(
          width: 400,
          child: Column(
            children: [
              Text(
                "Enter username",
                style: AdwaitaThemeData.getTextTheme().headline1,
              ),
              SizedBox(
                height: 10,
              ),
              AdwTextField(
                autofocus: true,
                controller: _controller,
                onSubmitted: (username) {
                  login(context, username);
                },
              ),
              SizedBox(
                height: 10,
              ),
              AdwButton(
                child: Text("Login"),
                onPressed: () {
                  login(context, _controller.text);
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  void login(BuildContext context, String username) {
    print(username);
    Navigator.of(context)
        .push(MaterialPageRoute(builder: (context) => ChatPage(username)));
  }
}

class ChatPage extends StatefulWidget {
  final String username;
  ChatPage(this.username, {Key? key}) : super(key: key);

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> {
  late final WebSocketChannel _socket;
  late final List<Message> messages;
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _socket = WebSocketChannel.connect(Uri.parse("ws://127.0.0.1:8080/ws"));
    _socket.sink.add(jsonEncode({"action": "login", "data": widget.username}));
    handleMessages(_socket.stream);
    messages = [];
    _controller = TextEditingController();
  }

  Future<void> handleMessages(Stream stream) async {
    stream.listen((event) {
      print(event);
      // pri
      if(event.runtimeType == String){
        var msg = jsonDecode(event);
        setState(() {
          messages.add(Message(msg["sender"], msg["data"]));
        });

      }

    });
  }

  @override
  Widget build(BuildContext context) {
    return AdwScaffold(
      headerbar: (_) => AdwHeaderBar.bitsdojo(
        appWindow: appWindow,
        title: Text("Leadpogrommer's chat"),
        start: [
          AdwHeaderButton(
            icon: Icon(Icons.logout),
            onPressed: () {
              _socket.sink.close();
              Navigator.of(context).pop();
            },
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView.builder(
              itemCount: messages.length,
              itemBuilder: (context, i) {
                return Column(
                  children: [

                    Text(messages[i].sender != null
                        ? messages[i].sender!
                        : "You"),
                    Text(messages[i].data),
                    Divider(),
                  ],
                );
              },
            ),
          ),
          Row(
            children: [
              Expanded(
                child: AdwTextField(
                  controller: _controller,
                  onSubmitted: send,
                ),
              ),
              AdwButton.circular(
                onPressed: () {
                  send(_controller.text);
                },
                child: Icon(Icons.send),
              )
            ],
          ),
        ],
      ),
    );
  }

  void send(String message) {
    _socket.sink.add(jsonEncode({"action": "send", "data": message}));
    _controller.clear();
    setState(() {
      messages.add(Message(null, message));
    });


  }
}

class Message {
  final String? sender;
  final String data;

  Message(this.sender, this.data);
}
