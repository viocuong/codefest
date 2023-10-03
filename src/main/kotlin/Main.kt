
import kotlinx.coroutines.runBlocking
import utils.SocketHelper

fun main(args: Array<String>) = runBlocking {
    SocketHelper.initSocket("http://localhost")
}