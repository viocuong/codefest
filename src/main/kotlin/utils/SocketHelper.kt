package utils

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import model.Information
import utils.JsonConverter.toJson

object SocketHelper {
    private var socket: Socket? = null
    fun initSocket(host: String) {
        val opts = IO.Options()
        opts.transports = arrayOf(WebSocket.NAME)
        socket = IO.socket(host, opts)
        socket?.on("join game") { args ->
            val data = args[0]
            println("Join game $data")
        }
        socket?.on(Socket.EVENT_CONNECT) {
            println("connect $it")
            socket?.emit(
                "join game",
                Information(
                    gameId = "125546b1-1d09-49a6-a344-ecc6bd592ccc",
                    playerId = "player2-xxx"
                ).toJson()
            )
        }
        socket?.on(Socket.EVENT_CONNECT_ERROR) {
            println("connect error ${it[0]}")
        }
        socket?.connect()
    }
}