package com.example.help_markun.hardware

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** 1 回分のスキャン結果（UI やマッチングに必要な情報だけ） */
data class BleSighting(
    val address: String,
    val name: String?,
    val rssi: Int,
)

class BleScanner(context: Context) {

    private val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter

    val isSupported: Boolean get() = adapter != null
    val isEnabled: Boolean get() = adapter?.isEnabled == true

    /**
     * collect している間だけ BLE スキャンを行う。
     * [filters] を渡すと画面オフ中もスキャンが続く（Android はフィルタなしのスキャンを画面オフで止めるため）。
     */
    @SuppressLint("MissingPermission")
    fun scan(
        filters: List<ScanFilter>? = null,
        mode: Int = ScanSettings.SCAN_MODE_LOW_LATENCY,
    ): Flow<BleSighting> = callbackFlow {
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            close(IllegalStateException("Bluetooth がオフになっています"))
            return@callbackFlow
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                trySend(result.toSighting())
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { trySend(it.toSighting()) }
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("BLE スキャンに失敗しました（コード $errorCode）"))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(mode)
            .build()
        scanner.startScan(filters, settings, callback)

        awaitClose { runCatching { scanner.stopScan(callback) } }
    }

    @SuppressLint("MissingPermission")
    private fun ScanResult.toSighting() = BleSighting(
        address = device.address,
        // scanRecord の名前は CONNECT 権限なしで読める。無ければ端末キャッシュ名を試す
        name = scanRecord?.deviceName ?: runCatching { device.name }.getOrNull(),
        rssi = rssi,
    )

    companion object {
        /** スキャンに必要な実行時権限 */
        val requiredPermissions: Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
    }
}
