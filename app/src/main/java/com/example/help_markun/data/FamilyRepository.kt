package com.example.help_markun.data

import org.json.JSONArray
import org.json.JSONObject

data class FamilyMember(val userId: String, val name: String, val joinedAt: Long)

/** 家族グループで見守っている方 */
data class WatchedPerson(val profileId: String, val name: String, val disability: String?)

data class Family(
    val id: String,
    val name: String,
    val inviteCode: String,
    val members: List<FamilyMember>,
    val watched: List<WatchedPerson>,
)

enum class FamilyEventKind(val key: String) {
    /** 誰かのアプリの近くで、その方のヘルプタグが見つかった */
    Nearby("nearby"),
    /** その方のカードが読み取られた */
    Card("card"),
    /** 見つけた人から家族へのメッセージ */
    Message("message");

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: Message
    }
}

data class FamilyEvent(
    val id: Long,
    val profileId: String,
    val personName: String,
    val kind: FamilyEventKind,
    val message: String?,
    val senderName: String?,
    val reporter: String?,
    val createdAt: Long,
) {
    /** 通知や一覧の見出し */
    val title: String
        get() = when (kind) {
            FamilyEventKind.Nearby -> "${personName}さんが見つかりました"
            FamilyEventKind.Card -> "${personName}さんのカードが読まれました"
            FamilyEventKind.Message -> "${personName}さんについての連絡"
        }

    /** 補足（メッセージのときだけ本文と送り主） */
    val body: String?
        get() = when (kind) {
            FamilyEventKind.Message -> (message ?: "連絡がありました") + (senderName?.let { "（$it）" } ?: "")
            else -> null
        }
}

/** 家族グループ（ログインが必要）と、家族へのお知らせ（ログインなしでも送れる） */
class FamilyRepository {

    /** 自分の家族グループ。入っていなければ null */
    suspend fun fetchFamily(token: String): Family? {
        val text = SupabaseHttp.call(
            "GET",
            "/rest/v1/families?select=id,name,invite_code,family_members(user_id,display_name,joined_at)&limit=1",
            token = token,
        )
        val array = JSONArray(text)
        if (array.length() == 0) return null
        val f = array.getJSONObject(0)
        val id = f.getString("id")
        val members = f.optJSONArray("family_members").objects().map {
            FamilyMember(it.optString("user_id"), it.optString("display_name"), SupabaseHttp.parseTime(it.optString("joined_at")))
        }.sortedBy { it.joinedAt }
        val watched = JSONArray(
            SupabaseHttp.call(
                "GET",
                "/rest/v1/help_profiles?select=id,display_name,disability&family_id=eq.$id&order=display_name",
                token = token,
            )
        ).objects().map { WatchedPerson(it.optString("id"), it.optString("display_name"), it.nullable("disability")) }
        return Family(id, f.optString("name"), f.optString("invite_code"), members, watched)
    }

    suspend fun createFamily(token: String, name: String, memberName: String) {
        rpc(token, "create_family", JSONObject().put("p_name", name).put("p_member_name", memberName))
    }

    suspend fun joinFamily(token: String, code: String, memberName: String) {
        rpc(token, "join_family", JSONObject().put("p_code", code).put("p_member_name", memberName))
    }

    suspend fun leaveFamily(token: String) = rpc(token, "leave_family", JSONObject())

    suspend fun unwatchProfile(token: String, profileId: String) =
        rpc(token, "unwatch_profile", JSONObject().put("p_profile", profileId))

    /** 家族へのお知らせ（新しい順）。afterId より新しいものだけにもできる */
    suspend fun fetchEvents(token: String, afterId: Long = -1L, limit: Int = 30): List<FamilyEvent> {
        val filter = if (afterId >= 0) "&id=gt.$afterId" else ""
        val text = SupabaseHttp.call(
            "GET",
            "/rest/v1/help_events?select=id,profile_id,kind,message,sender_name,reporter,created_at,help_profiles(display_name)" +
                "&order=id.desc&limit=$limit$filter",
            token = token,
        )
        return JSONArray(text).objects().map {
            FamilyEvent(
                id = it.optLong("id"),
                profileId = it.optString("profile_id"),
                personName = it.optJSONObject("help_profiles")?.optString("display_name")?.takeIf { n -> n.isNotBlank() } ?: "見守っている方",
                kind = FamilyEventKind.of(it.optString("kind")),
                message = it.nullable("message"),
                senderName = it.nullable("sender_name"),
                reporter = it.nullable("reporter"),
                createdAt = SupabaseHttp.parseTime(it.optString("created_at")),
            )
        }
    }

    /**
     * その方の家族に知らせる。家族が登録されていない方への送信や、短時間の重複はサーバー側で自動的に捨てられる。
     * token が null ならログインなしで送る。
     */
    suspend fun report(profileId: String, kind: FamilyEventKind, message: String? = null, senderName: String? = null, token: String? = null) {
        val body = JSONObject()
            .put("profile_id", profileId)
            .put("kind", kind.key)
            .put("message", message?.take(200) ?: JSONObject.NULL)
            .put("sender_name", senderName?.take(40) ?: JSONObject.NULL)
        SupabaseHttp.call("POST", "/rest/v1/help_events", body.toString(), token = token, prefer = "return=minimal")
    }

    private suspend fun rpc(token: String, name: String, args: JSONObject) {
        SupabaseHttp.call("POST", "/rest/v1/rpc/$name", args.toString(), token = token)
    }
}

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else List(length()) { getJSONObject(it) }

private fun JSONObject.nullable(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
