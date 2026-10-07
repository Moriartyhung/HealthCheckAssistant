package com.healthcheck.patient.data.api

import android.content.Context
import com.healthcheck.patient.BuildConfig
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Backend Base URL 的唯一管理點。
 *
 * - 預設值來自 gradle.properties 的 healthcheck.baseUrl（Build 時注入 BuildConfig.DEFAULT_BASE_URL）。
 * - App 內「伺服器設定」可在執行期覆寫，存於 SharedPreferences，不必重新 Build。
 * - 不要在其他檔案寫死任何 IP / localhost / 10.0.2.2。
 */
object ApiConfig {
    private const val PREFS = "api_config"
    private const val KEY_BASE_URL = "base_url"

    val gson: Gson = Gson()

    fun defaultBaseUrl(): String = BuildConfig.DEFAULT_BASE_URL

    fun getBaseUrl(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_BASE_URL, null)
            ?.let(::normalize)
            ?: defaultBaseUrl()

    /** 回傳 null 表示格式不合法。 */
    fun normalize(raw: String): String? {
        val value = raw.trim()
        if (!Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(value)) return null
        val url = value.toHttpUrlOrNull() ?: return null
        // Base URL 不接受登入資訊、query 或 fragment，避免意外改變 API 路徑。
        if (url.host.isBlank() || url.username.isNotEmpty() || url.password.isNotEmpty() ||
            url.query != null || url.fragment != null) return null
        return if (url.encodedPath.endsWith("/")) url.toString()
        else url.newBuilder().addPathSegment("").build().toString()
    }

    fun setBaseUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_BASE_URL, url).apply()
    }

    fun resetBaseUrl(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_BASE_URL).apply()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }
        builder.build()
    }

    fun createApi(baseUrl: String): HealthCheckApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(HealthCheckApi::class.java)
}
