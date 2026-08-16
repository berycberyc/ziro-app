package kz.ziro.app.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

sealed class LoginResult {
    data class Success(val role: String) : LoginResult()
    data class NotAllowed(val role: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

class AuthRepository {

    private val client = SupabaseProvider.client

    suspend fun login(email: String, password: String): LoginResult {
        return try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = client.auth.currentUserOrNull()?.id
                ?: return LoginResult.Error("Пайдаланушы табылмады")

            val profile = client.postgrest["profiles"]
                .select(columns = Columns.list("id", "full_name", "role")) {
                    filter { eq("id", userId) }
                }
                .decodeSingle<Profile>()

            if (profile.role == "admin" || profile.role == "teacher") {
                LoginResult.Success(profile.role)
            } else {
                client.auth.signOut()
                LoginResult.NotAllowed(profile.role)
            }
        } catch (e: Exception) {
            LoginResult.Error(e.message ?: "Белгісіз қате")
        }
    }

    suspend fun logout() {
        client.auth.signOut()
    }
}
