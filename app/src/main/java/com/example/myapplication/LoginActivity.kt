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

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var signUpButton: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        usernameInput = findViewById(R.id.username_input)
        passwordInput = findViewById(R.id.password_input)
        loginButton = findViewById(R.id.login_button)
        signUpButton = findViewById(R.id.sign_up_button)
        progressBar = findViewById(R.id.loading_indicator)
        errorText = findViewById(R.id.error_text)

        usernameInput.addTextChangedListener { errorText.visibility = View.GONE }
        passwordInput.addTextChangedListener { errorText.visibility = View.GONE }

        loginButton.setOnClickListener { attemptLogin() }
        signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val username = usernameInput.text?.toString()?.trim().orEmpty()
        val password = passwordInput.text?.toString()?.trim().orEmpty()

        if (username.isBlank() || password.isBlank()) {
            showError(getString(R.string.error_missing_credentials))
            return
        }

        showLoading(true)
        lifecycleScope.launch {
            runCatching {
                UserRepository.login(username, password)
            }.onSuccess { user ->
                showLoading(false)
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.login_success, user.username),
                    Toast.LENGTH_SHORT
                ).show()
                val intent = Intent(this@LoginActivity, HomePage::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }.onFailure { error ->
                showLoading(false)
                showError(error.message ?: getString(R.string.error_generic))
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        loginButton.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }
}
