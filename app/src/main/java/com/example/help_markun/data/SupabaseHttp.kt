package com.example.help_markun.data

import com.example.help_markun.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.net.ssl.SSLException

/** Supabase から返ったエラー（message はそのまま画面に出せる日本語） */
class SupabaseException(val code: Int, message: String) : IOException(message)

/**
 * Supabase（Auth・REST）を呼ぶための共通処理。HttpURLConnection だけで動く。
 * token を渡すとログイン中のユーザーとして、渡さなければログインなし（anon）として呼ぶ。
 */
object SupabaseHttp {
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY

    val isConfigured: Boolean get() = baseUrl.isNotBlank() && anonKey.isNotBlank()

    suspend fun call(
        method: String,
        path: String,
        body: String? = null,
        token: String? = null,
        prefer: String? = null,
    ): String = withContext(Dispatchers.IO) {
        if (!isConfigured) throw IOException("Supabase の URL / キーが未設定です")
        try {
            request(method, path, body, token, prefer)
        } catch (e: UnknownHostException) {
            throw IOException("インターネットに接続できません。通信環境を確認してください", e)
        } catch (e: SocketTimeoutException) {
            throw IOException("サーバーに接続できませんでした（時間切れ）。通信環境を確認してください", e)
        } catch (e: ConnectException) {
            throw IOException("サーバーに接続できませんでした。通信環境を確認してください", e)
        } catch (e: SSLException) {
            throw IOException("安全な接続を確立できませんでした。端末の日時設定を確認してください", e)
        }
    }

    private fun request(method: String, path: String, body: String?, token: String?, prefer: String?): String {
        val conn = URL("$baseUrl$path").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer ${token ?: anonKey}")
            conn.setRequestProperty("Accept", "application/json")
            prefer?.let { conn.setRequestProperty("Prefer", it) }
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw SupabaseException(code, friendlyError(code, text))
            return text
        } finally {
            conn.disconnect()
        }
    }

    /** サーバーの英語のエラーを、利用者が次に何をすればいいか分かる日本語にする */
    private fun friendlyError(code: Int, body: String): String {
        val json = runCatching { JSONObject(body) }.getOrNull()
        val raw = listOf("msg", "error_description", "message", "error")
            .firstNotNullOfOrNull { k -> json?.optString(k)?.takeIf { it.isNotBlank() } }
            .orEmpty()
        val errorCode = json?.optString("error_code").orEmpty()
        val lower = raw.lowercase()
        return when {
            // データベースの関数が日本語で返したものはそのまま出す
            raw.any { it.code > 0x3000 } -> raw
            errorCode == "invalid_credentials" || "invalid login" in lower -> "メールアドレスかパスワードが違います"
            errorCode == "email_not_confirmed" || "not confirmed" in lower -> "メールの確認がまだ済んでいません。届いたメールのリンクを開いてください"
            errorCode == "user_already_exists" || "already registered" in lower -> "このメールアドレスはすでに登録されています。ログインしてください"
            errorCode == "weak_password" || "password should be" in lower -> "パスワードは 6 文字以上にしてください"
            errorCode == "email_address_invalid" || "validate email" in lower || "invalid format" in lower -> "メールアドレスの形式が正しくありません"
            errorCode == "over_email_send_rate_limit" || "rate limit" in lower -> "短時間に何度も送られました。しばらく待ってからもう一度お試しください"
            errorCode == "signup_disabled" || "signups not allowed" in lower -> "現在、新しいアカウントは作れません"
            code == 401 || code == 403 -> "ログインの有効期限が切れました。もう一度ログインしてください"
            code == 404 -> "サーバーに必要な設定が見つかりません（コード $code）"
            code == 429 -> "混み合っています。しばらく待ってからもう一度お試しください"
            code in 500..599 -> "サーバー側で一時的なエラーが起きています（コード $code）"
            else -> "うまくいきませんでした（コード $code）"
        }
    }

    /** Supabase の日時（2026-09-24T13:31:07.279+00:00 など）をミリ秒に。Android 7 でも動くよう java.time は使わない */
    fun parseTime(value: String?): Long {
        if (value.isNullOrBlank() || value.length < 19) return 0L
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        return runCatching { format.parse(value.substring(0, 19))?.time ?: 0L }.getOrDefault(0L)
    }
}
