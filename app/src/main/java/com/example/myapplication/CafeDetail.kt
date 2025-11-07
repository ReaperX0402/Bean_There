package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.myapplication.adapter.CategoryAdapter
import com.example.myapplication.data.UserSessionManager
import com.example.myapplication.data.WishlistRepository
import com.example.myapplication.model.Cafe
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CafeDetail : Fragment() {

    private var cafe: Cafe? = null
    private lateinit var categoryAdapter: CategoryAdapter
    private var wishlistJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cafe = arguments?.getSerializable(ARG_CAFE) as? Cafe
        if (cafe == null) {
            Log.e(TAG, "Cafe argument missing")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cafe_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        categoryAdapter = CategoryAdapter(R.layout.item_category_button_small)
        val categoriesRecycler = view.findViewById<RecyclerView>(R.id.categories_recycle)
        categoriesRecycler.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        categoriesRecycler.adapter = categoryAdapter

        cafe?.let { bindCafe(view, it) }
        setupWishlistButton(view)
    }

    override fun onDestroyView() {
        wishlistJob?.cancel()
        super.onDestroyView()
    }

    private fun bindCafe(root: View, cafe: Cafe) {
        val imageView: ImageView = root.findViewById(R.id.cafe_image)
        imageView.contentDescription = cafe.name
        imageView.load(cafe.img_url) {
            crossfade(true)
            placeholder(R.drawable.contact2)
            error(R.drawable.contact2)
        }

        val nameView: TextView = root.findViewById(R.id.cafe_name)
        nameView.text = cafe.name

        val ratingView: TextView = root.findViewById(R.id.rating)
        ratingView.text = cafe.rating_avg?.let { rating ->
            getString(R.string.rating_format, rating)
        } ?: getString(R.string.rating_unavailable)

        val operatingHoursView: TextView = root.findViewById(R.id.operating_hour)
        operatingHoursView.text = cafe.operatingHours?.takeIf { it.isNotBlank() }?.let {
            getString(R.string.operating_hours_format, it)
        } ?: getString(R.string.operating_hours_unavailable)

        val locationView: TextView = root.findViewById(R.id.location)
        val address = cafe.address?.takeIf { it.isNotBlank() }
        locationView.text = address?.let {
            getString(R.string.location_format, it)
        } ?: getString(R.string.location_unavailable)

        val phoneView: TextView = root.findViewById(R.id.phone_no)
        val phone = cafe.phone_no?.takeIf { it.isNotBlank() }
        phoneView.text = phone?.let {
            getString(R.string.phone_format, it)
        } ?: getString(R.string.phone_unavailable)

        val viewOnMap: TextView = root.findViewById(R.id.view_on_map_txt)
        viewOnMap.setOnClickListener { openMap(cafe) }

        categoryAdapter.submitList(cafe.tags)
    }

    private fun setupWishlistButton(root: View) {
        val button = root.findViewById<Button>(R.id.add_to_wishlist)
        button.setOnClickListener {
            val cafe = cafe
            if (cafe == null) {
                Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = UserSessionManager.getUserId(requireContext())
            if (userId.isNullOrBlank()) {
                Toast.makeText(requireContext(), R.string.wishlist_requires_login, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            button.isEnabled = false
            wishlistJob?.cancel()
            wishlistJob = viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val result = WishlistRepository.addToWishlist(
                        userId = userId,
                        cafeId = cafe.cafe_id,
                        fallbackCafe = cafe
                    )
                    val messageRes = if (result.isNew) {
                        R.string.wishlist_add_success
                    } else {
                        R.string.wishlist_already_exists
                    }
                    Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
                } catch (error: Throwable) {
                    Log.e(TAG, "Failed to add cafe to wishlist", error)
                    Toast.makeText(
                        requireContext(),
                        R.string.wishlist_add_failure,
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    button.isEnabled = true
                }
            }
        }
    }

    private fun openMap(cafe: Cafe) {
        val context = requireContext()
        val uri = when {
            cafe.lat != null && cafe.long != null -> {
                Uri.parse("geo:${cafe.lat},${cafe.long}?q=${Uri.encode(cafe.name)}")
            }
            !cafe.address.isNullOrBlank() -> {
                Uri.parse("geo:0,0?q=${Uri.encode(cafe.address)}")
            }
            else -> null
        }

        if (uri == null) {
            Toast.makeText(context, R.string.map_unavailable, Toast.LENGTH_SHORT).show()
            return
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(context, R.string.map_app_missing, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val ARG_CAFE = "arg_cafe"
        private const val TAG = "CafeDetail"

        fun newInstance(cafe: Cafe): CafeDetail {
            return CafeDetail().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CAFE, cafe)
                }
            }
        }
    }
}
