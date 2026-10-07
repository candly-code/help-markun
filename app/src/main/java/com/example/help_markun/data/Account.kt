package com.example.help_markun.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/** ログイン中のアカウント（Supabase Auth のセッション） */
data class Session(
    val accessToken: String,
    val refreshToken: String,
    /** アクセストークンの有効期限（ミリ秒） */
    val expiresAt: Long,
    val userId: String,
    val email: String,
    val name: String,
)

sealed interface SignUpResult {
    data class SignedIn(val session: Session) : SignUpResult
    /** 確認メールのリンクを開くまでログインできない設定のとき */
    data class NeedsConfirmation(val email: String) : SignUpResult
}

/**
 * アカウントの保存先。アプリ本体と常駐サービスの両方から使うので、プロセスで 1 つだけ作る。
 * ログインしていなくてもアプリはすべて使える（ログインは家族機能のためだけ）。
 */
class AccountStore private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("account", Context.MODE_PRIVATE)
    private val refreshLock = Mutex()

    private val _session = MutableStateFlow(load())
    val session: StateFlow<Session?> = _session.asStateFlow()

    /** 家族グループに入っているか（サービスをすぐ起動してよいかの判断に使う） */
    var inFamily: Boolean
        get() = prefs.getBoolean(KEY_IN_FAMILY, false)
        set(value) = prefs.edit().putBoolean(KEY_IN_FAMILY, value).apply()

    /** 通知済み・既読にした家族のお知らせの最後の番号 */
    var lastNotifiedEventId: Long
        get() = prefs.getLong(KEY_LAST_NOTIFIED, -1L)
        set(value) = prefs.edit().putLong(KEY_LAST_NOTIFIED, value).apply()

    var lastReadEventId: Long
        get() = prefs.getLong(KEY_LAST_READ, -1L)
        set(value) = prefs.edit().putLong(KEY_LAST_READ, value).apply()

    fun save(session: Session) {
        prefs.edit()
            .putString(KEY_ACCESS, session.accessToken)
            .putString(KEY_REFRESH, session.refreshToken)
            .putLong(KEY_EXPIRES, session.expiresAt)
            .putString(KEY_USER, session.userId)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_NAME, session.name)
            .apply()
        _session.value = session
    }

    fun clear() {
        prefs.edit().clear().apply()
        _session.value = null
    }

    /** 期限切れが近ければ更新してから返す。更新できなければ（ログアウトされていれば）null */
    suspend fun validSession(auth: AuthRepository = AuthRepository()): Session? = refreshLock.withLock {
        val s = _session.value ?: return null
        if (s.expiresAt - System.currentTimeMillis() > 60_000L) return s
        return runCatching { auth.refresh(s.refreshToken) }
            .onSuccess { save(it) }
            .getOrElse { e ->
                // 通信できないだけなら今のトークンのまま。トークンが無効なら本当にログアウト
                if (e is SupabaseException && e.code in 400..499) {
                    clear()
                    null
                } else {
                    s
                }
            }
    }

    private fun load(): Session? {
        val access = prefs.getString(KEY_ACCESS, null) ?: return null
        return Session(
            accessToken = access,
            refreshToken = prefs.getString(KEY_REFRESH, null) ?: return null,
            expiresAt = prefs.getLong(KEY_EXPIRES, 0L),
            userId = prefs.getString(KEY_USER, null) ?: return null,
            email = prefs.getString(KEY_EMAIL, "").orEmpty(),
            name = prefs.getString(KEY_NAME, "").orEmpty(),
        )
    }

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_EXPIRES = "expires_at"
        private const val KEY_USER = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_NAME = "name"
        private const val KEY_IN_FAMILY = "in_family"
        private const val KEY_LAST_NOTIFIED = "last_notified_event"
        private const val KEY_LAST_READ = "last_read_event"

        @Volatile
        private var instance: AccountStore? = null

        fun get(context: Context): AccountStore =
            instance ?: synchronized(this) { instance ?: AccountStore(context).also { instance = it } }
    }
}

/** Supabase Auth（メールアドレス＋パスワード） */
class AuthRepository {

    suspend fun signIn(email: String, password: String): Session {
        val body = JSONObject().put("email", email.trim()).put("password", password).toString()
        return parseSession(SupabaseHttp.call("POST", "/auth/v1/token?grant_type=password", body))
    }

    suspend fun signUp(email: String, password: String, name: String): SignUpResult {
        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .put("data", JSONObject().put("name", name.trim()))
            .toString()
        val json = JSONObject(SupabaseHttp.call("POST", "/auth/v1/signup", body))
        // メール確認が必要な設定だとセッションは返らない
        return if (json.has("access_token")) SignUpResult.SignedIn(parseSession(json.toString()))
        else SignUpResult.NeedsConfirmation(email.trim())
    }

    suspend fun refresh(refreshToken: String): Session {
        val body = JSONObject().put("refresh_token", refreshToken).toString()
        return parseSession(SupabaseHttp.call("POST", "/auth/v1/token?grant_type=refresh_token", body))
    }

    suspend fun signOut(accessToken: String) {
        runCatching { SupabaseHttp.call("POST", "/auth/v1/logout", "{}", token = accessToken) }
    }

    private fun parseSession(text: String): Session {
        val json = JSONObject(text)
        val user = json.getJSONObject("user")
        val meta = user.optJSONObject("user_metadata")
        val email = user.optString("email")
        return Session(
            accessToken = json.getString("access_token"),
            refreshToken = json.getString("refresh_token"),
            expiresAt = System.currentTimeMillis() + json.optLong("expires_in", 3600L) * 1000L,
            userId = user.getString("id"),
            email = email,
            name = meta?.optString("name")?.takeIf { it.isNotBlank() } ?: email.substringBefore('@'),
        )
    }
}
