import bot.BotExecutor
import bot.model.PlayerInfo
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Local

const val RAW_BIRTHDAY_FORMAT = "dd/MM/yyyy"
const val SERVER_BIRTHDAY_FORMAT = "yyyy-MM-dd"

// "0033-07-22"
fun recoverDate(wrongDate: String): String {
    val (startYear, endYear) = 1900 to 2024
    val (startMonth, endMonth) = 1 to 12
    val (startDay, endDay) = 1 to 31
    for (i in startYear..endYear) {
        for (j in startMonth..endMonth) {
            for (k in startDay..endDay) {
                val date = SimpleDateFormat(RAW_BIRTHDAY_FORMAT, Locale.US).parse("$i/$j/$k")
                val dateResult = SimpleDateFormat(SERVER_BIRTHDAY_FORMAT, Locale.US).format(date)
                if(dateResult == wrongDate)println( "$i-$j-$k")
            }
        }
    }
    return ""
}

fun main(args: Array<String>): Unit = runBlocking {
//    val date = SimpleDateFormat("dd/MM/yyyy", Locale.US).parse("1999/02/28")
////    println(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date))
//    val y  = SimpleDateFormat("yy").format(date)
//    val m = SimpleDateFormat("D").format(date)
//            val ddd = m.padStart(3,'0')
    // year = 1999//365 = 5
    // month = 1999 - year*365 = (1999 - 5*365)//30 = 5
    // day = 1999 - year * 365 - month*31 = 1999 - 5*365 - 5*30
    // month = 02 + 1999 - 1999//365 * 365 = 07
    // day = 1999 - 1999//365 * 365

//    println(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date))
////    println(SimpleDateFormat("yyyy-MM-dd").format(date))
//    println(recoverDate("0033-07-22"))
//    val date2 = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("0011-08-28")
//    println(SimpleDateFormat(SERVER_BIRTHDAY_FORMAT, Locale.US).format(date2))
//    val map = mapOf(Student("A") to 1, Student("B") to 2, Student("A") to 3)
//    //ln(map.size)
    launch {
        val botExecutor = BotExecutor()
        botExecutor.initGame(
            host = "http://localhost",
            clientInfo = PlayerInfo(gameId = "2f8ac26d-b672-45bd-8204-6970c1d82243", playerId = "player1-xxx"),
            killMode = false,
        )
    }
//
    launch {
        val botExecutor = BotExecutor()
        botExecutor.initGame(
            host = "http://localhost",
            clientInfo = PlayerInfo(gameId = "9f15e6cb-2402-4d74-a259-bda3812485ba", playerId = "player2-xxx"),
            killMode = true
        )
    }

//    val (vin, email) = Pair(
//        URLEncoder.encode(
//            CryptographyHelper.encryptStringAES256CBC(
//                data = "MRHCY2670NP000000" ?: Constants.STRING_EMPTY,
//                cryptographyEncryptRolloutInfo = CryptographyEncryptRolloutInfo.HONDA_CONNECT,
//            ),
//            CryptographyHelper.URL_ENCODE_DECODE_CHARSET,
//        ),
//        URLEncoder.encode(
//            CryptographyHelper.encryptStringAES256CBC(
//                data = "crtscc18.j424@aol.com" ?: Constants.STRING_EMPTY,
//                cryptographyEncryptRolloutInfo = CryptographyEncryptRolloutInfo.HONDA_CONNECT,
//            ),
//            CryptographyHelper.URL_ENCODE_DECODE_CHARSET,
//        ),
//            URLEncoder.encode(
//                CryptographyHelper.encryptStringAES256CBC(
//                    data = data.phoneNumber ?: Constants.STRING_EMPTY,
//                    cryptographyEncryptRolloutInfo = cryptographyEncryptRolloutInfo,
//                ),
//                CryptographyHelper.URL_ENCODE_DECODE_CHARSET,
//            ),
//    )
//    println("CUongnv vin = $vin, email = $email")
//    val dataDecrypt = Pair(
//        CryptographyHelper.decryptStringAES256CBC(
//            URLDecoder.decode(vin)),
//        CryptographyHelper.decryptStringAES256CBC(URLDecoder.decode(email)),
//    )
//    println(URLDecoder.decode("NDW7qmhT9DDkISFGl5GCxj9njYV1GF%2FYIFps9QCX8Kc%3D"))
    println("週一".lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
}