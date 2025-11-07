package com.example.myapplication.data

import com.example.myapplication.model.User
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.PostgrestFilterBuilder
import io.github.jan.supabase.postgrest.result.decodeList
import io.github.jan.supabase.postgrest.result.decodeSingle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object UserRepository {

    private val client get() = SupabaseProvider.client

    @Serializable
    private data class UserResponse(
        val user_id: String? = null,
        val username: String,
        val email: String? = null,
        val password: String,
        @SerialName("profile_picture_url")
        val profilePictureUrl: String? = null,
        val created_at: String? = null
    ) {
        fun toUser(): User = User(
            userId = user_id.orEmpty(),
            username = username,
            email = email,
            password = password,
            profilePictureUrl = profilePictureUrl,
            createdAt = created_at
        )
    }

    suspend fun login(username: String, password: String): User = withContext(Dispatchers.IO) {
        val remoteUser = fetchSingleUser {
            eq("username", username)
        } ?: throw IllegalArgumentException("Account not found")

        if (remoteUser.password != password) {
            throw IllegalArgumentException("Incorrect password")
        }

        remoteUser.toUser().copy(password = "")
    }

    suspend fun signUp(username: String, email: String, password: String): User = withContext(Dispatchers.IO) {
        val existingUser = fetchSingleUser {
            eq("username", username)
        }
        if (existingUser != null) {
            throw IllegalArgumentException("Username already taken")
        }

        val insertedUser = client.from("user")
            .insert(
                mapOf(
                    "username" to username,
                    "email" to email,
                    "password" to password
                )
            ) {
                select(columns = Columns.ALL)
            }
            .decodeSingle<UserResponse>()

        insertedUser.toUser().copy(password = "")
    }

    private suspend fun fetchSingleUser(builder: PostgrestFilterBuilder.() -> Unit): UserResponse? {
        val result = client.from("user")
            .select(columns = Columns.ALL) {
                builder()
            }
            .decodeList<UserResponse>()
        return result.firstOrNull()
    }
}
