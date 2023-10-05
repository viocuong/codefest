package utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.json.JSONObject
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object JsonConverter {
    val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    inline fun <reified T> fromJson(jsonObj: JSONObject, vararg customAdapters: Any ): T? {
        val moshi: Moshi = Moshi.Builder().run {
            add(KotlinJsonAdapterFactory())
            customAdapters.forEach { adapter ->
                add(adapter)
            }
            build()
        }
        val adapter: JsonAdapter<T> = moshi.adapter(T::class.java)
        return adapter.fromJson(jsonObj.toString())
    }

    inline fun <reified T> T.toJson(): JSONObject {
        val adapter: JsonAdapter<T> = moshi.adapter(T::class.java)
        return JSONObject(adapter.toJson(this))
    }
}