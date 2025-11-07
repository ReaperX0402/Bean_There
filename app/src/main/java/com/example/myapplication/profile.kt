package com.example.myapplication
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.UserRepository
import com.example.myapplication.data.UserSessionManager
import com.example.myapplication.model.User
import kotlinx.coroutines.launch

class Profile : Fragment(R.layout.fragment_profile) {

    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var joinedText: TextView
    private lateinit var errorText: TextView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var editButton: Button
    private lateinit var profileHeader: View
    private lateinit var headerDivider: View
    private lateinit var postsLabel: View
    private lateinit var postsRecyclerView: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nameText = view.findViewById(R.id.profile_name)
        emailText = view.findViewById(R.id.profile_email)
        joinedText = view.findViewById(R.id.profile_joined)
        errorText = view.findViewById(R.id.profile_error_text)
        loadingIndicator = view.findViewById(R.id.profile_loading_indicator)
        editButton = view.findViewById(R.id.edit_profile_btn)
        profileHeader = view.findViewById(R.id.profile_header)
        headerDivider = view.findViewById(R.id.header_divider)
        postsLabel = view.findViewById(R.id.posts_label)
        postsRecyclerView = view.findViewById(R.id.posts_recycler_view)

        editButton.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            loadProfile()
        }
    }

    private suspend fun loadProfile() {
        val context = requireContext()
        val userId = UserSessionManager.getUserId(context)
        val cachedUsername = UserSessionManager.getUsername(context)
        val cachedEmail = UserSessionManager.getEmail(context)

        if (!cachedUsername.isNullOrBlank() || !cachedEmail.isNullOrBlank()) {
            nameText.text = cachedUsername ?: getString(R.string.profile_username_placeholder)
            emailText.text = cachedEmail ?: getString(R.string.profile_email_placeholder)
            joinedText.text = getString(R.string.profile_member_since_placeholder)
            setContentVisible(true)
            editButton.isEnabled = true
            errorText.isGone = true
        }

        if (userId.isNullOrBlank()) {
            showLoggedOutState()
            return
        }

        showLoading(true)
        errorText.isGone = true

        runCatching {
            UserRepository.getUserById(userId)
        }.onSuccess { user ->
            showLoading(false)
            if (user == null) {
                showError(getString(R.string.profile_not_found))
                return@onSuccess
            }
            bindUser(user)
            UserSessionManager.saveUser(context, user)
        }.onFailure { error ->
            showLoading(false)
            val message = error.message ?: getString(R.string.error_generic)
            showError(message)
        }
    }

    private fun bindUser(user: User) {
        nameText.text = user.username.ifBlank { getString(R.string.profile_username_placeholder) }
        emailText.text = user.email ?: getString(R.string.profile_email_placeholder)
        joinedText.text = user.createdAt?.let {
            getString(R.string.profile_member_since, it)
        } ?: getString(R.string.profile_member_since_placeholder)
        setContentVisible(true)
        errorText.isGone = true
        editButton.isEnabled = true
    }

    private fun showLoggedOutState() {
        setContentVisible(false)
        loadingIndicator.isVisible = false
        errorText.isVisible = true
        errorText.text = getString(R.string.profile_requires_login)
        editButton.isEnabled = false
    }

    private fun showError(message: String) {
        val hadContent = profileHeader.isVisible
        if (hadContent) {
            errorText.isGone = true
            context?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
        } else {
            setContentVisible(false)
            editButton.isEnabled = false
            errorText.text = message
            errorText.isVisible = true
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loadingIndicator.isVisible = isLoading
        if (isLoading) {
            setContentVisible(false)
        }
    }

    private fun setContentVisible(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        profileHeader.visibility = visibility
        headerDivider.visibility = visibility
        postsLabel.visibility = visibility
        postsRecyclerView.visibility = visibility
    }
}
