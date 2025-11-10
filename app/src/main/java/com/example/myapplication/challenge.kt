package com.example.myapplication

import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapter.RewardAdapter
import com.example.myapplication.adapter.RewardListItem
import com.example.myapplication.adapter.VoucherAdapter
import com.example.myapplication.data.RewardRepository
import com.example.myapplication.data.UserSessionManager
import com.example.myapplication.model.Reward
import com.example.myapplication.model.UserVoucher
import java.text.NumberFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class Challenge : Fragment(R.layout.fragment_challenge) {

    private lateinit var pointsText: TextView
    private lateinit var rewardsEmptyState: TextView
    private lateinit var vouchersEmptyState: TextView
    private lateinit var rewardAdapter: RewardAdapter
    private lateinit var voucherAdapter: VoucherAdapter
    private var loadJob: Job? = null
    private var currentUserId: String? = null
    private var currentPoints: Int = 0
    private val numberFormatter = NumberFormat.getIntegerInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pointsText = view.findViewById(R.id.points)
        rewardsEmptyState = view.findViewById(R.id.rewards_empty_state)
        vouchersEmptyState = view.findViewById(R.id.vouchers_empty_state)

        val rewardsRecycler = view.findViewById<RecyclerView>(R.id.rewards_recycler)
        rewardAdapter = RewardAdapter(
            onRedeem = ::onRedeemReward,
            onUse = ::onUseReward
        )
        rewardsRecycler.layoutManager = LinearLayoutManager(requireContext())
        rewardsRecycler.adapter = rewardAdapter

        val vouchersRecycler = view.findViewById<RecyclerView>(R.id.vouchers_recycler)
        voucherAdapter = VoucherAdapter(::onUseVoucher)
        vouchersRecycler.layoutManager = LinearLayoutManager(requireContext())
        vouchersRecycler.adapter = voucherAdapter

        loadChallengeData()
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        super.onDestroyView()
    }

    private fun loadChallengeData() {
        val context = requireContext()
        val userId = UserSessionManager.getUserId(context)
        currentUserId = userId

        if (userId.isNullOrBlank()) {
            currentPoints = UserSessionManager.getPoints(context)
            updatePointsText()
            rewardAdapter.submitList(emptyList())
            voucherAdapter.submitList(emptyList())
            rewardsEmptyState.visibility = View.VISIBLE
            rewardsEmptyState.text = getString(R.string.challenge_login_required)
            vouchersEmptyState.visibility = View.VISIBLE
            vouchersEmptyState.text = getString(R.string.challenge_login_required)
            Toast.makeText(context, R.string.challenge_login_required, Toast.LENGTH_SHORT).show()
            return
        }

        loadJob?.cancel()
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val points = RewardRepository.getUserPoints(userId)
                currentPoints = points
                updatePointsText()

                val rewards = RewardRepository.getRewards()
                    .sortedBy { it.pointsRequired }
                val vouchers = RewardRepository.getUserVouchers(userId)
                    .sortedByDescending { it.obtainedAt }

                val rewardItems = buildRewardItems(rewards, vouchers, points)
                rewardAdapter.submitList(rewardItems)
                rewardsEmptyState.visibility = if (rewardItems.isEmpty()) View.VISIBLE else View.GONE
                rewardsEmptyState.text = if (rewardItems.isEmpty()) {
                    getString(R.string.challenge_rewards_empty)
                } else {
                    ""
                }

                voucherAdapter.submitList(vouchers)
                vouchersEmptyState.visibility = if (vouchers.isEmpty()) View.VISIBLE else View.GONE
                vouchersEmptyState.text = if (vouchers.isEmpty()) {
                    getString(R.string.challenge_vouchers_empty)
                } else {
                    ""
                }
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to load rewards", error)
                Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildRewardItems(
        rewards: List<Reward>,
        vouchers: List<UserVoucher>,
        points: Int
    ): List<RewardListItem> {
        val vouchersByReward = vouchers.groupBy { it.reward.id }
        return rewards.map { reward ->
            val rewardVouchers = vouchersByReward[reward.id].orEmpty()
            val redeemableVouchers = rewardVouchers.filter { RewardRepository.isVoucherRedeemed(it) }
            val isActive = reward.status.isNullOrBlank() || reward.status.equals("active", ignoreCase = true)
            RewardListItem(
                reward = reward,
                availableVouchers = redeemableVouchers.size,
                canRedeem = isActive && points >= reward.pointsRequired,
                canUse = redeemableVouchers.isNotEmpty(),
                nextVoucherId = redeemableVouchers.firstOrNull()?.id
            )
        }
    }

    private fun onRedeemReward(reward: Reward) {
        val userId = requireLoggedInUserId() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                RewardRepository.redeemReward(userId, reward)
                Toast.makeText(requireContext(), R.string.challenge_redeem_success, Toast.LENGTH_SHORT).show()
                loadChallengeData()
            } catch (error: RewardRepository.InsufficientPointsException) {
                Toast.makeText(requireContext(), R.string.challenge_insufficient_points, Toast.LENGTH_SHORT).show()
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to redeem reward", error)
                Toast.makeText(requireContext(), R.string.challenge_redeem_failure, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onUseReward(item: RewardListItem) {
        val voucherId = item.nextVoucherId
        if (voucherId.isNullOrBlank()) {
            Toast.makeText(requireContext(), R.string.challenge_use_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        if (requireLoggedInUserId() == null) {
            return
        }
        performVoucherUse(voucherId)
    }

    private fun onUseVoucher(voucher: UserVoucher) {
        if (!RewardRepository.isVoucherRedeemed(voucher)) {
            Toast.makeText(requireContext(), R.string.challenge_use_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        if (requireLoggedInUserId() == null) {
            return
        }
        performVoucherUse(voucher.id)
    }

    private fun performVoucherUse(voucherId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                RewardRepository.useVoucher(voucherId)
                Toast.makeText(requireContext(), R.string.challenge_use_success, Toast.LENGTH_SHORT).show()
                loadChallengeData()
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to update voucher", error)
                Toast.makeText(requireContext(), R.string.challenge_use_failure, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requireLoggedInUserId(): String? {
        val userId = currentUserId ?: UserSessionManager.getUserId(requireContext())
        return if (userId.isNullOrBlank()) {
            Toast.makeText(requireContext(), R.string.challenge_login_required, Toast.LENGTH_SHORT).show()
            null
        } else {
            userId
        }
    }

    private fun updatePointsText() {
        pointsText.text = numberFormatter.format(currentPoints)
    }

    companion object {
        private const val TAG = "ChallengeFragment"
    }
}
