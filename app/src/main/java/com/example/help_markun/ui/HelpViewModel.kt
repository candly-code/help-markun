package com.example.help_markun.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.help_markun.data.HelpProfile
import com.example.help_markun.data.SupabaseRepository
import com.example.help_markun.data.normalizeKey
import com.example.help_markun.hardware.BleScanner
import com.example.help_markun.hardware.CardInfo
import com.example.help_markun.hardware.Haptics
import com.example.help_markun.hardware.NfcStatus
import com.example.help_markun.ui.components.signalLevel
import com.example.help_markun.ui.theme.AppSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppTab { Nearby, Card }

data class NearbyDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val firstSeen: Long,
    val lastSeen: Long,
    val match: HelpProfile?,
    /** 電波の途切れ具合：0 = 受信中、1 = 弱まっている、2 = 途切れている（まもなく消える） */
    val fade: Int = 0,
) {
    val isMatch: Boolean get() = match != null
}

data class NearbyState(
    val scanning: Boolean = false,
    val devices: List<NearbyDevice> = emptyList(),
    val registeredCount: Int = 0,
    val syncing: Boolean = false,
    val error: String? = null,
    /** 最後に登録情報を同期できた時刻 */
    val lastSyncedAt: Long? = null,
) {
    val matches: List<NearbyDevice> get() = devices.filter { it.isMatch }
}

sealed interface CardState {
    data object Waiting : CardState
    data class Loading(val card: CardInfo) : CardState
    /** profile は DB に登録があった場合のみ。lookupNote は照合できなかった理由 */
    data class Result(val card: CardInfo, val profile: HelpProfile?, val lookupNote: String?) : CardState
}

class HelpViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = SupabaseRepository()
    private val scanner = BleScanner(app)
    private val haptics = Haptics(app)

    val supabaseConfigured = repository.isConfigured
    val bleSupported = scanner.isSupported

    private val _tab = MutableStateFlow(AppTab.Nearby)
    val tab: StateFlow<AppTab> = _tab.asStateFlow()

    private val _nearby = MutableStateFlow(NearbyState())
    val nearby: StateFlow<NearbyState> = _nearby.asStateFlow()

    private val _card = MutableStateFlow<CardState>(CardState.Waiting)
    val card: StateFlow<CardState> = _card.asStateFlow()

    private val _nfcStatus = MutableStateFlow(NfcStatus.Unsupported)
    val nfcStatus: StateFlow<NfcStatus> = _nfcStatus.asStateFlow()

    // 設定（端末内に保存して次回も同じにする）
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun updateSettings(s: AppSettings) {
        _settings.value = s
        prefs.edit()
            .putString("theme_mode", s.themeMode.name)
            .putString("text_size", s.textSize.name)
            .putBoolean("reduce_motion", s.reduceMotion)
            .putBoolean("alert_vibration", s.alertVibration)
            .putBoolean("touch_feedback", s.touchFeedback)
            .putInt("fade_out_seconds", s.fadeOutSeconds)
            .putBoolean("hide_far", s.hideFarDevices)
            .putBoolean("background_watch", s.backgroundWatch)
            .apply()
        publish()
    }

    private fun loadSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            themeMode = enumOr(prefs.getString("theme_mode", null), d.themeMode),
            textSize = enumOr(prefs.getString("text_size", null), d.textSize),
            reduceMotion = prefs.getBoolean("reduce_motion", d.reduceMotion),
            alertVibration = prefs.getBoolean("alert_vibration", d.alertVibration),
            touchFeedback = prefs.getBoolean("touch_feedback", d.touchFeedback),
            fadeOutSeconds = prefs.getInt("fade_out_seconds", d.fadeOutSeconds),
            hideFarDevices = prefs.getBoolean("hide_far", d.hideFarDevices),
            backgroundWatch = prefs.getBoolean("background_watch", d.backgroundWatch),
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        runCatching { enumValueOf<E>(name ?: "") }.getOrDefault(default)

    /** 正規化した BLE ID → 登録プロフィール */
    private var bleProfiles: Map<String, HelpProfile> = emptyMap()

    /** スキャン中に見つかった端末（メインスレッドからのみ触る） */
    private val sightings = LinkedHashMap<String, NearbyDevice>()

    /** すでに振動で知らせた一致端末（離れて戻ってきたら再通知する） */
    private val alerted = mutableSetOf<String>()

    private var scanJob: Job? = null
    private var tickerJob: Job? = null
    private var resumeScanOnStart = false

    init {
        refreshProfiles()
    }

    fun selectTab(tab: AppTab) {
        _tab.value = tab
    }

    /** 最初に詳細を見せたい方（通知から開いた時など）。画面が表示したら consumeFocus で消す */
    private val _focus = MutableStateFlow<HelpProfile?>(null)
    val focus: StateFlow<HelpProfile?> = _focus.asStateFlow()

    fun focusOn(profile: HelpProfile) {
        _tab.value = AppTab.Nearby
        _focus.value = profile
    }

    fun consumeFocus() {
        _focus.value = null
    }

    fun isBluetoothEnabled() = scanner.isEnabled

    fun refreshProfiles() {
        if (!repository.isConfigured) {
            _nearby.update { it.copy(error = "Supabase の接続情報が未設定です") }
            return
        }
        viewModelScope.launch {
            _nearby.update { it.copy(syncing = true) }
            runCatching { repository.fetchBleProfiles() }
                .onSuccess { profiles ->
                    bleProfiles = profiles
                    // 既に見えている端末にも新しい登録情報を反映する
                    sightings.replaceAll { _, d -> d.copy(match = matchFor(d.address, d.name)) }
                    _nearby.update {
                        it.copy(
                            registeredCount = profiles.size,
                            syncing = false,
                            error = null,
                            lastSyncedAt = System.currentTimeMillis(),
                        )
                    }
                    publish()
                }
                .onFailure { e ->
                    _nearby.update { it.copy(syncing = false, error = e.message ?: "同期に失敗しました") }
                }
        }
    }

    fun startScan() {
        if (scanJob?.isActive == true) return
        _nearby.update { it.copy(scanning = true, error = if (repository.isConfigured) null else it.error) }

        scanJob = viewModelScope.launch {
            scanner.scan()
                .catch { e ->
                    _nearby.update { it.copy(error = e.message) }
                }
                .collect { s ->
                    val prev = sightings[s.address]
                    val now = System.currentTimeMillis()
                    // 名前はパケットによって欠けることがあるので、一度得た名前を保持する
                    val name = s.name ?: prev?.name
                    sightings[s.address] = NearbyDevice(
                        address = s.address,
                        name = name,
                        // RSSI は揺れが大きいので指数移動平均でならす
                        rssi = prev?.let { (it.rssi * 0.7f + s.rssi * 0.3f).toInt() } ?: s.rssi,
                        firstSeen = prev?.firstSeen ?: now,
                        lastSeen = now,
                        match = matchFor(s.address, name),
                    )
                }
            // フローが終了＝スキャン停止
            stopScan()
        }

        tickerJob = viewModelScope.launch {
            while (isActive) {
                publish()
                delay(TICK_MS)
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        tickerJob?.cancel()
        scanJob = null
        tickerJob = null
        _nearby.update { it.copy(scanning = false) }
    }

    /** Activity#onStop / onStart から呼ばれ、バックグラウンドでは電池を使わない */
    fun onAppHidden() {
        resumeScanOnStart = scanJob?.isActive == true
        stopScan()
    }

    fun onAppVisible() {
        if (resumeScanOnStart) startScan()
        resumeScanOnStart = false
    }

    fun clearDevices() {
        sightings.clear()
        alerted.clear()
        publish()
    }

    // ---- FeliCa ----

    fun onNfcStatus(status: NfcStatus) {
        _nfcStatus.value = status
    }

    /** NFC スレッドから呼ばれる。どんなカードでも結果を表示し、DB に登録があれば合わせて出す */
    fun onCard(card: CardInfo) {
        val current = _card.value
        // 同じカードを置きっぱなしにした時の連続読み取りを無視
        if (current is CardState.Loading && current.card.id == card.id) return

        viewModelScope.launch {
            if (_settings.value.touchFeedback) haptics.tick()
            _tab.value = AppTab.Card
            _card.value = CardState.Loading(card)
            _card.value = if (!repository.isConfigured) {
                CardState.Result(card, profile = null, lookupNote = "データベース未設定のため、カードの情報のみ表示しています")
            } else {
                runCatching { repository.fetchByCardId(card.id) }.fold(
                    onSuccess = { p -> CardState.Result(card, p, lookupNote = null) },
                    onFailure = { e -> CardState.Result(card, null, lookupNote = e.message ?: "照合に失敗しました") },
                )
            }
        }
    }

    fun resetCard() {
        _card.value = CardState.Waiting
    }

    // ---- 内部処理 ----

    private fun matchFor(address: String, name: String?): HelpProfile? =
        bleProfiles[normalizeKey(address)] ?: name?.let { bleProfiles[normalizeKey(it)] }

    private fun publish() {
        val now = System.currentTimeMillis()
        // 電波が途切れた端末は、いきなり消さずに段階的に薄くしてから外す
        val limit = _settings.value.fadeOutSeconds * 1000L
        sightings.entries.removeAll { now - it.value.lastSeen > limit }
        sightings.replaceAll { _, d ->
            val age = now - d.lastSeen
            val fade = when {
                age < FRESH_MS -> 0
                age < limit * 2 / 3 -> 1
                else -> 2
            }
            if (fade != d.fade) d.copy(fade = fade) else d
        }
        alerted.retainAll(sightings.keys)

        val newMatches = sightings.values.filter { it.isMatch && it.address !in alerted }
        if (newMatches.isNotEmpty()) {
            alerted += newMatches.map { it.address }
            if (_settings.value.alertVibration) haptics.alert()
        }

        // 電波強度で並べ替えると毎回順番が入れ替わるので、見つけた順で固定する
        val sorted = sightings.values.sortedWith(
            compareByDescending<NearbyDevice> { it.isMatch }
                .thenBy { it.firstSeen }
                .thenBy { it.address }
        )
        // 画面に出る内容（並び・名前・電波の段階・一致）が変わった時だけ通知して、無駄な再描画を防ぐ
        val old = _nearby.value.devices
        val changed = sorted.size != old.size || sorted.indices.any { i ->
            val a = sorted[i]
            val b = old[i]
            a.address != b.address || a.name != b.name || a.match != b.match || a.fade != b.fade ||
                signalLevel(a.rssi) != signalLevel(b.rssi)
        }
        if (changed) _nearby.update { it.copy(devices = sorted) }
    }

    override fun onCleared() {
        stopScan()
    }

    private companion object {
        const val TICK_MS = 1_000L
        /** これより長く受信がないと「弱まっている」扱い */
        const val FRESH_MS = 5_000L
    }
}
