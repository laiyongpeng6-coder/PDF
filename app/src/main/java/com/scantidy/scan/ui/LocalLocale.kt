package com.scantidy.scan.ui

import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

/**
 * 当前 App 显示语言（Locale）
 * - 默认 = 系统 Locale
 * - 设置页可强制选择具体语言（覆盖系统）
 *
 * 由于 v0.1 不做 in-app 切换，仅作为后续扩展的钩子；
 * 当前使用 Android 系统级多语言资源切换（res/values-xx）。
 */
val LocalAppLocale = compositionLocalOf { Locale.getDefault() }
