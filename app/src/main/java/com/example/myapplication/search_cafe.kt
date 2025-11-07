package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapter.CafeAdapter
import com.example.myapplication.adapter.CategoryAdapter
import com.example.myapplication.model.Cafe
import com.example.myapplication.model.Tag

class SearchCafe : Fragment() {

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
        val categoryAdapter = CategoryAdapter()
        categoriesRecycler.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        categoriesRecycler.adapter = categoryAdapter

        val categories = listOf(
            Tag(tag_id = "1", tag_name = getString(R.string.aesthetic)),
            Tag(tag_id = "2", tag_name = getString(R.string.instagrammable)),
            Tag(tag_id = "3", tag_name = getString(R.string.chill)),
            Tag(tag_id = "4", tag_name = getString(R.string.work_friendly))
        )
        categoryAdapter.submitList(categories)
    }

    private fun setupCafeRecycler(root: View) {
        val cafeRecycler = root.findViewById<RecyclerView>(R.id.cafe_recycle)
        val cafeAdapter = CafeAdapter()
        cafeRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
        cafeRecycler.adapter = cafeAdapter

        val cafes = listOf(
            Cafe(
                cafe_id = "cafe_1",
                name = "Wheeler's Yard",
                address = "28 Lor Ampas, Singapore",
                rating_avg = 4.5,
                tags = listOf(
                    Tag(tag_id = "1", tag_name = getString(R.string.aesthetic)),
                    Tag(tag_id = "3", tag_name = getString(R.string.chill))
                )
            ),
            Cafe(
                cafe_id = "cafe_2",
                name = "Atlas Coffeehouse",
                address = "6 Duke's Rd, Singapore",
                rating_avg = 4.7,
                tags = listOf(
                    Tag(tag_id = "2", tag_name = getString(R.string.instagrammable)),
                    Tag(tag_id = "4", tag_name = getString(R.string.work_friendly))
                )
            )
        )
        cafeAdapter.submitList(cafes)
    }
}
