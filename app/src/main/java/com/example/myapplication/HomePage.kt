package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class HomePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)

        val root = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.profile_btn).setOnClickListener {
            openDestination(MainContainer.Destination.PROFILE)
        }

        findViewById<MaterialButton>(R.id.search_btn).setOnClickListener {
            openDestination(MainContainer.Destination.SEARCH)
        }

        findViewById<MaterialButton>(R.id.nearby_cafe_btn).setOnClickListener {
            openDestination(MainContainer.Destination.NEARBY)
        }

        findViewById<MaterialButton>(R.id.challenges_btn).setOnClickListener {
            openDestination(MainContainer.Destination.CHALLENGES)
        }

        findViewById<MaterialButton>(R.id.wishlist_btn).setOnClickListener {
            openDestination(MainContainer.Destination.WISHLIST)
        }

        findViewById<MaterialButton>(R.id.check_in_btn).setOnClickListener {
            openDestination(MainContainer.Destination.CHECK_IN)
        }

        findViewById<MaterialButton>(R.id.community_btn).setOnClickListener {
            openDestination(MainContainer.Destination.COMMUNITY)
        }

    }

    private fun openDestination(destination: MainContainer.Destination) {
        startActivity(MainContainer.createIntent(this, destination))
    }
}