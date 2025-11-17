package com.example.myapplication.order

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.CartItemAdapter
import com.example.myapplication.adapter.MenuSectionAdapter
import com.example.myapplication.data.CartManager
import com.example.myapplication.data.CartManager.CartConflictException
import com.example.myapplication.data.OrderRepository
import com.example.myapplication.data.UserSessionManager
import com.example.myapplication.model.CartState
import com.example.myapplication.model.MenuItem
import com.example.myapplication.model.MenuSection
import com.example.myapplication.model.OrderRequest
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class OrderFragment : Fragment() {

    private lateinit var menuRecycler: RecyclerView
    private lateinit var loadingView: ProgressBar
    private lateinit var emptyState: TextView
    private lateinit var cartCard: MaterialCardView
    private lateinit var cartTitle: TextView
    private lateinit var cartTotal: TextView
    private lateinit var cartButton: MaterialButton

    private val menuSectionAdapter = MenuSectionAdapter { section, item ->
        addItemToCart(section, item)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_order, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        menuRecycler = view.findViewById(R.id.order_menu_list)
        loadingView = view.findViewById(R.id.order_loading)
        emptyState = view.findViewById(R.id.order_empty_state)
        cartCard = view.findViewById(R.id.cart_summary_card)
        cartTitle = view.findViewById(R.id.cart_summary_title)
        cartTotal = view.findViewById(R.id.cart_summary_total)
        cartButton = view.findViewById(R.id.cart_summary_button)

        menuRecycler.layoutManager = LinearLayoutManager(requireContext())
        menuRecycler.adapter = menuSectionAdapter
        menuRecycler.itemAnimator = null

        cartButton.setOnClickListener { showCartBottomSheet() }

        loadMenus()
        updateCartSummary()
    }

    private fun loadMenus() {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                val sections = OrderRepository.getMenuSections()
                menuSectionAdapter.submitList(sections)
                emptyState.isVisible = sections.isEmpty()
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to load menus", error)
                emptyState.isVisible = true
                emptyState.text = getString(R.string.order_load_error)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        loadingView.isVisible = loading
        menuRecycler.isVisible = !loading
    }

    private fun addItemToCart(section: MenuSection, item: MenuItem) {
        val context = requireContext()
        val result = CartManager.addItem(context, section.cafeId, section.cafeName, item)
        result.onSuccess {
            updateCartSummary()
            Toast.makeText(
                context,
                getString(R.string.order_add_success, item.itemName),
                Toast.LENGTH_SHORT
            ).show()
        }.onFailure { error ->
            if (error is CartConflictException) {
                val cafeName = error.existingCafeName
                    ?: getString(R.string.order_unknown_cafe)
                Toast.makeText(
                    context,
                    getString(R.string.order_cart_other_cafe, cafeName),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(context, R.string.order_add_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCartSummary() {
        val state = CartManager.getCartState(requireContext())
        cartCard.isVisible = state.items.isNotEmpty()
        if (state.items.isEmpty()) {
            return
        }
        val cafeName = state.cafeName ?: getString(R.string.order_unknown_cafe)
        cartTitle.text = getString(
            R.string.order_cart_summary_title,
            cafeName,
            state.itemCount
        )
        cartTotal.text = getString(R.string.order_total_format, state.total)
    }

    private fun showCartBottomSheet() {
        val context = requireContext()
        val cartState = CartManager.getCartState(context)
        if (cartState.items.isEmpty()) {
            Toast.makeText(context, R.string.order_cart_empty_message, Toast.LENGTH_SHORT).show()
            updateCartSummary()
            return
        }

        val dialog = BottomSheetDialog(context)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_cart, null)
        dialog.setContentView(sheetView)

        val cafeName: TextView = sheetView.findViewById(R.id.cart_cafe_name)
        val recycler: RecyclerView = sheetView.findViewById(R.id.cart_items_recycler)
        val totalView: TextView = sheetView.findViewById(R.id.cart_total_value)
        val emptyView: TextView = sheetView.findViewById(R.id.cart_empty_text)
        val notesInput: TextInputEditText = sheetView.findViewById(R.id.cart_input_notes)
        val paxInput: TextInputEditText = sheetView.findViewById(R.id.cart_input_pax)
        val checkoutButton: MaterialButton = sheetView.findViewById(R.id.checkout_button)
        val clearButton: MaterialButton = sheetView.findViewById(R.id.clear_cart_button)

        val cartAdapter = CartItemAdapter(
            onQuantityChanged = { cartItem, newQuantity ->
                val updated = CartManager.updateQuantity(context, cartItem.itemId, newQuantity)
                cartAdapter.submitList(updated.items)
                bindCartState(updated, cafeName, totalView, emptyView, recycler, checkoutButton)
                updateCartSummary()
                if (updated.items.isEmpty()) {
                    dialog.dismiss()
                }
            },
            onRemoveItem = { cartItem ->
                val updated = CartManager.removeItem(context, cartItem.itemId)
                cartAdapter.submitList(updated.items)
                bindCartState(updated, cafeName, totalView, emptyView, recycler, checkoutButton)
                updateCartSummary()
                if (updated.items.isEmpty()) {
                    dialog.dismiss()
                }
            }
        )

        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = cartAdapter
        cartAdapter.submitList(cartState.items)
        bindCartState(cartState, cafeName, totalView, emptyView, recycler, checkoutButton)

        clearButton.setOnClickListener {
            CartManager.clearCart(context)
            cartAdapter.submitList(emptyList())
            bindCartState(CartState(), cafeName, totalView, emptyView, recycler, checkoutButton)
            updateCartSummary()
            dialog.dismiss()
        }

        checkoutButton.setOnClickListener {
            val userId = UserSessionManager.getUserId(context)
            if (userId.isNullOrBlank()) {
                Toast.makeText(context, R.string.order_checkout_requires_login, Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }
            val pax = paxInput.text?.toString()?.toIntOrNull()?.takeIf { it > 0 }
            val notes = notesInput.text?.toString()
            submitOrder(dialog, cartAdapter, checkoutButton, notes, pax)
        }

        dialog.show()
    }

    private fun bindCartState(
        state: CartState,
        cafeName: TextView,
        totalView: TextView,
        emptyView: TextView,
        recycler: RecyclerView,
        checkoutButton: MaterialButton
    ) {
        val hasItems = state.items.isNotEmpty()
        cafeName.text = state.cafeName ?: getString(R.string.order_unknown_cafe)
        totalView.text = getString(R.string.order_total_format, state.total)
        emptyView.isVisible = !hasItems
        recycler.isVisible = hasItems
        checkoutButton.isEnabled = hasItems
    }

    private fun submitOrder(
        dialog: BottomSheetDialog,
        cartAdapter: CartItemAdapter,
        checkoutButton: MaterialButton,
        notes: String?,
        pax: Int?
    ) {
        val context = requireContext()
        val cartState = CartManager.getCartState(context)
        if (cartState.items.isEmpty()) {
            Toast.makeText(context, R.string.order_cart_empty_message, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            updateCartSummary()
            return
        }
        val userId = UserSessionManager.getUserId(context) ?: return
        val cafeId = cartState.cafeId
        if (cafeId.isNullOrBlank()) {
            Toast.makeText(context, R.string.order_add_error, Toast.LENGTH_SHORT).show()
            checkoutButton.isEnabled = true
            return
        }
        checkoutButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = OrderRequest(
                    cafeId = cafeId,
                    userId = userId,
                    items = cartState.items,
                    total = cartState.total,
                    notes = notes?.takeIf { it.isNotBlank() },
                    pax = pax
                )
                OrderRepository.placeOrder(request)
                CartManager.clearCart(context)
                cartAdapter.submitList(emptyList())
                updateCartSummary()
                Toast.makeText(context, R.string.order_checkout_success, Toast.LENGTH_LONG).show()
                dialog.dismiss()
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to place order", error)
                Toast.makeText(context, R.string.order_checkout_failure, Toast.LENGTH_LONG).show()
                checkoutButton.isEnabled = true
            }
        }
    }

    companion object {
        private const val TAG = "OrderFragment"
    }
}
