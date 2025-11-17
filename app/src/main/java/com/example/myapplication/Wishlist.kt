package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapter.WishlistAdapter
import com.example.myapplication.data.UserSessionManager
import com.example.myapplication.data.WishlistRepository
import com.example.myapplication.model.WishlistItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class Wishlist : Fragment() {

    private lateinit var wishlistAdapter: WishlistAdapter
    private var loadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wishlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler(view)
        loadWishlist()
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        super.onDestroyView()
    }

    private fun setupRecycler(root: View) {
        val recyclerView = root.findViewById<RecyclerView>(R.id.wishlist_recycle)
        wishlistAdapter = WishlistAdapter(
            onOpenLocation = ::openLocation,
            onRemove = ::removeFromWishlist,
            onCheckIn = ::openCheckIn
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = wishlistAdapter
    }

    private fun loadWishlist() {
        val ctx = context ?: return
        val userId = UserSessionManager.getUserId(requireContext())
        if (userId.isNullOrBlank()) {
            wishlistAdapter.submitList(emptyList())
            Toast.makeText(ctx, R.string.wishlist_requires_login, Toast.LENGTH_SHORT)
                .show()
            return
        }

        loadJob?.cancel()
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val wishlist = WishlistRepository.getWishlist(userId)
                wishlistAdapter.submitList(wishlist)
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to load wishlist", error)
                Toast.makeText(ctx, R.string.error_generic, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openLocation(item: WishlistItem) {
        val cafe = item.cafe
        val uri = when {
            cafe.lat != null && cafe.long != null -> {
                "geo:${cafe.lat},${cafe.long}?q=${Uri.encode(cafe.name)}".toUri()
            }
            !cafe.address.isNullOrBlank() -> {
                "geo:0,0?q=${Uri.encode(cafe.address)}".toUri()
            }
            else -> null
        }

        if (uri == null) {
            Toast.makeText(requireContext(), R.string.map_unavailable, Toast.LENGTH_SHORT).show()
            return
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        val context = requireContext()
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(context, R.string.map_app_missing, Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeFromWishlist(item: WishlistItem) {
        val userId = UserSessionManager.getUserId(requireContext())
        if (userId.isNullOrBlank()) {
            Toast.makeText(requireContext(), R.string.wishlist_requires_login, Toast.LENGTH_SHORT)
                .show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                WishlistRepository.removeFromWishlist(userId, item.id)
                Toast.makeText(requireContext(), R.string.wishlist_remove_success, Toast.LENGTH_SHORT)
                    .show()
                loadWishlist()
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to remove wishlist item", error)
                Toast.makeText(requireContext(), R.string.wishlist_remove_failure, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun openCheckIn(item: WishlistItem) {
        val cafe = item.cafe
        val activity = activity
        if (activity is MainContainer) {
            activity.navigateTo(MainContainer.Destination.CHECK_IN, cafe)
        } else {
            val context = requireContext()
            val intent = MainContainer.createIntent(
                context,
                MainContainer.Destination.CHECK_IN,
                cafe
            )
            startActivity(intent)
        }
    }

    fun refresh() {
        loadWishlist()
    }

    companion object {
        private const val TAG = "WishlistFragment"
    }
}
