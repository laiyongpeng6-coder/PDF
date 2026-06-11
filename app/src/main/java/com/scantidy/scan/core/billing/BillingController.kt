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
 * - 查询商品详情（inapp / subs 并行，互不影响）
 * - 查询已购买项目
 * - 发起购买流程
 * - 确认购买
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

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) handlePurchase(purchase)
        } else {
            _purchaseError.value = billingResult.debugMessage
            Timber.w("Purchase error: ${billingResult.responseCode} ${billingResult.debugMessage}")
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

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

    // ---- 连接 ----
    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("BillingClient connected")
                    _isConnected.value = true
                    queryAllProducts()
                    queryPurchases()
                } else {
                    Timber.w("Billing setup failed: ${result.debugMessage}")
                    _isConnected.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                Timber.w("BillingClient disconnected, reconnecting…")
                _isConnected.value = false
                connect()
            }
        })
    }

    // ---- 查询商品（inapp + subs 并行，互不干扰）----
    private fun queryAllProducts() {
        val all = mutableListOf<ProductDetails>()
        var pending = 2

        fun collect() {
            if (--pending <= 0) _productDetails.value = all
        }

        // inapp
        if (ProductCatalog.inappIds.isNotEmpty()) {
            billingClient.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder()
                    .setProductList(ProductCatalog.inappIds.map {
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(it)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    }).build()
            ) { result, details ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) all.addAll(details)
                collect()
            }
        } else {
            collect()
        }

        // subs
        if (ProductCatalog.subscriptionIds.isNotEmpty()) {
            billingClient.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder()
                    .setProductList(ProductCatalog.subscriptionIds.map {
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(it)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    }).build()
            ) { result, details ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) all.addAll(details)
                collect()
            }
        } else {
            collect()
        }
    }

    // ---- 已购查询 ----
    fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) processPurchases(purchases)
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) processPurchases(purchases)
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val hasPremium = purchases.any { p ->
            p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                (p.products.firstOrNull() in ProductCatalog.inappIds ||
                 p.products.firstOrNull() in ProductCatalog.subscriptionIds)
        }
        _isPremium.value = hasPremium
    }

    // ---- 发起购买 ----
    fun launchPurchase(activity: Activity, productDetails: ProductDetails) {
        _purchaseInProgress.value = true
        _purchaseError.value = null

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .apply {
                if (productDetails.productType == BillingClient.ProductType.SUBS) {
                    productDetails.subscriptionOfferDetails?.firstOrNull()?.let {
                        setOfferToken(it.offerToken)
                    }
                }
            }
            .build()

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseInProgress.value = false
            _purchaseError.value = "Billing flow failed: ${result.debugMessage}"
            Timber.e("launchBillingFlow error: ${result.responseCode} ${result.debugMessage}")
        }
    }

    // ---- 处理结果 ----
    private fun handlePurchase(purchase: Purchase) {
        _purchaseInProgress.value = false
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken).build()
                ) {}
            }
            _isPremium.value = true
            _purchaseSuccess.value = true
        }
    }

    fun consumePurchaseSuccess(): Boolean {
        val s = _purchaseSuccess.value
        _purchaseSuccess.value = false
        return s
    }

    fun clearError() { _purchaseError.value = null }

    fun disconnect() { billingClient.endConnection() }
}
