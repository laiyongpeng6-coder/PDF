package com.scantidy.scan.core.billing

/**
 * Google Play 商品目录
 *
 * 需在 Google Play Console → 商品 → 应用内商品 中配置相同 ID：
 * - pdf_premium_unlock: 一次性商品 (inapp, non-consumable)
 * - pdf_premium_monthly: 月度自动续期订阅 (auto-renewing sub)
 * - pdf_premium_yearly: 年度自动续期订阅 (auto-renewing sub)
 */
object ProductCatalog {

    // --- 商品 ID ---
    const val ID_PREMIUM_UNLOCK = "pdf_premium_unlock"
    const val ID_PREMIUM_MONTHLY = "pdf_premium_monthly"
    const val ID_PREMIUM_YEARLY = "pdf_premium_yearly"

    /** 所有在售商品 ID 列表（用于 queryProductDetails） */
    val allProductIds = setOf(ID_PREMIUM_UNLOCK, ID_PREMIUM_MONTHLY, ID_PREMIUM_YEARLY)

    /** 订阅类商品 ID */
    val subscriptionIds = setOf(ID_PREMIUM_MONTHLY, ID_PREMIUM_YEARLY)

    /** 一次性商品 ID */
    val inappIds = setOf(ID_PREMIUM_UNLOCK)

    /** 判断是否为订阅 */
    fun isSubscription(productId: String) = productId in subscriptionIds
}

/**
 * 高级功能清单。
 * 购买后这些功能解锁；未购买时显示付费墙提示。
 */
enum class PremiumFeature(
    val displayName: String,
    val description: String
) {
    PDF_ENCRYPT("PDF 加密", "为 PDF 添加密码保护"),
    PDF_DECRYPT("PDF 解密", "移除 PDF 密码"),
    WATERMARK("添加水印", "文字/图片水印，防泄露"),
    OCR_FULL_TEXT("全文 OCR", "扫描并识别文档全文，支持搜索"),
    BATCH_CONVERT("批量转换", "一次转换多个文件"),
    ADVANCED_FILTERS("高级滤镜", "增强/灰度/黑白等多档滤镜"),
    NO_ADS("无水印保存", "保存的 PDF 不带品牌水印")
}
