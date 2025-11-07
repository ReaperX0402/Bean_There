package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.CafeDetail
import com.example.myapplication.adapter.CafeAdapter
import com.example.myapplication.adapter.CategoryAdapter
import com.example.myapplication.data.CafeRepository
import com.example.myapplication.model.Cafe
import kotlinx.coroutines.launch

class SearchCafe : Fragment() {

    private val categoryAdapter = CategoryAdapter()
    private val cafeAdapter = CafeAdapter { cafe ->
        openCafeDetail(cafe)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // No-op for now; placeholder in case arguments are needed later
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_cafe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategoryRecycler(view)
        setupCafeRecycler(view)
    }

    private fun setupCategoryRecycler(root: View) {
        val categoriesRecycler = root.findViewById<RecyclerView>(R.id.categories_recycle)
        categoriesRecycler.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        categoriesRecycler.adapter = categoryAdapter
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val categories = CafeRepository.getAllTags()
                categoryAdapter.submitList(categories)
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to load categories", error)
            }
        }
    }

    private fun setupCafeRecycler(root: View) {
        val cafeRecycler = root.findViewById<RecyclerView>(R.id.cafe_recycle)
        cafeRecycler.layoutManager = LinearLayoutManager(requireContext())
        cafeRecycler.adapter = cafeAdapter
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val cafes = CafeRepository.getAllCafes()
                cafeAdapter.submitList(cafes)
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to load cafes", error)
            }
        }
    }

    private fun openCafeDetail(cafe: Cafe) {
        parentFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, CafeDetail.newInstance(cafe))
            .addToBackStack(null)
            .commit()
    }

    companion object {
        private const val TAG = "SearchCafe"
    }
}
