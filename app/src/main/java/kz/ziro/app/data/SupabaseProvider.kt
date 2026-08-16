package kz.ziro.app.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Same Supabase project as the website — the app talks to the same backend,
 * the same users, the same tables (profiles, test_types, test_sessions, etc.)
 */
object SupabaseProvider {

    // TODO: replace with your real Supabase URL and Publishable (anon) key,
    // same values used in the website's environment variables.
    private const val SUPABASE_URL = "https://phpuhsdshorbvdlvianw.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_7d0t3FAjRtM-BWl79NqLig_xb4FfnBv"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(GoTrue)
        install(Postgrest)
    }
}
