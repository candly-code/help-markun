package com.example.help_markun.ui

import android.app.Application
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.help_markun.data.AccountStore
import com.example.help_markun.data.AuthRepository
import com.example.help_markun.data.Family
import com.example.help_markun.data.FamilyEvent
import com.example.help_markun.data.FamilyEventKind
import com.example.help_markun.data.FamilyRepository
import com.example.help_markun.data.HelpProfile
import com.example.help_markun.data.Session
import com.example.help_markun.data.SignUpResult
import com.example.help_markun.data.SupabaseHttp
import com.example.help_markun.service.FamilyNotifier
import com.example.help_markun.service.HelpScanService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AccountState(
    val session: Session? = null,
    val family: Family? = null,
    /** 家族グループの情報を読み込み中 */
    val loading: Boolean = false,
    /** ログイン・参加などの操作中 */
    val busy: Boolean = false,
    val events: List<FamilyEvent> = emptyList(),
    val lastReadId: Long = -1L,
    val error: String? = null,
    /** 確認メールを送った、などの案内 */
    val notice: String? = null,
) {
    val signedIn: Boolean get() = session != null
    val unread: Int get() = events.count { it.id > lastReadId && it.reporter != session?.userId }
}

/**
 * アカウントと家族グループ。ログインしなくてもアプリの機能はすべて使え、
 * ログインして家族グループに入ると、見守っている方のお知らせが家族全員に届く。
 */
class AccountViewModel(app: Application) : AndroidViewModel(app) {

    private val store = AccountStore.get(app)
    private val auth = AuthRepository()
    private val family = FamilyRepository()

    val configured = SupabaseHttp.isConfigured

    private val _state = MutableStateFlow(AccountState(session = store.session.value, lastReadId = store.lastReadEventId))
    val state: StateFlow<AccountState> = _state.asStateFlow()

    private var pollJob: Job? = null

    /** アカウントのシートを開いているか（家族のお知らせの通知から開いた時も使う） */
    private val _sheetOpen = MutableStateFlow(false)
    val sheetOpen: StateFlow<Boolean> = _sheetOpen.asStateFlow()

    fun openSheet() {
        _sheetOpen.value = true
        refresh()
    }

    fun closeSheet() {
        _sheetOpen.value = false
        consumeMessages()
    }

    init {
        // サービス側でトークンが更新・失効した場合も画面に反映する
        viewModelScope.launch {
            store.session.collect { s ->
                val lost = _state.value.session != null && s == null
                _state.update {
                    if (s == null) it.copy(session = null, family = null, events = emptyList())
                    else it.copy(session = s)
                }
                if (lost) store.inFamily = false
            }
        }
        refresh()
    }

    // ---- ログイン ----

    fun signIn(email: String, password: String) = run("ログインしました") {
        store.save(auth.signIn(email, password))
        loadFamily()
    }

    fun signUp(email: String, password: String, name: String) = run(null) {
        when (val r = auth.signUp(email, password, name)) {
            is SignUpResult.SignedIn -> {
                store.save(r.session)
                _state.update { it.copy(notice = "アカウントを作りました") }
                loadFamily()
            }
            is SignUpResult.NeedsConfirmation -> _state.update {
                it.copy(notice = "確認メールを送りました。リンクを開いてからログインしてください")
            }
        }
    }

    fun signOut() {
        val token = store.session.value?.accessToken
        store.clear()
        _state.update { AccountState(notice = "ログアウトしました") }
        stopWatchIfIdle()
        viewModelScope.launch { token?.let { auth.signOut(it) } }
    }

    // ---- 家族グループ ----

    fun createFamily(name: String) = run("家族グループを作りました") {
        val s = requireSession()
        family.createFamily(s.accessToken, name, s.name)
        loadFamily()
    }

    fun joinFamily(code: String) = run("家族グループに参加しました") {
        val s = requireSession()
        family.joinFamily(s.accessToken, code, s.name)
        loadFamily()
    }

    fun leaveFamily() = run("家族グループから抜けました") {
        family.leaveFamily(requireSession().accessToken)
        loadFamily()
    }

    fun unwatch(profileId: String, name: String) = run("${name}さんを見守りから外しました") {
        family.unwatchProfile(requireSession().accessToken, profileId)
        loadFamily()
    }

    /** 見つけた人から、その方の家族へ連絡する（ログインなしでも送れる） */
    fun sendToFamily(profile: HelpProfile, message: String, senderName: String?, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val s = store.validSession()
            val ok = runCatching {
                family.report(
                    profile.id, FamilyEventKind.Message, message,
                    senderName = senderName?.takeIf { it.isNotBlank() } ?: s?.name,
                    token = s?.accessToken,
                )
            }.isSuccess
            onDone(ok)
        }
    }

    fun refresh() {
        if (store.session.value == null || !configured) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { loadFamily() }.onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(loading = false) }
        }
    }

    /** お知らせを見たので既読にする（アイコンのバッジと通知も片付ける） */
    fun markRead() {
        val newest = _state.value.events.maxOfOrNull { it.id } ?: return
        store.lastReadEventId = newest
        store.lastNotifiedEventId = maxOf(store.lastNotifiedEventId, newest)
        _state.update { it.copy(lastReadId = newest) }
        FamilyNotifier.clearFamilyNotifications(getApplication())
    }

    fun consumeMessages() = _state.update { it.copy(error = null, notice = null) }

    // ---- アプリの表示に合わせてお知らせを確認 ----

    fun onAppVisible() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                if (store.inFamily && store.session.value != null) {
                    FamilyNotifier.poll(getApplication(), notify = false)?.let { events ->
                        _state.update { it.copy(events = events) }
                    }
                }
                delay(POLL_MS)
            }
        }
    }

    fun onAppHidden() {
        pollJob?.cancel()
        pollJob = null
    }

    // ---- 内部処理 ----

    private suspend fun loadFamily() {
        val s = store.validSession() ?: return
        val f = family.fetchFamily(s.accessToken)
        store.inFamily = f != null
        val events = if (f != null) runCatching { family.fetchEvents(s.accessToken) }.getOrDefault(emptyList()) else emptyList()
        if (f != null && store.lastNotifiedEventId < 0) {
            // 参加前のお知らせをまとめて通知しないよう、ここまでを通知済みにする
            store.lastNotifiedEventId = events.maxOfOrNull { it.id } ?: 0L
        }
        _state.update { it.copy(session = s, family = f, events = events) }
        // 家族グループに入ったら、アプリを閉じていてもお知らせを受け取れるようにする
        if (f != null) HelpScanService.startIfAllowed(getApplication()) else stopWatchIfIdle()
    }

    /** 見守るもの（BLE の許可・家族グループ）がなくなったら、常駐サービスをすぐ止める */
    private fun stopWatchIfIdle() {
        if (!HelpScanService.canRun(getApplication())) HelpScanService.stop(getApplication())
    }

    private suspend fun requireSession(): Session =
        store.validSession() ?: throw IllegalStateException("ログインの有効期限が切れました。もう一度ログインしてください")

    private fun run(success: String?, block: suspend () -> Unit) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, notice = null) }
            runCatching { block() }
                .onSuccess { if (success != null) _state.update { it.copy(notice = success) } }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "うまくいきませんでした") } }
            _state.update { it.copy(busy = false) }
        }
    }

    private companion object {
        const val POLL_MS = 20_000L
    }
}

/** 画面のどこからでもアカウント・家族機能を呼べるようにする */
val LocalAccount = staticCompositionLocalOf<AccountViewModel?> { null }
