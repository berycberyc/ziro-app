package kz.ziro.app.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

class RegistrationRepository {
    private val client = SupabaseProvider.client

    /** All paid registrations for a session, with student info joined in. */
    suspend fun getForSession(sessionId: String): List<Registration> {
        return client.postgrest["registrations"]
            .select(Columns.raw("id, test_type_id, classroom, test_variant, payment_status, students(full_name, language)")) {
                filter { eq("test_session_id", sessionId) }
            }
            .decodeList<Registration>()
            .filter { it.payment_status == "paid" }
    }

    suspend fun getById(id: String): Registration? {
        return try {
            client.postgrest["registrations"]
                .select(Columns.raw("id, test_type_id, classroom, test_variant, payment_status, students(full_name, language)")) {
                    filter { eq("id", id) }
                }
                .decodeSingleOrNull<Registration>()
        } catch (e: Exception) {
            null
        }
    }
}
