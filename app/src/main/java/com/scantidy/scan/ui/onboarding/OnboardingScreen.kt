package com.scantidy.scan.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scantidy.scan.R
import com.scantidy.scan.ui.theme.Primary
import kotlinx.coroutines.launch

/**
 * 新手引导页数据
 */
private data class PageData(
    val icon: ImageVector,
    val title: String,
    val description: String
)

/**
 * 新手引导品宣页
 * - 左右滑动切换 4 页
 * - 底部指示器 + 跳过 / 开始使用 按钮
 * - 完成后回调 onFinished
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        PageData(
            icon = Icons.Outlined.CameraAlt,
            title = stringResource(R.string.onboarding_scan_title),
            description = stringResource(R.string.onboarding_scan_desc)
        ),
        PageData(
            icon = Icons.Outlined.Security,
            title = stringResource(R.string.onboarding_privacy_title),
            description = stringResource(R.string.onboarding_privacy_desc)
        ),
        PageData(
            icon = Icons.Outlined.Handyman,
            title = stringResource(R.string.onboarding_tools_title),
            description = stringResource(R.string.onboarding_tools_desc)
        ),
        PageData(
            icon = Icons.Outlined.FolderOpen,
            title = stringResource(R.string.onboarding_manage_title),
            description = stringResource(R.string.onboarding_manage_desc)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val isLastPage = currentPage == pages.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部操作栏：页码 + 跳过
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 页码指示
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (index == currentPage) 20.dp else 8.dp,
                                height = 8.dp
                            )
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (index == currentPage) Color(Primary.value)
                                else Color(Primary.value).copy(alpha = 0.3f)
                            )
                    )
                    if (index < pages.lastIndex) Spacer(modifier = Modifier.width(6.dp))
                }
            }

            // 跳过按钮
            if (!isLastPage) {
                TextButton(onClick = onFinished) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 滑动页面
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            OnboardingPageContent(
                page = pages[page],
                modifier = Modifier.fillMaxSize()
            )
        }

        // 底部按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLastPage) {
                Button(
                    onClick = onFinished,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(Primary.value)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_start),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onFinished
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(Primary.value)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_next),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单页内容
 */
@Composable
private fun OnboardingPageContent(
    page: PageData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(Primary.value).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(Primary.value)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 大标题
        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 描述文字
        Text(
            text = page.description,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}
