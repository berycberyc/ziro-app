package kz.ziro.app.data

import io.github.jan.supabase.postgrest.postgrest

class TestTypeRepository {
    private val client = SupabaseProvider.client

    suspend fun getAll(): List<TestType> {
        return client.postgrest["test_types"]
            .select()
            .decodeList<TestType>()
    }

    suspend fun create(testType: TestType): Boolean {
        return try {
            client.postgrest["test_types"].insert(testType)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun delete(id: String): Boolean {
        return try {
            client.postgrest["test_types"].delete {
                filter { eq("id", id) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
