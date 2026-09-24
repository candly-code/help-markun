package com.example.help_markun.hardware

import android.app.Activity
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV

enum class NfcStatus { Unsupported, Disabled, Ready }

/** タッチされたカードから読み取れた情報 */
data class CardInfo(
    /** カード固有 ID（FeliCa なら IDm）。DB 照合キーにも使う */
    val id: String,
    /** 人が読める種類名（例：交通系 IC カード） */
    val kind: String,
    /** 通信方式（例：FeliCa / NFC-A） */
    val technology: String,
    /** 交通系カードの残高（読めた場合のみ） */
    val balance: Int? = null,
    /** 表示用の詳細項目 */
    val details: List<Pair<String, String>> = emptyList(),
    /** NDEF レコードをテキスト化したもの */
    val ndef: List<String> = emptyList(),
    val techs: List<String> = emptyList(),
)

/**
 * NFC Reader Mode で FeliCa・MIFARE・IC カードなど、あらゆる NFC カードを待ち受ける。
 * Activity の onResume / onPause で start / stop を呼ぶ。
 */
class NfcCardReader(
    private val activity: Activity,
    private val onCard: (CardInfo) -> Unit,
) : NfcAdapter.ReaderCallback {

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    val status: NfcStatus
        get() = when {
            adapter == null -> NfcStatus.Unsupported
            !adapter.isEnabled -> NfcStatus.Disabled
            else -> NfcStatus.Ready
        }

    fun start() {
        if (status != NfcStatus.Ready) return
        adapter?.enableReaderMode(
            activity,
            this,
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V,
            null,
        )
    }

    fun stop() {
        runCatching { adapter?.disableReaderMode(activity) }
    }

    /** NFC スレッドで呼ばれるので、ここで同期的に読み取ってよい */
    override fun onTagDiscovered(tag: Tag) {
        onCard(runCatching { parse(tag) }.getOrElse { basicInfo(tag) })
    }

    private fun basicInfo(tag: Tag) = CardInfo(
        id = tag.id.toHex(),
        kind = "NFC カード",
        technology = "不明",
        techs = tag.shortTechs(),
    )

    private fun parse(tag: Tag): CardInfo {
        val techs = tag.techList.toSet()
        val details = mutableListOf<Pair<String, String>>()
        var kind = "NFC カード"
        var technology = "NFC"
        var balance: Int? = null

        when {
            NfcF::class.java.name in techs -> {
                val f = NfcF.get(tag)
                technology = "FeliCa"
                kind = "FeliCa カード"
                details += "IDm" to tag.id.toHex().chunked(4).joinToString(" ")
                details += "PMm" to f.manufacturer.toHex().chunked(4).joinToString(" ")
                details += "システムコード" to f.systemCode.toHex()
                balance = runCatching { readTransitBalance(f, tag.id) }.getOrNull()
                if (balance != null) kind = "交通系 IC カード"
                else if (f.systemCode.toHex() == "FE00") kind = "電子マネー / 社員証など"
            }
            NfcV::class.java.name in techs -> {
                val v = NfcV.get(tag)
                technology = "NFC-V"
                kind = "ICタグ（ISO 15693）"
                details += "DSFID" to "%02X".format(v.dsfId)
            }
            else -> {
                val isoDep = IsoDep::class.java.name in techs
                if (NfcA::class.java.name in techs) {
                    val a = NfcA.get(tag)
                    technology = "NFC-A"
                    details += "ATQA" to a.atqa.toHex()
                    details += "SAK" to "%02X".format(a.sak)
                }
                if (NfcB::class.java.name in techs) {
                    technology = "NFC-B"
                    kind = "IC カード（Type B）"
                    details += "アプリデータ" to NfcB.get(tag).applicationData.toHex()
                }
                if (isoDep) {
                    val d = IsoDep.get(tag)
                    if (technology == "NFC-A") kind = "IC カード（Type A）"
                    (d.historicalBytes ?: d.hiLayerResponse)?.takeIf { it.isNotEmpty() }?.let {
                        details += "応答データ" to it.toHex()
                    }
                }
                if (MifareClassic::class.java.name in techs) {
                    val m = MifareClassic.get(tag)
                    kind = "MIFARE Classic"
                    details += "容量" to "${m.size} バイト / ${m.sectorCount} セクタ"
                }
                if (MifareUltralight::class.java.name in techs) {
                    kind = when (MifareUltralight.get(tag).type) {
                        MifareUltralight.TYPE_ULTRALIGHT_C -> "MIFARE Ultralight C"
                        else -> "MIFARE Ultralight / NTAG"
                    }
                }
            }
        }

        details += "UID の長さ" to "${tag.id.size} バイト"

        val ndef = runCatching { readNdef(tag) }.getOrDefault(emptyList())
        if (ndef.isNotEmpty() && kind == "NFC カード") kind = "NFC タグ"

        return CardInfo(
            id = tag.id.toHex(),
            kind = kind,
            technology = technology,
            balance = balance,
            details = details,
            ndef = ndef,
            techs = tag.shortTechs(),
        )
    }

    /**
     * 交通系カード（Suica・PASMO・ICOCA など）の利用履歴サービス 0x090F の先頭ブロックを
     * 暗号化なしで読み、残高を取り出す。対応していないカードでは例外 or null。
     */
    private fun readTransitBalance(f: NfcF, idm: ByteArray): Int? {
        f.connect()
        try {
            val cmd = byteArrayOf(
                0, 0x06, *idm,
                0x01, 0x0F, 0x09, // サービス 1 個：0x090F（リトルエンディアン）
                0x01, 0x80.toByte(), 0x00, // ブロック 1 個：0 番
            )
            cmd[0] = cmd.size.toByte()
            val res = f.transceive(cmd)
            // [len, 0x07, IDm(8), status1, status2, blocks, data(16)]
            if (res.size < 29 || res[1] != 0x07.toByte() || res[10] != 0.toByte()) return null
            val data = res.copyOfRange(13, 29)
            return (data[10].toInt() and 0xFF) or ((data[11].toInt() and 0xFF) shl 8)
        } finally {
            runCatching { f.close() }
        }
    }

    private fun readNdef(tag: Tag): List<String> {
        val message = Ndef.get(tag)?.cachedNdefMessage ?: return emptyList()
        return message.records.mapNotNull { it.describe() }
    }
}

private fun NdefRecord.describe(): String? {
    toUri()?.let { return it.toString() }
    if (tnf == NdefRecord.TNF_WELL_KNOWN && type.contentEquals(NdefRecord.RTD_TEXT) && payload.isNotEmpty()) {
        val status = payload[0].toInt()
        val langLen = status and 0x3F
        val charset = if (status and 0x80 == 0) Charsets.UTF_8 else Charsets.UTF_16
        return String(payload, 1 + langLen, payload.size - 1 - langLen, charset)
    }
    if (tnf == NdefRecord.TNF_MIME_MEDIA) return "データ（${String(type, Charsets.US_ASCII)}）"
    return null
}

private fun Tag.shortTechs() = techList.map { it.substringAfterLast('.') }

private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

