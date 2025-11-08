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
import com.example.myapplication.model.Cafe

class MainContainer : AppCompatActivity() {

    private var currentDestination: Destination? = null
    private var pendingCheckInCafe: Cafe? = null

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

        pendingCheckInCafe = intentReviewCafe(intent)

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
        pendingCheckInCafe = intentReviewCafe(intent)
        val destination = intentDestination(intent)
            ?: if (pendingCheckInCafe != null) Destination.CHECK_IN else null
        destination?.let { navigateTo(it) }
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

    fun navigateTo(destination: Destination, cafe: Cafe? = null) {
        val targetCafe = if (destination == Destination.CHECK_IN) {
            cafe ?: pendingCheckInCafe
        } else {
            null
        }

        if (destination == currentDestination && (destination != Destination.CHECK_IN || targetCafe == null)) {
            return
        }

        if (destination != Destination.CHECK_IN) {
            pendingCheckInCafe = null
        }

        val fragment = when (destination) {
            Destination.SEARCH -> SearchCafe()
            Destination.NEARBY -> CafeMaps()
            Destination.CHALLENGES -> Challenge()
            Destination.WISHLIST -> Wishlist()
            Destination.CHECK_IN -> {
                pendingCheckInCafe = null
                targetCafe?.let { CheckIn.newInstance(it) } ?: CheckIn()
            }
            Destination.COMMUNITY -> Community()
            Destination.PROFILE -> Profile()
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment, destination.name)
            .commit()

        currentDestination = destination
    }

    private fun intentReviewCafe(intent: Intent?): Cafe? {
        if (intent == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_REVIEW_CAFE, Cafe::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_REVIEW_CAFE) as? Cafe
        }
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
        private const val EXTRA_REVIEW_CAFE = "com.example.myapplication.extra.REVIEW_CAFE"
        private const val STATE_DESTINATION = "com.example.myapplication.state.DESTINATION"

        fun createIntent(context: Context, destination: Destination, cafe: Cafe? = null): Intent {
            return Intent(context, MainContainer::class.java).apply {
                putExtra(EXTRA_DESTINATION, destination)
                cafe?.let { putExtra(EXTRA_REVIEW_CAFE, it) }
            }
        }
    }
}
