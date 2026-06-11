package com.scantidy.scan.core.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

/**
 * Firebase Analytics 埋点工具
 *
 * 事件命名规范：snake_case，如 scan_completed / premium_viewed
 * 参数命名规范：snake_case，如 document_pages / premium_plan
 */
object AnalyticsTracker {

    private var instance: FirebaseAnalytics? = null

    fun init() {
        instance = Firebase.analytics
    }

    // ---- 启动流程 ----

    fun logSplashShown() {
        log("splash_shown")
    }

    fun logOnboardingStarted() {
        log("onboarding_started")
    }

    fun logOnboardingCompleted() {
        log("onboarding_completed")
    }

    fun logOnboardingSkipped() {
        log("onboarding_skipped")
    }

    // ---- 扫描 ----

    fun logScanStarted() {
        log("scan_started")
    }

    fun logScanCompleted(pages: Int, durationMs: Long) {
        log("scan_completed", mapOf(
            "document_pages" to pages.toString(),
            "duration_ms" to durationMs.toString()
        ))
    }

    // ---- PDF 保存 ----

    fun logPdfSaved(pages: Int, hasOcr: Boolean, hasEncrypt: Boolean, hasWatermark: Boolean) {
        log("pdf_saved", mapOf(
            "pages" to pages.toString(),
            "has_ocr" to hasOcr.toString(),
            "has_encrypt" to hasEncrypt.toString(),
            "has_watermark" to hasWatermark.toString()
        ))
    }

    fun logPdfOpened(pageCount: Int) {
        log("pdf_opened", mapOf("page_count" to pageCount.toString()))
    }

    // ---- PDF 编辑 ----

    fun logPdfMerged(fileCount: Int) {
        log("pdf_merged", mapOf("file_count" to fileCount.toString()))
    }

    fun logPdfSplit(pages: Int) {
        log("pdf_split", mapOf("pages" to pages.toString()))
    }

    fun logPdfEncrypted() {
        log("pdf_encrypted")
    }

    fun logPdfDecrypted() {
        log("pdf_decrypted")
    }

    fun logWatermarkAdded() {
        log("watermark_added")
    }

    fun logFormatConverted(from: String, to: String) {
        log("format_converted", mapOf(
            "from_format" to from,
            "to_format" to to
        ))
    }

    // ---- 链接 ----

    fun logLinkConverted() {
        log("link_converted")
    }

    // ---- OCR ----

    fun logOcrPerformed(language: String) {
        log("ocr_performed", mapOf("language" to language))
    }

    // ---- 搜索 ----

    fun logSearchPerformed(query: String) {
        log("search_performed", mapOf("has_query" to query.isNotEmpty().toString()))
    }

    // ---- 商业化 ----

    fun logPremiumViewed() {
        log("premium_viewed")
    }

    fun logPurchaseInitiated(productId: String) {
        log("purchase_initiated", mapOf("product_id" to productId))
    }

    fun logPurchaseCompleted(productId: String) {
        log("purchase_completed", mapOf("product_id" to productId))
    }

    fun logPurchaseFailed(productId: String, errorMsg: String) {
        log("purchase_failed", mapOf(
            "product_id" to productId,
            "error" to errorMsg
        ))
    }

    // ---- 设置 ----

    fun logThemeChanged(mode: String) {
        log("theme_changed", mapOf("mode" to mode))
    }

    fun logLanguageChanged(lang: String) {
        log("language_changed", mapOf("language" to lang))
    }

    // ---- 分享 ----

    fun logPdfShared() {
        log("pdf_shared")
    }

    // ---- 通用 ----

    fun logFirstLaunch() {
        log("first_launch")
    }

    fun logAppOpened() {
        log("app_opened")
    }

    // ---- 内部 ----

    private fun log(event: String, params: Map<String, String> = emptyMap()) {
        val bundle = Bundle().apply {
            params.forEach { (k, v) -> putString(k, v) }
        }
        instance?.logEvent(event, bundle)
    }
}
