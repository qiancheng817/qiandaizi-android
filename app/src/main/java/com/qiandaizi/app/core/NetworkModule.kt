package com.qiandaizi.app.core

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import kotlinx.serialization.json.Json
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    explicitNulls = false
    coerceInputValues = true
}

/**
 * 统一附加：
 * - Authorization: Bearer <token>
 * - ?bookId=<当前账本>
 */
class CommonInterceptor(
    private val tokenProvider: () -> String?,
    private val bookProvider: () -> Int?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        var request = chain.request()

        tokenProvider()?.takeIf { it.isNotBlank() }?.let {
            request = request.newBuilder().header("Authorization", "Bearer $it").build()
        }
        val bookId = bookProvider()
        if (bookId != null && request.url.queryParameter("bookId") == null) {
            val url = request.url.newBuilder()
                .addQueryParameter("bookId", bookId.toString())
                .build()
            request = request.newBuilder().url(url).build()
        }
        return chain.proceed(request)
    }
}

object NetFactory {

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(
                CommonInterceptor(
                    tokenProvider = { AppGraph.state.token() },
                    bookProvider = { AppGraph.state.bookId() }
                )
            )
            .addInterceptor(logging)
            .build()
    }

    private val cache = ConcurrentHashMap<String, Api>()

    fun api(baseUrl: String): Api {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return cache.getOrPut(normalized) {
            Retrofit.Builder()
                .baseUrl(normalized + "api/")
                .client(client)
                .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(Api::class.java)
        }
    }
}
