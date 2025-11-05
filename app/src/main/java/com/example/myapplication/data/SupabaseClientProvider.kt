package com.example.myapplication.data

import com.example.myapplication.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Lazily initialises a single [SupabaseClient] instance that the rest of the
 * application can use to communicate with the Supabase backend.
 */
object SupabaseClientProvider {

    /**
     * A singleton [SupabaseClient] configured with the project's Supabase URL and anon key.
     *
     * The URL and key are supplied through Gradle secrets and exposed to the runtime via
     * [BuildConfig]. Both values must be provided before the client can be used.
     */
    val client: SupabaseClient by lazy {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val supabaseKey = BuildConfig.SUPABASE_ANON_KEY

        require(supabaseUrl.isNotBlank()) {
            "Supabase URL is missing. Define SUPABASE_URL in secrets.properties."
        }
        require(supabaseKey.isNotBlank()) {
            "Supabase anon key is missing. Define SUPABASE_ANON_KEY in secrets.properties."
        }

        createSupabaseClient(supabaseUrl, supabaseKey) {
            install(Postgrest)
            install(GoTrue)
        }
    }
}
