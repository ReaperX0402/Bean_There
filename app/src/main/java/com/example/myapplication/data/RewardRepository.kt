package com.example.myapplication.data

import com.example.myapplication.model.Reward
import com.example.myapplication.model.UserVoucher
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.eq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

object RewardRepository {

    class InsufficientPointsException : Exception("Insufficient points to redeem this reward.")

    data class RedemptionResult(
        val voucher: UserVoucher,
        val remainingPoints: Int
    )

    private val client get() = SupabaseProvider.client

    private const val STATUS_REDEEMED = "redeemed"
    private const val STATUS_USED = "used"

    @Serializable
    private data class RewardResponse(
        val reward_id: String,
        val reward_name: String,
        val description: String? = null,
        val points_required: Int,
        val image_url: String? = null,
        val status: String? = null
    ) {
        fun toReward(): Reward = Reward(
            id = reward_id,
            name = reward_name,
            description = description,
            pointsRequired = points_required,
            imageUrl = image_url,
            status = status
        )
    }

    @Serializable
    private data class UserRewardResponse(
        val user_reward_id: String,
        val reward_id: String,
        val status: String,
        val obtained_at: String? = null,
        val reward: RewardResponse? = null
    ) {
        fun toUserVoucher(fallbackReward: Reward? = null): UserVoucher? {
            val rewardModel = reward?.toReward() ?: fallbackReward ?: return null
            return UserVoucher(
                id = user_reward_id,
                reward = rewardModel,
                status = status,
                obtainedAt = obtained_at
            )
        }
    }

    @Serializable
    private data class UserPointsResponse(
        val points: Int = 0
    )

    suspend fun getRewards(): List<Reward> = withContext(Dispatchers.IO) {
        client.from("reward")
            .select(columns = Columns.ALL)
            .decodeList<RewardResponse>()
            .map { it.toReward() }
    }

    suspend fun getUserPoints(userId: String): Int = withContext(Dispatchers.IO) {
        client.from("user")
            .select(columns = Columns.list("points")) {
                filter { eq("user_id", userId) }
                limit(1)
            }
            .decodeList<UserPointsResponse>()
            .firstOrNull()?.points ?: 0
    }

    suspend fun getUserVouchers(userId: String): List<UserVoucher> = withContext(Dispatchers.IO) {
        client.from("user_reward")
            .select(columns = USER_REWARD_COLUMNS) {
                filter { eq("user_id", userId) }
            }
            .decodeList<UserRewardResponse>()
            .mapNotNull { it.toUserVoucher() }
    }

    suspend fun redeemReward(userId: String, reward: Reward): RedemptionResult = withContext(Dispatchers.IO) {
        val currentPoints = getUserPoints(userId)
        if (currentPoints < reward.pointsRequired) {
            throw InsufficientPointsException()
        }

        val updatedPoints = client.from("user")
            .update({
                set("points", currentPoints - reward.pointsRequired)
            }) {
                filter { eq("user_id", userId) }
                select(columns = Columns.list("points"))
                limit(1)
            }
            .decodeList<UserPointsResponse>()
            .firstOrNull()?.points ?: (currentPoints - reward.pointsRequired)

        val voucher = client.from("user_reward")
            .insert(
                mapOf(
                    "user_id" to userId,
                    "reward_id" to reward.id,
                    "status" to STATUS_REDEEMED
                )
            ) {
                select(columns = USER_REWARD_COLUMNS)
            }
            .decodeList<UserRewardResponse>()
            .firstOrNull()?.toUserVoucher(fallbackReward = reward)
            ?: throw IllegalStateException("Failed to create user voucher")

        RedemptionResult(voucher, updatedPoints)
    }

    suspend fun useVoucher(userRewardId: String): UserVoucher = withContext(Dispatchers.IO) {
        client.from("user_reward")
            .update({
                set("status", STATUS_USED)
            }) {
                filter { eq("user_reward_id", userRewardId) }
                select(columns = USER_REWARD_COLUMNS)
                limit(1)
            }
            .decodeList<UserRewardResponse>()
            .firstOrNull()?.toUserVoucher()
            ?: throw IllegalStateException("Voucher not found")
    }

    private val USER_REWARD_COLUMNS = Columns.raw(
        "user_reward_id, reward_id, status, obtained_at, reward:reward_id(*)"
    )

    fun isVoucherRedeemed(voucher: UserVoucher): Boolean {
        return voucher.status.equals(STATUS_REDEEMED, ignoreCase = true)
    }
}
