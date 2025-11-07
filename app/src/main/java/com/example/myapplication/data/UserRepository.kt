package com.example.myapplication.data

import com.example.myapplication.model.User
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object UserRepository {

    private val client get() = SupabaseProvider.client

    @Serializable
    private data class UserResponse(
        val user_id: String,
        val username: String,
        val email: String? = null,
        val password: String,
        @SerialName("profile_picture_url")
        val profilePictureUrl: String? = null,
        val created_at: String? = null
    ) {
        fun toUser(): User = User(
            userId = user_id,
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

        val user = remoteUser.toUser().copy(password = "")
        if (user.userId.isBlank()) {
            throw IllegalStateException("Account is missing an identifier")
        }
        user
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

        val user = insertedUser.toUser().copy(password = "")
        if (user.userId.isBlank()) {
            throw IllegalStateException("Account is missing an identifier")
        }
        user
    }

    suspend fun getUserById(userId: String): User? = withContext(Dispatchers.IO) {
        fetchSingleUser {
            eq("user_id", userId)
        }?.toUser()?.copy(password = "")
    }

    suspend fun updateUser(
        userId: String,
        username: String,
        email: String,
        newPassword: String?
    ): User = withContext(Dispatchers.IO) {
        val conflictingUser = fetchSingleUser {
            eq("username", username)
        }
        if (conflictingUser != null && conflictingUser.user_id != userId) {
            throw IllegalArgumentException("Username already taken")
        }

        client.from("user")
            .update({
                set("username", username)
                set("email", email)
                if (!newPassword.isNullOrBlank()) {
                    set("password", newPassword)
                }
            }) {
                filter { eq("user_id", userId) }
                select(columns = Columns.ALL)
            }
            .decodeSingle<UserResponse>()
            .toUser()
            .copy(password = "")
    }

    private suspend fun fetchSingleUser(builder: PostgrestFilterBuilder.() -> Unit): UserResponse? {
        val result = client.from("user")
            .select(columns = Columns.ALL) {
                filter { builder() }
                limit(1)
            }
            .decodeList<UserResponse>()
        return result.firstOrNull()
    }
}
