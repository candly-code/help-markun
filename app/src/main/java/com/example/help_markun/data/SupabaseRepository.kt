package com.example.help_markun.data

import com.example.help_markun.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
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
                val detail = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("Supabase エラー ($code) $detail".trim())
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(body)
            List(array.length()) { HelpProfile.fromJson(array.getJSONObject(it)) }
        } finally {
            conn.disconnect()
        }
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val TABLE = "help_profiles"
    }
}
