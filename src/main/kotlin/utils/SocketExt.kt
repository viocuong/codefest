package utils

import io.socket.client.Socket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject

fun Socket.onFLow(event: String) = callbackFlow{
    on(event){
        channel.trySend((it.get(0) as JSONObject))
    }
    awaitClose {  }
}