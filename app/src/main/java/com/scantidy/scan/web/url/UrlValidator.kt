package com.scantidy.scan.web.url

import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * URL 校验
 *  - 只允许 http / https
 *  - 拒绝 file / javascript / data / ftp 等
 *  - 必须有 host
 */
@Singleton
class UrlValidator @Inject constructor() {

    enum class Validity {
        VALID,
        EMPTY,
        INVALID_SCHEME,
        NO_HOST,
        MALFORMED
    }

    fun validate(input: String): Validity {
        val s = input.trim()
        if (s.isEmpty()) return Validity.EMPTY
        val lower = s.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return Validity.INVALID_SCHEME
        }
        return try {
            val uri = URI(s)
            if (uri.host.isNullOrBlank()) Validity.NO_HOST else Validity.VALID
        } catch (e: URISyntaxException) {
            Validity.MALFORMED
        }
    }

    fun isValid(input: String): Boolean = validate(input) == Validity.VALID

    /**
     * 提取 host 用于显示
     */
    fun hostOf(input: String): String? = try {
        URI(input).host
    } catch (e: Throwable) {
        null
    }
}
