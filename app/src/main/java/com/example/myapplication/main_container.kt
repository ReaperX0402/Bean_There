package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainContainer : AppCompatActivity() {

    private var currentDestination: Destination? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_container)

        val root = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.back_btn).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.profile_btn).setOnClickListener {
            navigateTo(Destination.PROFILE)
        }

        if (savedInstanceState == null) {
            val initialDestination = intentDestination(intent) ?: Destination.SEARCH
            navigateTo(initialDestination)
        } else {
            currentDestination = savedInstanceState.getString(STATE_DESTINATION)?.let {
                runCatching { Destination.valueOf(it) }.getOrNull()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentDestination(intent)?.let { navigateTo(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentDestination?.let { outState.putString(STATE_DESTINATION, it.name) }
    }

    private fun intentDestination(intent: Intent?): Destination? {
        if (intent == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_DESTINATION, Destination::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_DESTINATION) as? Destination
        }
    }

    private fun navigateTo(destination: Destination) {
        if (destination == currentDestination) return

        val fragment = when (destination) {
            Destination.SEARCH -> SearchCafe()
            Destination.NEARBY -> CafeMaps()
            Destination.CHALLENGES -> Challenge()
            Destination.WISHLIST -> Wishlist()
            Destination.CHECK_IN -> CheckIn()
            Destination.COMMUNITY -> Community()
            Destination.PROFILE -> Profile()
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment, destination.name)
            .commit()

        currentDestination = destination
    }

    enum class Destination {
        SEARCH,
        NEARBY,
        CHALLENGES,
        WISHLIST,
        CHECK_IN,
        COMMUNITY,
        PROFILE
    }

    companion object {
        private const val EXTRA_DESTINATION = "com.example.myapplication.extra.DESTINATION"
        private const val STATE_DESTINATION = "com.example.myapplication.state.DESTINATION"

        fun createIntent(context: Context, destination: Destination): Intent {
            return Intent(context, MainContainer::class.java).apply {
                putExtra(EXTRA_DESTINATION, destination)
            }
        }
    }
}
