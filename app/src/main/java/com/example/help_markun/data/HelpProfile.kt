package com.example.help_markun.data

import org.json.JSONObject

/** Supabase の help_profiles テーブル 1 行分 */
data class HelpProfile(
    val id: String,
    val displayName: String,
    val felicaIdm: String?,
    val bleId: String?,
    val disability: String?,
    val helpRequest: String?,
    val communication: String?,
    val bloodType: String?,
    val allergies: String?,
    val medications: String?,
    val medicalNotes: String?,
    val emergencyContactName: String?,
    val emergencyContactPhone: String?,
) {
    /** アバター表示用のイニシャル（1〜2 文字） */
    val initials: String
        get() = displayName.trim().take(if (displayName.any { it.code > 0x7F }) 1 else 2).uppercase()

    companion object {
        fun fromJson(json: JSONObject) = HelpProfile(
            id = json.optString("id"),
            displayName = json.optNullableString("display_name") ?: "名前未登録",
            felicaIdm = json.optNullableString("felica_idm"),
            bleId = json.optNullableString("ble_id"),
            disability = json.optNullableString("disability"),
            helpRequest = json.optNullableString("help_request"),
            communication = json.optNullableString("communication"),
            bloodType = json.optNullableString("blood_type"),
            allergies = json.optNullableString("allergies"),
            medications = json.optNullableString("medications"),
            medicalNotes = json.optNullableString("medical_notes"),
            emergencyContactName = json.optNullableString("emergency_contact_name"),
            emergencyContactPhone = json.optNullableString("emergency_contact_phone"),
        )
    }
}

private fun JSONObject.optNullableString(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

/** BLE アドレス・デバイス名・IDm を比較用にそろえる */
fun normalizeKey(raw: String): String = raw.trim().uppercase()
