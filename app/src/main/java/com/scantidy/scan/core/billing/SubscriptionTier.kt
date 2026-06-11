package com.scantidy.scan.core.billing

/**
 * 订阅等级
 */
enum class SubscriptionTier(
    val displayName: String,
    val periodText: String,
    val savingsText: String?
) {
    MONTHLY("月度", "每月", null),
    YEARLY("年度", "每年", "省 40%")
}

/**
 * 商品展示数据（映射到 UI）
 */
data class ProductUiModel(
    val productId: String,
    val title: String,
    val description: String,
    val price: String,
    val rawPriceMicros: Long,
    val currencyCode: String,
    val tier: SubscriptionTier,
    val isOneTime: Boolean,
    val productDetails: com.android.billingclient.api.ProductDetails // 原始对象
)
