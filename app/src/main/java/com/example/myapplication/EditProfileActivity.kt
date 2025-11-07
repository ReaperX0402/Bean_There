package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.UserRepository
import com.example.myapplication.data.UserSessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var usernameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var newPasswordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var backButton: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)

        usernameInput = findViewById(R.id.edit_profile_username_input)
        emailInput = findViewById(R.id.edit_profile_email_input)
        newPasswordInput = findViewById(R.id.edit_profile_password_input)
        confirmPasswordInput = findViewById(R.id.edit_profile_confirm_password_input)
        saveButton = findViewById(R.id.save_profile_button)
        backButton = findViewById(R.id.back_to_profile_button)
        progressBar = findViewById(R.id.edit_profile_loading_indicator)
        errorText = findViewById(R.id.edit_profile_error_text)

        listOf(usernameInput, emailInput, newPasswordInput, confirmPasswordInput).forEach { input ->
            input.addTextChangedListener { errorText.visibility = View.GONE }
        }

        saveButton.setOnClickListener { attemptSave() }
        backButton.setOnClickListener { finish() }

        lifecycleScope.launch {
            populateFields()
        }
    }

    private suspend fun populateFields() {
        val userId = UserSessionManager.getUserId(this) ?: run {
            showError(getString(R.string.profile_requires_login))
            saveButton.isEnabled = false
            return
        }

        showLoading(true)
        runCatching {
            UserRepository.getUserById(userId)
        }.onSuccess { user ->
            showLoading(false)
            if (user == null) {
                showError(getString(R.string.profile_not_found))
                saveButton.isEnabled = false
                return@onSuccess
            }
            usernameInput.setText(user.username)
            emailInput.setText(user.email.orEmpty())
        }.onFailure { error ->
            showLoading(false)
            showError(error.message ?: getString(R.string.error_generic))
            saveButton.isEnabled = false
        }
    }

    private fun attemptSave() {
        val userId = UserSessionManager.getUserId(this)
        if (userId.isNullOrBlank()) {
            showError(getString(R.string.profile_requires_login))
            return
        }

        val username = usernameInput.text?.toString()?.trim().orEmpty()
        val email = emailInput.text?.toString()?.trim().orEmpty()
        val newPassword = newPasswordInput.text?.toString()?.trim().orEmpty()
        val confirmPassword = confirmPasswordInput.text?.toString()?.trim().orEmpty()

        if (username.isBlank() || email.isBlank()) {
            showError(getString(R.string.error_missing_credentials))
            return
        }

        if (newPassword.isNotEmpty() && newPassword != confirmPassword) {
            showError(getString(R.string.error_password_mismatch))
            return
        }

        showLoading(true)
        lifecycleScope.launch {
            runCatching {
                UserRepository.updateUser(
                    userId = userId,
                    username = username,
                    email = email,
                    newPassword = newPassword.ifBlank { null }
                )
            }.onSuccess { updatedUser ->
                showLoading(false)
                UserSessionManager.saveUser(this@EditProfileActivity, updatedUser)
                Toast.makeText(
                    this@EditProfileActivity,
                    getString(R.string.edit_profile_success),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }.onFailure { error ->
                showLoading(false)
                showError(error.message ?: getString(R.string.error_generic))
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        saveButton.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }
}
