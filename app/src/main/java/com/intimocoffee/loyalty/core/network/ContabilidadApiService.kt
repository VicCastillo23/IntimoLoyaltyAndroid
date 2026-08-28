package com.intimocoffee.loyalty.core.network

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET

interface ContabilidadApiService {

    @GET("api/public/loyalty/promos")
    suspend fun getLoyaltyPromos(): Response<ApiResponse<List<LoyaltyPromoResponse>>>
}

@Serializable
data class LoyaltyPromoResponse(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val ctaLabel: String = "",
    val imagePath: String = "",
    val imageUrl: String = "",
    val linkUrl: String = "",
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val rewardId: Long? = null,
    val isRedeemable: Boolean = false,
    val pointsCost: Int = 0,
    val discountPercent: Int? = null,
    val category: String = "EVENT",
    val triggerType: String = "CLAIMABLE",
    val validDays: Int? = null,
    val maxRedemptionsPerCustomer: Int? = null,
    val couponCode: String? = null,
    val validFrom: String? = null,
    val validUntil: String? = null,
)
