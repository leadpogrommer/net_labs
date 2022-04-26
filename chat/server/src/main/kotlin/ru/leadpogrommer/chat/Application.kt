package ru.leadpogrommer.chat

//import io.ktor.serialization.kotlinx.json.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow

fun main() {
    val chatServer = ChatServer()
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(WebSockets)
        routing {
            webSocket("/ws") {
                chatServer.handleConnection(this)
            }
        }
    }.start(wait = true)
}

@Serializable
data class ClientMessage(val action: String, val data: String);
@Serializable
data class ServerMessage(val sender: String, val data: String);

class ChatServer(){
    private val clients = Collections.synchronizedMap(mutableMapOf<String, DefaultWebSocketServerSession>());
    private var nextClientId = 0;

    suspend fun handleConnection(session: DefaultWebSocketServerSession){
        var loggedIn = false;
        lateinit var username: String
        for(frame in session.incoming){
            when(frame){
                is Frame.Text -> {
                    try {
                        val message = Json.decodeFromString<ClientMessage>(frame.readText())
                        when(message.action){
                            "login" -> {
                                if(!loggedIn){
                                    loggedIn = true
                                    var id: Int = 0;
                                    synchronized(nextClientId){
                                        id = nextClientId++;
                                    }
                                    username = "${message.data}#$id"
                                    clients[username] = session
                                    broadcast(ServerMessage("system", "$username joined"))
                                }
                            }
                            "send" -> {
                                if(loggedIn){
                                    broadcast(ServerMessage(username, message.data), setOf(username))
                                }
                            }
                        }
                    }catch (e: SerializationException){
                        println(e.message)
                    }
                }
                else -> {}
            }
        }
        if(loggedIn){
            clients.remove(username);
            broadcast(ServerMessage("system", "$username left"))
        }
    }
    suspend fun broadcast(message: ServerMessage, exclude: Collection<String> = emptyList()){
        clients.filter { !exclude.contains(it.key) }.map {
            it.value.send(Frame.Text(Json.encodeToString(message)))
        }
    }
}