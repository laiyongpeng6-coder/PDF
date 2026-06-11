package com.scantidy.scan.ui.premium

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scantidy.scan.R
import com.scantidy.scan.core.analytics.AnalyticsTracker
import com.scantidy.scan.core.billing.PremiumFeature
import com.scantidy.scan.core.billing.ProductCatalog
import com.scantidy.scan.ui.theme.Primary

/**
 * 高级版购买页面
 *
 * 布局：
 * - 顶部：标题 + 返回按钮
 * - 功能列表（已解锁 / 锁定状态）
 * - 定价卡片（年度推荐 / 月度 / 永久）
 * - 购买按钮
 */
@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    viewModel: PremiumViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        AnalyticsTracker.logPremiumViewed()
        viewModel.refreshPurchases()
    }

    // 购买成功弹窗
    if (state.purchaseSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.clearSuccess() },
            title = { Text("🎉 购买成功！") },
            text = { Text("所有高级功能已解锁，感谢支持！") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearSuccess()
                    onBack()
                }) {
                    Text("开始使用")
                }
            }
        )
    }

    // 错误提示
    state.purchaseError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("支付失败") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("重试")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("高级版") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 头部标语
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "解锁全部高级功能",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "离线安全 · 无广告 · 终生可用",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 功能列表
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumFeature.entries.forEach { feature ->
                            FeatureRow(feature)
                        }
                    }
                }
            }

            // 定价卡片
            val products = state.products

            // 年度订阅（推荐）
            val yearly = products.find { it.productId == ProductCatalog.ID_PREMIUM_YEARLY }
            if (yearly != null) {
                item {
                    PricingCard(
                        title = "年度会员",
                        price = yearly.price,
                        period = "每年自动续期",
                        badge = "最受欢迎",
                        isRecommended = true,
                        isSelected = false,
                        onClick = {
                            AnalyticsTracker.logPurchaseInitiated(ProductCatalog.ID_PREMIUM_YEARLY)
                            (context as? Activity)?.let {
                                viewModel.launchPurchase(it, ProductCatalog.ID_PREMIUM_YEARLY)
                            }
                        }
                    )
                }
            }

            // 月度订阅
            val monthly = products.find { it.productId == ProductCatalog.ID_PREMIUM_MONTHLY }
            if (monthly != null) {
                item {
                    PricingCard(
                        title = "月度会员",
                        price = monthly.price,
                        period = "每月自动续期，可随时取消",
                        badge = null,
                        isRecommended = false,
                        isSelected = false,
                        onClick = {
                            AnalyticsTracker.logPurchaseInitiated(ProductCatalog.ID_PREMIUM_MONTHLY)
                            (context as? Activity)?.let {
                                viewModel.launchPurchase(it, ProductCatalog.ID_PREMIUM_MONTHLY)
                            }
                        }
                    )
                }
            }

            // 永久解锁
            val oneTime = products.find { it.productId == ProductCatalog.ID_PREMIUM_UNLOCK }
            if (oneTime != null) {
                item {
                    PricingCard(
                        title = "永久解锁",
                        price = oneTime.price,
                        period = "一次付费，永久使用",
                        badge = "最超值",
                        isRecommended = false,
                        isSelected = false,
                        onClick = {
                            AnalyticsTracker.logPurchaseInitiated(ProductCatalog.ID_PREMIUM_UNLOCK)
                            (context as? Activity)?.let {
                                viewModel.launchPurchase(it, ProductCatalog.ID_PREMIUM_UNLOCK)
                            }
                        }
                    )
                }
            }

            // 加载中
            if (products.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isConnected) {
                            Text("加载商品信息…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("连接 Google Play 中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 已购买提示
            if (state.isPremium) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(Primary.value).copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(Primary.value),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "您已是高级会员，所有功能已解锁",
                                color = Color(Primary.value),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 底部间距
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun FeatureRow(feature: PremiumFeature) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(Primary.value).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Star,
                contentDescription = null,
                tint = Color(Primary.value),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                feature.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                feature.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PricingCard(
    title: String,
    price: String,
    period: String,
    badge: String?,
    isRecommended: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isRecommended)
        Color(Primary.value)
    else
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isRecommended) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Badge + 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (badge != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(Primary.value).copy(alpha = 0.15f)
                    ) {
                        Text(
                            badge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(Primary.value)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 价格
            if (price.isNotEmpty()) {
                val priceAnnotated = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold)) {
                        append(price)
                    }
                }
                Text(
                    priceAnnotated,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    "加载中…",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                period,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isRecommended) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "💡 推荐 — 比月度省 40%",
                    fontSize = 13.sp,
                    color = Color(Primary.value),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
