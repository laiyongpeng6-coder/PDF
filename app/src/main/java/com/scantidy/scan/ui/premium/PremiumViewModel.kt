package com.scantidy.scan.ui.premium

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.scantidy.scan.core.billing.BillingController
import com.scantidy.scan.core.billing.ProductCatalog
import com.scantidy.scan.core.billing.ProductUiModel
import com.scantidy.scan.core.billing.SubscriptionTier
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PremiumUiState(
    val products: List<ProductUiModel> = emptyList(),
    val isPremium: Boolean = false,
    val isConnected: Boolean = false,
    val purchaseInProgress: Boolean = false,
    val purchaseError: String? = null,
    val purchaseSuccess: Boolean = false
)

class PremiumViewModel(application: Application) : AndroidViewModel(application) {

    private val billing = BillingController.getInstance(application)

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    init {
        billing.connect()
        observeBillingState()
    }

    private fun observeBillingState() {
        viewModelScope.launch {
            // 连接状态
            launch {
                billing.isConnected.collect { connected ->
                    _uiState.update { it.copy(isConnected = connected) }
                }
            }
            // 高级状态
            launch {
                billing.isPremium.collect { premium ->
                    _uiState.update { it.copy(isPremium = premium) }
                }
            }
            // 购买中
            launch {
                billing.purchaseInProgress.collect { inProgress ->
                    _uiState.update { it.copy(purchaseInProgress = inProgress) }
                }
            }
            // 错误
            launch {
                billing.purchaseError.collect { error ->
                    _uiState.update { it.copy(purchaseError = error) }
                }
            }
            // 商品详情
            launch {
                billing.productDetails.collect { details ->
                    _uiState.update { it.copy(products = mapToUiModels(details)) }
                }
            }
        }
    }

    fun launchPurchase(activity: android.app.Activity, productId: String) {
        val product = _uiState.value.products
            .find { it.productId == productId } ?: return
        billing.clearError()
        billing.launchPurchase(activity, product.productDetails)
    }

    fun refreshPurchases() {
        billing.queryPurchases()
    }

    fun consumePurchaseSuccess(): Boolean {
        val success = billing.consumePurchaseSuccess()
        if (success) {
            _uiState.update { it.copy(purchaseSuccess = true) }
        }
        return success
    }

    fun clearError() {
        billing.clearError()
    }

    fun clearSuccess() {
        _uiState.update { it.copy(purchaseSuccess = false) }
    }

    private fun mapToUiModels(details: List<ProductDetails>): List<ProductUiModel> {
        val oneTimeDetail = details.find { it.productId == ProductCatalog.ID_PREMIUM_UNLOCK }
        val monthlyDetail = details.find { it.productId == ProductCatalog.ID_PREMIUM_MONTHLY }
        val yearlyDetail = details.find { it.productId == ProductCatalog.ID_PREMIUM_YEARLY }

        return listOfNotNull(
            yearlyDetail?.toUiModel(SubscriptionTier.YEARLY),
            monthlyDetail?.toUiModel(SubscriptionTier.MONTHLY),
            oneTimeDetail?.toUiModel(null)
        )
    }

    override fun onCleared() {
        super.onCleared()
        billing.disconnect()
    }
}

private fun ProductDetails.toUiModel(tier: SubscriptionTier?): ProductUiModel {
    val oneTimePricing = oneTimePurchaseOfferDetails?.formattedPrice ?: ""
    val subPricing = subscriptionOfferDetails?.firstOrNull()?.let { offer ->
        offer.pricingPhases.pricingPhaseList.firstOrNull()?.formattedPrice ?: ""
    } ?: ""

    val price = when {
        tier != null -> "$subPricing/${when (tier) {
            SubscriptionTier.MONTHLY -> "月"
            SubscriptionTier.YEARLY -> "年"
        }}"
        else -> oneTimePricing
    }

    val rawPriceMicros = when {
        tier != null -> subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            ?.priceAmountMicros ?: 0L
        else -> oneTimePurchaseOfferDetails?.priceAmountMicros ?: 0L
    }

    return ProductUiModel(
        productId = productId,
        title = tier?.displayName ?: "永久解锁",
        description = if (tier != null) tier.periodText else "一次付费，永久使用",
        price = price,
        rawPriceMicros = rawPriceMicros,
        currencyCode = "",
        tier = tier ?: SubscriptionTier.MONTHLY,
        isOneTime = tier == null,
        productDetails = this
    )
}
