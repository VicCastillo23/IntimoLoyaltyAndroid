package com.intimocoffee.loyalty.core.network

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.*

interface LoyaltyApiService {
    
    @POST("/loyalty/customer/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<CustomerResponse>>
    
    @POST("/loyalty/customer/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<CustomerResponse>>

    @POST("/loyalty/customer/set-password")
    suspend fun setPassword(@Body request: SetPasswordRequest): Response<ApiResponse<CustomerResponse>>
    
    @GET("/loyalty/customer/{id}")
    suspend fun getCustomer(@Path("id") id: Long): Response<ApiResponse<CustomerResponse>>
    
    @PUT("/loyalty/customer/{id}")
    suspend fun updateCustomer(@Path("id") id: Long, @Body request: UpdateCustomerRequest): Response<ApiResponse<CustomerResponse>>
    
    @GET("/loyalty/customer/{id}/points")
    suspend fun getPoints(@Path("id") customerId: Long): Response<ApiResponse<PointsResponse>>
    
    @GET("/loyalty/customer/{id}/transactions")
    suspend fun getTransactions(
        @Path("id") customerId: Long,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<List<TransactionResponse>>>
    
    @GET("/loyalty/rewards")
    suspend fun getRewards(): Response<ApiResponse<List<RewardResponse>>>

    @GET("/loyalty/customer/{id}/coupons")
    suspend fun getCoupons(@Path("id") customerId: Long): Response<ApiResponse<List<CouponResponse>>>
    
    @POST("/loyalty/redeem")
    suspend fun redeemReward(@Body request: RedeemRequest): Response<ApiResponse<String>>

    @POST("/loyalty/coupon-code/redeem")
    suspend fun redeemCouponCode(@Body request: PointCouponCodeRedeemRequest): Response<ApiResponse<String>>
    
    @GET("/loyalty/qrcode/{customerId}")
    suspend fun getQRCode(@Path("customerId") customerId: Long): Response<ApiResponse<QRCodeResponse>>
    
    @GET("/health")
    suspend fun healthCheck(): Response<ApiResponse<String>>
}

// Request models
@Serializable
data class RegisterRequest(
    val name: String,
    val phone: String,
    val password: String,
    val email: String? = null,
    val lastName: String? = null,
    val birthDate: String? = null,
    val gender: String? = null,
)

@Serializable
data class LoginRequest(val phone: String, val password: String)

@Serializable
data class SetPasswordRequest(
    val phone: String,
    val newPassword: String,
    val currentPassword: String? = null
)

@Serializable
data class RedeemRequest(val customerId: Long, val rewardId: Long)

@Serializable
data class PointCouponCodeRedeemRequest(val customerId: Long, val code: String)

@Serializable
data class UpdateCustomerRequest(
    val name: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val birthDate: String? = null,
    val gender: String? = null,
    val favoriteDrink: String? = null,
    val allergies: String? = null,
    val notes: String? = null
)

// Response models
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val timestamp: String? = null
)

@Serializable
data class CustomerResponse(
    val id: Long,
    val name: String,
    val lastName: String? = null,
    val phone: String,
    val email: String? = null,
    val birthDate: String? = null,
    val gender: String? = null,
    val favoriteDrink: String? = null,
    val allergies: String? = null,
    val notes: String? = null,
    val totalVisits: Int = 0,
    val currentMonthVisits: Int = 0,
    val totalSpent: Double = 0.0,
    val totalPoints: Int = 0,
    val lifetimePoints: Int = 0,
    val tier: String = "BRONZE",
    val createdAt: String = ""
)

@Serializable
data class PointsResponse(
    val customerId: Long,
    val totalPoints: Int,
    val lifetimePoints: Int,
    val tier: String
)

@Serializable
data class TransactionResponse(
    val id: Long,
    val customerId: Long,
    val orderId: Long? = null,
    val pointsEarned: Int = 0,
    val pointsRedeemed: Int = 0,
    val type: String,
    val description: String? = null,
    val createdAt: String
)

@Serializable
data class RewardResponse(
    val id: Long,
    val name: String,
    val description: String? = null,
    val pointsCost: Int,
    val productId: Long? = null,
    val isActive: Boolean = true,
    val category: String = "POINTS",
    val triggerType: String = "CLAIMABLE",
    val triggerValue: Int? = null,
    val minTier: String? = null,
    val maxRedemptionsPerCustomer: Int? = null,
    val validDays: Int? = null,
    val iconEmoji: String? = null,
    val discountPercent: Int? = null,
    val couponCode: String? = null,
    val validFrom: String? = null,
    val validUntil: String? = null
)

@Serializable
data class QRCodeResponse(
    val customerId: Long,
    val customerName: String,
    val qrData: String
)

@Serializable
data class CouponResponse(
    val redemptionId: Long,
    val customerId: Long,
    val rewardId: Long,
    val rewardName: String,
    val productId: Long? = null,
    val discountPercent: Int? = null,
    val couponCode: String? = null,
    val status: String,
    val expiresAt: Long? = null,
    val qrData: String
)
