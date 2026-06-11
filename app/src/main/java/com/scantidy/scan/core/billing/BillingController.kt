package com.scantidy.scan.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Google Play Billing 控制器
 *
 * 职责：
 * - 初始化 BillingClient 并连接到 Google Play
 * - 查询商品详情 (queryProductDetailsAsync)
 * - 查询已购买项目 (queryPurchasesAsync)
 * - 发起购买流程 (launchBillingFlow)
 * - 确认消费/确认购买 (acknowledgePurchase)
 */
class BillingController private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: BillingController? = null

        fun getInstance(context: Context): BillingController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingController(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // --- BillingClient ---
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else {
            _purchaseError.value = billingResult.debugMessage
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    // --- 状态 ---
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails: StateFlow<List<ProductDetails>> = _productDetails.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _purchaseInProgress = MutableStateFlow(false)
    val purchaseInProgress: StateFlow<Boolean> = _purchaseInProgress.asStateFlow()

    private val _purchaseError = MutableStateFlow<String?>(null)
    val purchaseError: StateFlow<String?> = _purchaseError.asStateFlow()

    private val _purchaseSuccess = MutableStateFlow(false)

    // --- 连接 ---
    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("BillingClient connected")
                    _isConnected.value = true
                    queryProducts()
                    queryPurchases()
                } else {
                    Timber.w("BillingClient setup failed: ${result.debugMessage}")
                    _isConnected.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                Timber.w("BillingClient disconnected")
                _isConnected.value = false
                // 自动重连
                connect()
            }
        })
    }

    // --- 查询商品 ---
    private fun queryProducts() {
        val inappParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ProductCatalog.inappIds.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            ).build()
        billingClient.queryProductDetailsAsync(inappParams) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Timber.d("In-app products: ${details.map { it.productId }}")
                querySubscriptions(details)
            }
        }
    }

    private fun querySubscriptions(existingInapp: List<ProductDetails>) {
        val subParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ProductCatalog.subscriptionIds.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            ).build()
        billingClient.queryProductDetailsAsync(subParams) { result, details ->
            val all = existingInapp.toMutableList()
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                all.addAll(details)
            }
            _productDetails.value = all
        }
    }

    // --- 查询已购 ---
    fun queryPurchases() {
        // 一次性商品
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            }
        }
        // 订阅
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val hasPremium = purchases.any { purchase ->
            when {
                purchase.purchaseState != Purchase.PurchaseState.PURCHASED -> false
                ProductCatalog.inappIds.contains(purchase.products.firstOrNull()) ||
                ProductCatalog.subscriptionIds.contains(purchase.products.firstOrNull()) -> true
                else -> false
            }
        }
        _isPremium.value = hasPremium
    }

    // --- 发起购买 ---
    fun launchPurchase(activity: Activity, productDetails: ProductDetails) {
        _purchaseInProgress.value = true
        _purchaseError.value = null

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            // 订阅类需要 offerToken
            .apply {
                if (productDetails.productType == BillingClient.ProductType.SUBS) {
                    productDetails.subscriptionOfferDetails?.firstOrNull()?.let {
                        setOfferToken(it.offerToken)
                    }
                }
            }
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams).let { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _purchaseInProgress.value = false
                _purchaseError.value = result.debugMessage
            }
        }
    }

    // --- 处理购买结果 ---
    private fun handlePurchase(purchase: Purchase) {
        _purchaseInProgress.value = false

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            acknowledgePurchase(purchase)
            _isPremium.value = true
            _purchaseSuccess.value = true
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return

        val ackParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(ackParams) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Timber.d("Purchase acknowledged: ${purchase.products}")
            }
        }
    }

    /** 消费成功状态（一次性读取） */
    fun consumePurchaseSuccess(): Boolean {
        val success = _purchaseSuccess.value
        _purchaseSuccess.value = false
        return success
    }

    /** 清除错误 */
    fun clearError() {
        _purchaseError.value = null
    }

    fun disconnect() {
        billingClient.endConnection()
    }
}
