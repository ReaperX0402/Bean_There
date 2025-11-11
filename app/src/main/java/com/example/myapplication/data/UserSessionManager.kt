package com.example.myapplication.data

import android.content.Context
import com.example.myapplication.model.User
import androidx.core.content.edit

object UserSessionManager {

    private const val PREFS_NAME = "user_session"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_POINTS = "points"

    fun saveUser(context: Context, user: User) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_USER_ID, user.userId)
                .putString(KEY_USERNAME, user.username)
                .putString(KEY_EMAIL, user.email)
                .putInt(KEY_POINTS, user.points)
        }
    }

    fun getUserId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_ID, null)
    }

    fun getPoints(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_POINTS, 0)
    }

    fun getUsername(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USERNAME, null)
    }

    fun getEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_EMAIL, null)
    }

    fun updatePoints(context: Context, points: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putInt(KEY_POINTS, points) }
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
