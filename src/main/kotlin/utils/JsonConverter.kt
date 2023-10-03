package utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import org.json.JSONObject

object JsonConverter {
    val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    inline fun <reified T> fromJson(jsonObj: JSONObject): T? {
        val adapter: JsonAdapter<T> = moshi.adapter(T::class.java)
        return adapter.fromJson(jsonObj.toString())
    }

    inline fun <reified T> T.toJson(): JSONObject {
        val adapter: JsonAdapter<T> = moshi.adapter(T::class.java)
        return JSONObject(adapter.toJson(this))
    }
}