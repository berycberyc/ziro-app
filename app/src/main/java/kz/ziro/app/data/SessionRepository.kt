package kz.ziro.app.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

@Serializable
data class TestSession(
    val id: String,
    val title_kk: String,
    val title_ru: String,
    val session_date: String? = null,
    val is_checking: Boolean = false
)

class SessionRepository {
    private val client = SupabaseProvider.client

    suspend fun getAll(): List<TestSession> {
        return client.postgrest["test_sessions"]
            .select()
            .decodeList<TestSession>()
    }
}
