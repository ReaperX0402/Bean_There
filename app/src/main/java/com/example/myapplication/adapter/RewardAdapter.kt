package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.myapplication.R
import com.example.myapplication.model.Reward
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat

data class RewardListItem(
    val reward: Reward,
    val availableVouchers: Int,
    val canRedeem: Boolean,
    val canUse: Boolean,
    val nextVoucherId: String?
)

class RewardAdapter(
    private val onRedeem: (Reward) -> Unit,
    private val onUse: (RewardListItem) -> Unit
) : ListAdapter<RewardListItem, RewardAdapter.RewardViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reward_card, parent, false)
        return RewardViewHolder(view, onRedeem, onUse)
    }

    override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RewardViewHolder(
        itemView: View,
        private val onRedeem: (Reward) -> Unit,
        private val onUse: (RewardListItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val nameText: TextView = itemView.findViewById(R.id.reward_name)
        private val descriptionText: TextView = itemView.findViewById(R.id.reward_description)
        private val pointsText: TextView = itemView.findViewById(R.id.reward_points_required)
        private val voucherInfoText: TextView = itemView.findViewById(R.id.reward_voucher_info)
        private val redeemButton: MaterialButton = itemView.findViewById(R.id.reward_redeem_button)
        private val useButton: MaterialButton = itemView.findViewById(R.id.reward_use_button)
        private val numberFormatter = NumberFormat.getIntegerInstance()

        fun bind(item: RewardListItem) {
            val reward = item.reward
            val context = itemView.context

            nameText.text = reward.name
            descriptionText.text = reward.description?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.challenge_reward_description_placeholder)

            val formattedPoints = numberFormatter.format(reward.pointsRequired)
            pointsText.text = context.getString(
                R.string.challenge_reward_points_required,
                formattedPoints
            )

            val voucherInfo = context.getString(
                R.string.challenge_reward_voucher_count,
                item.availableVouchers
            )
            voucherInfoText.text = voucherInfo

            redeemButton.isEnabled = item.canRedeem
            redeemButton.setOnClickListener { onRedeem(reward) }

            useButton.isEnabled = item.canUse && !item.nextVoucherId.isNullOrBlank()
            useButton.setOnClickListener { onUse(item) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RewardListItem>() {
            override fun areItemsTheSame(oldItem: RewardListItem, newItem: RewardListItem): Boolean {
                return oldItem.reward.id == newItem.reward.id
            }

            override fun areContentsTheSame(oldItem: RewardListItem, newItem: RewardListItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
