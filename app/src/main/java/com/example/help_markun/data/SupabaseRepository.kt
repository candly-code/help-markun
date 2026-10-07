package com.example.help_markun.data

import com.example.help_markun.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import java.net.URL
import java.net.URLEncoder

/**
 * Supabase の REST API（PostgREST）を直接呼び出すリポジトリ。
 * 追加 SDK なしで動くよう HttpURLConnection を使っている。
 */
class SupabaseRepository(
    private val baseUrl: String = BuildConfig.SUPABASE_URL.trimEnd('/'),
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY,
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && anonKey.isNotBlank()

    /** BLE ID が登録されている全プロフィールを、正規化済みキー → プロフィールの形で返す */
    suspend fun fetchBleProfiles(): Map<String, HelpProfile> =
        query("select=*&ble_id=not.is.null")
            .mapNotNull { p -> p.bleId?.let { normalizeKey(it) to p } }
            .toMap()

    /** カード ID（FeliCa の IDm や NFC の UID）でプロフィールを 1 件取得（未登録なら null） */
    suspend fun fetchByCardId(id: String): HelpProfile? =
        query("select=*&felica_idm=eq.${encode(normalizeKey(id))}&limit=1").firstOrNull()

    private suspend fun query(params: String): List<HelpProfile> = withContext(Dispatchers.IO) {
        if (!isConfigured) throw IOException("Supabase の URL / キーが未設定です")
        try {
            request(params)
        } catch (e: UnknownHostException) {
            throw IOException("インターネットに接続できません。通信環境を確認してください", e)
        } catch (e: SocketTimeoutException) {
            throw IOException("データベースに接続できませんでした（時間切れ）。通信環境を確認してください", e)
        } catch (e: ConnectException) {
            throw IOException("データベースに接続できませんでした。通信環境を確認してください", e)
        } catch (e: SSLException) {
            throw IOException("安全な接続を確立できませんでした。端末の日時設定を確認してください", e)
        }
    }

    private fun request(params: String): List<HelpProfile> {
        val conn = URL("$baseUrl/rest/v1/$TABLE?$params").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.setRequestProperty("Accept", "application/json")

            val code = conn.responseCode
            if (code !in 200..299) {
                throw IOException(
                    when (code) {
                        401, 403 -> "データベースの接続キーが正しくありません（コード $code）"
                        404 -> "データベースに help_profiles テーブルが見つかりません（コード $code）"
                        in 500..599 -> "データベース側で一時的なエラーが起きています（コード $code）"
                        else -> "データベースから読み込めませんでした（コード $code）"
                    }
                )
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(body)
            return List(array.length()) { HelpProfile.fromJson(array.getJSONObject(it)) }
        } finally {
            conn.disconnect()
        }
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val TABLE = "help_profiles"
    }
}
