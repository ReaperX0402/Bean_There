package com.example.myapplication

import android.content.Intent
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var usernameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var signUpButton: MaterialButton
    private lateinit var backToLoginButton: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        usernameInput = findViewById(R.id.sign_up_username_input)
        emailInput = findViewById(R.id.sign_up_email_input)
        passwordInput = findViewById(R.id.sign_up_password_input)
        confirmPasswordInput = findViewById(R.id.sign_up_confirm_password_input)
        signUpButton = findViewById(R.id.create_account_button)
        backToLoginButton = findViewById(R.id.back_to_login_button)
        progressBar = findViewById(R.id.sign_up_loading_indicator)
        errorText = findViewById(R.id.sign_up_error_text)

        listOf(usernameInput, emailInput, passwordInput, confirmPasswordInput).forEach { input ->
            input.addTextChangedListener { errorText.visibility = View.GONE }
        }

        signUpButton.setOnClickListener { attemptSignUp() }
        backToLoginButton.setOnClickListener { finish() }
    }

    private fun attemptSignUp() {
        val username = usernameInput.text?.toString()?.trim().orEmpty()
        val email = emailInput.text?.toString()?.trim().orEmpty()
        val password = passwordInput.text?.toString()?.trim().orEmpty()
        val confirmPassword = confirmPasswordInput.text?.toString()?.trim().orEmpty()

        if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            showError(getString(R.string.error_missing_credentials))
            return
        }

        if (password != confirmPassword) {
            showError(getString(R.string.error_password_mismatch))
            return
        }

        showLoading(true)
        lifecycleScope.launch {
            runCatching {
                UserRepository.signUp(username, email, password)
            }.onSuccess { user ->
                showLoading(false)
                Toast.makeText(
                    this@SignUpActivity,
                    getString(R.string.sign_up_success, user.username),
                    Toast.LENGTH_SHORT
                ).show()
                val intent = Intent(this@SignUpActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finish()
            }.onFailure { error ->
                showLoading(false)
                showError(error.message ?: getString(R.string.error_generic))
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        signUpButton.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }
}
