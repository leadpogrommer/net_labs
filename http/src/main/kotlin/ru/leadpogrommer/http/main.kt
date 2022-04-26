package ru.leadpogrommer.http

import sun.misc.Signal
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketException
import java.nio.file.Path
import java.util.concurrent.Executors

fun main() {
    val server = ServerSocket()
    server.bind(InetSocketAddress("127.0.0.1", 8080))

    var running = true

    val handlersPool = Executors.newCachedThreadPool()
    Signal.handle(Signal("INT")) {
        println("Stopping")
        running = false
        server.close()
    }

    while (running) {
        try {
            val client = server.accept()
            handlersPool.submit(ClientHandler(client, Path.of("/home/ilya/work/net/http/www")))
        } catch (e: SocketException) {
            break
        }
    }

    handlersPool.shutdownNow()
    server.close()
}