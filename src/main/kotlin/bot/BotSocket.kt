package bot

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket

object BotSocket {
     var socket: Socket? = null

    fun initSocket(host: String, socketHandler: Socket.() -> Unit) {
        val opts = IO.Options()
        opts.transports = arrayOf(WebSocket.NAME)
        socket = IO.socket(host, opts).apply {
            connect()
        }
        socket?.on(Socket.EVENT_CONNECT_ERROR) {
            println("connect error ${it[0]}")
        }
        socket?.socketHandler()
    }
}