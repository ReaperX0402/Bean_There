package com.example.myapplication.data

import com.example.myapplication.model.Reward
import com.example.myapplication.model.UserVoucher
import io.github.jan.supabase.postgrest.delete
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object RewardRepository {

    class InsufficientPointsException : Exception("Insufficient points to redeem this reward.")

    data class RedemptionResult(
        val voucher: UserVoucher,
        val remainingPoints: Int
    )

    private val client get() = SupabaseProvider.client
    private const val STATUS_AVAILABLE = "claimed"
    private const val STATUS_REDEEMED = "redeemed"

    @Serializable
    private data class RewardResponse(
        @SerialName("reward_id") val id: String,
        @SerialName("name") val name: String,
        @SerialName("description") val description: String? = null,
        @SerialName("point_cost") val pointCost: Int,
        @SerialName("inventory") val inventory: Int? = null,
        @SerialName("active") val active: Boolean
    ) {
        fun toReward(): Reward = Reward(
            id = id,
            name = name,
            description = description,
            pointsRequired = pointCost,
            imageUrl = null,
            status = if (active) "active" else "inactive"
        )
    }

    @Serializable
    private data class UserRewardResponse(
        @SerialName("user_reward_id") val userRewardId: String,
        @SerialName("reward_id") val rewardId: String,
        @SerialName("status") val status: String,
        @SerialName("claimed_at") val claimedAt: String,
        @SerialName("code") val code: String? = null,
        @SerialName("reward") val reward: RewardResponse? = null
    ) {
        fun toUserVoucher(fallbackReward: Reward? = null): UserVoucher? {
            val rewardModel = reward?.toReward() ?: fallbackReward ?: return null
            return UserVoucher(
                id = userRewardId,
                reward = rewardModel,
                status = status,
                obtainedAt = claimedAt,
                code = code
            )
        }
    }

    @Serializable
    private data class UserPointsResponse(
        @SerialName("total_point") val points: Int = 0
    )

    suspend fun getRewards(): List<Reward> = withContext(Dispatchers.IO) {
        client.from("reward")
            .select(
                Columns.list("reward_id", "name", "description", "point_cost", "inventory", "active")
            ) {
                filter { eq("active", true) }
            }
            .decodeList<RewardResponse>()
            .map { it.toReward() }
    }

    suspend fun getUserPoints(userId: String): Int = withContext(Dispatchers.IO) {
        client.from("user")
            .select(columns = Columns.list("total_point")) {
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
        if (currentPoints < reward.pointsRequired) throw InsufficientPointsException()

        // 1) Deduct points
        val newPoints = currentPoints - reward.pointsRequired
        val updated = client.from("user")
            .update({ set("total_point", newPoints) }) {
                filter { eq("user_id", userId) }
                select(columns = Columns.list("total_point"))
                limit(1)
            }
            .decodeSingle<UserPointsResponse>()
            .points
        val voucher = client.from("user_reward")
            .insert(
                mapOf(
                    "user_id" to userId,
                    "reward_id" to reward.id,
                    "status" to STATUS_AVAILABLE
                    // claimed_at can be defaulted in DB with now()
                )
            ) {
                select(columns = USER_REWARD_COLUMNS)
            }
            .decodeSingle<UserRewardResponse>()
            .toUserVoucher(fallbackReward = reward)
            ?: throw IllegalStateException("Failed to create user voucher")

        RedemptionResult(voucher, updated)
    }

    suspend fun useVoucher(userRewardId: String): UserVoucher = withContext(Dispatchers.IO) {
        val updatedVoucher = client.from("user_reward")
            .update({
                set("status", STATUS_REDEEMED)
            }) {
                filter { eq("user_reward_id", userRewardId) }
                select(columns = USER_REWARD_COLUMNS)
                limit(1)
            }
            .decodeSingle<UserRewardResponse>()
            .toUserVoucher()
            ?: throw IllegalStateException("Voucher not found")

        updatedVoucher
    }

    private val USER_REWARD_COLUMNS = Columns.raw(
        // join the reward so UI has full details
        "user_reward_id, reward_id, status, claimed_at, code, reward:reward_id(*)"
    )

    suspend fun deleteVoucher(userRewardId: String) = withContext(Dispatchers.IO) {
        client.from("user_reward")
            .delete {
                filter { eq("user_reward_id", userRewardId) }
            }
    }

    fun isVoucherAvailable(voucher: UserVoucher): Boolean =
        voucher.status.equals(STATUS_AVAILABLE, ignoreCase = true)
}
