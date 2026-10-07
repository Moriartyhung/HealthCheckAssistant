package com.healthcheck.patient.data.repository

import com.google.gson.JsonParseException
import com.healthcheck.patient.data.api.ApiConfig
import com.healthcheck.patient.data.model.ApiErrorResponse
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** 統一的 API 呼叫結果。HTTP 錯誤會解析 openapi.json 的 ApiErrorResponse { message }。 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class HttpError(val code: Int, val message: String) : ApiResult<Nothing>()
    data class NetworkError(val message: String) : ApiResult<Nothing>()
    data class ParseError(val message: String) : ApiResult<Nothing>()
}

fun ApiResult<*>.errorText(): String = when (this) {
    is ApiResult.Success<*> -> ""
    is ApiResult.HttpError -> "$message（HTTP $code）"
    is ApiResult.NetworkError -> message
    is ApiResult.ParseError -> message
}

/**
 * @param defaultMessages 依 openapi.json 各 endpoint 的 response description，
 *        當 Backend 沒有回傳 message 時使用。
 */
suspend fun <T> safeApiCall(
    defaultMessages: Map<Int, String> = emptyMap(),
    call: suspend () -> Response<T>,
): ApiResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) ApiResult.Success(body)
            else ApiResult.ParseError("伺服器回應內容為空")
        } else {
            val code = response.code()
            val serverMessage = try {
                response.errorBody()?.string()?.takeIf { it.isNotBlank() }?.let {
                    ApiConfig.gson.fromJson(it, ApiErrorResponse::class.java)?.message
                }
            } catch (e: Exception) {
                null
            }
            ApiResult.HttpError(
                code,
                serverMessage?.takeIf { it.isNotBlank() }
                    ?: defaultMessages[code]
                    ?: defaultHttpMessage(code)
            )
        }
    } catch (e: UnknownHostException) {
        ApiResult.NetworkError("無法連線到伺服器（找不到主機），請確認伺服器設定")
    } catch (e: ConnectException) {
        ApiResult.NetworkError("無法連線到伺服器，請確認 Backend 已啟動且位址正確")
    } catch (e: SocketTimeoutException) {
        ApiResult.NetworkError("連線逾時，請確認手機與 Backend 在同一網路")
    } catch (e: JsonParseException) {
        ApiResult.ParseError("伺服器回應格式無法解析：${e.message}")
    } catch (e: IllegalStateException) {
        // Gson 型別不符常以 IllegalStateException 拋出
        ApiResult.ParseError("伺服器回應格式無法解析：${e.message}")
    } catch (e: IOException) {
        ApiResult.NetworkError("網路錯誤：${e.message ?: e.javaClass.simpleName}")
    }
}

private fun defaultHttpMessage(code: Int): String = when (code) {
    400 -> "請求參數不正確"
    404 -> "找不到資料"
    409 -> "資料狀態衝突"
    in 500..599 -> "服務暫時無法使用"
    else -> "伺服器錯誤"
}
