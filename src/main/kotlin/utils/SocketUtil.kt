//package utils
//
//import okhttp3.OkHttpClient
//import java.net.InetSocketAddress
//import java.net.Socket
//import java.net.URISyntaxException
//import java.net.http.WebSocket
//import java.util.concurrent.TimeUnit
//
//
//object SocketUtil {
//    fun init(url: String?): Socket? {
//        val okHttpClient: OkHttpClient = httpClientBuilder
//        IO.setDefaultOkHttpCallFactory(okHttpClient)
//        IO.setDefaultOkHttpWebSocketFactory(okHttpClient)
//        try {
//            return IO.socket(url)
//        } catch (e: URISyntaxException) {
//            e.printStackTrace()
//        }
//        return null
//    }
//
//    private val httpClientBuilder: OkHttpClient
//        private get() {
//            val clientBuilder: OkHttpClient.Builder =
//                Builder().connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES)
//                    .readTimeout(5, TimeUnit.MINUTES)
//            val host = "hl-proxyb"
//            val port = 8080
//            val username = "xxxx"
//            val password = "xxxxx"
//            clientBuilder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)))
//            clientBuilder.proxyAuthenticator(ProxyAuthenticator(username, password))
//            return clientBuilder.build()
//        }
//}
//
//var mSocket: Socket? = null
//
//fun connectToServer() {
//    if (mSocket != null) {
//        mSocket.disconnect()
//        mSocket = null
//    }
//    mSocket = try {
//        SocketUtil.init("http://codefest79.satek.vn")
//    } catch (e: Exception) {
//        e.printStackTrace()
//        return
//    }
//    if (mSocket == null) {
//        println("connectToServer SOCKET NULL")
//        return
//    }
//    mSocket.on("player room state", object : WebSocket.Listener {
//        fun call(vararg objects: Any) {
//            if (objects != null) {
//                if (objects.size != 0) {
//                    println("ROOM_STATE " + objects[0].toString())
//                    mDataExecutor.execute(DriveElephantRunable(mSocket, objects[0].toString()))
//                }
//            }
//        }
//    })
//    mSocket.on("player drive elephant state", object : Listener() {
//        fun call(vararg objects: Any) {
//            if (objects != null) {
//                if (objects.size != 0) {
//                    println("DRIVE_ELEPHANT_STATE " + objects[0].toString())
//                }
//            }
//        }
//    })
//    mSocket.connect()
//    mSocket.emit("player join room", Constants.KEY_TEAM, Constants.KEY_TEAM)
//}