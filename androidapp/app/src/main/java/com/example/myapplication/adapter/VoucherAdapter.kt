package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.RewardRepository
import com.example.myapplication.model.UserVoucher
import com.google.android.material.button.MaterialButton

class VoucherAdapter(
    private val onUse: (UserVoucher) -> Unit
) : ListAdapter<UserVoucher, VoucherAdapter.VoucherViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_voucher_card, parent, false)
        return VoucherViewHolder(view, onUse)
    }

    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class VoucherViewHolder(
        itemView: View,
        private val onUse: (UserVoucher) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val rewardNameText: TextView = itemView.findViewById(R.id.voucher_reward_name)
        private val statusText: TextView = itemView.findViewById(R.id.voucher_status)
        private val obtainedText: TextView = itemView.findViewById(R.id.voucher_obtained)
        private val useButton: MaterialButton = itemView.findViewById(R.id.voucher_use_button)

        fun bind(voucher: UserVoucher) {
            val context = itemView.context
            rewardNameText.text = voucher.reward.name
            statusText.text = context.getString(
                R.string.challenge_voucher_status,
                voucher.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            )
            obtainedText.text = voucher.obtainedAt?.let {
                val displayDate = it.substringBefore('T', it)
                context.getString(R.string.challenge_voucher_obtained, displayDate)
            } ?: context.getString(R.string.challenge_voucher_obtained_unknown)

            val canUse = RewardRepository.isVoucherAvailable(voucher)
            useButton.isEnabled = canUse
            useButton.setOnClickListener { onUse(voucher) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UserVoucher>() {
            override fun areItemsTheSame(oldItem: UserVoucher, newItem: UserVoucher): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: UserVoucher, newItem: UserVoucher): Boolean {
                return oldItem == newItem
            }
        }
    }
}
