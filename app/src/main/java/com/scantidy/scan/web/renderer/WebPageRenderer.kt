package com.scantidy.scan.web.renderer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.print.PrintAttributes
import android.print.pdf.PrintedPdfDocument
import android.view.View
import android.webkit.WebView
import com.scantidy.scan.web.url.UrlValidator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 链接 → PDF 渲染器
 *  - 加载 URL 到 WebView
 *  - 等 onPageFinished
 *  - 整页截屏 → 通过 PrintedPdfDocument 输出 PDF
 *  - v0.1 简化：单页（不做分页）
 *
 * 重要：本功能因为没有 INTERNET 权限，**实际无法在没开权限的设备上工作**。
 * UI 层必须在用户开启 INTERNET 权限后才能调用。
 */
@Singleton
class WebPageRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val urlValidator: UrlValidator
) {

    /**
     * 主入口
     * @return 渲染好的 PDF 文件
     */
    suspend fun renderUrlToPdf(url: String, output: File): File = withContext(Dispatchers.IO) {
        require(urlValidator.isValid(url)) { "Invalid URL: $url" }
        val webView = createWebView()
        try {
            val bitmap = captureWholePage(webView, url)
            bitmapToPdf(bitmap, output)
            output
        } finally {
            webView.destroy()
        }
    }

    private fun createWebView(): WebView {
        return WebView(context).apply {
            settings.javaScriptEnabled = false           // 安全
            settings.domStorageEnabled = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            // 沙箱：禁用 file 访问
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            // 桌面 UA：很多站点对手机 UA 返回 mobile 版
            settings.userAgentString = "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 PDFScannerSaver/0.1"
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
        }
    }

    private suspend fun captureWholePage(webView: WebView, url: String): Bitmap = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation {
            webView.stopLoading()
        }
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView, finishedUrl: String?) {
                // 给 300ms 让布局稳定
                view.postDelayed({
                    try {
                        val bmp = captureBitmap(view)
                        if (cont.isActive) cont.resume(bmp)
                    } catch (e: Throwable) {
                        if (cont.isActive) cont.resumeWithException(e)
                    }
                }, 300L)
            }

            override fun onReceivedError(
                view: WebView,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                val msg = error?.description?.toString() ?: "Unknown error"
                Timber.w("WebView error: $msg")
                if (cont.isActive) cont.resumeWithException(IllegalStateException(msg))
            }
        }
        webView.loadUrl(url)
    }

    private fun captureBitmap(view: WebView): Bitmap {
        // 测量并布局
        val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(widthSpec, heightSpec)
        val measuredH = view.measuredHeight.coerceAtLeast(800)
        view.layout(0, 0, 1080, measuredH)

        val bmp = Bitmap.createBitmap(1080, measuredH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        // 兜底：先涂白，避免透明背景
        canvas.drawColor(Color.WHITE)
        view.draw(canvas)
        return bmp
    }

    private fun bitmapToPdf(bitmap: Bitmap, output: File): File {
        output.parentFile?.mkdirs()
        // A4 in points: 595 x 842
        val pdfDoc = PrintedPdfDocument(
            context,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
        )
        val pageInfo = PdfDocument.PageInfo.Builder(
            PrintAttributes.MediaSize.ISO_A4.widthMils,
            PrintAttributes.MediaSize.ISO_A4.heightMils,
            1
        ).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        // 把 bitmap 缩放到 A4 画布
        val pageW = pageInfo.pageWidth.toFloat()
        val pageH = pageInfo.pageHeight.toFloat()
        val bmpW = bitmap.width.toFloat()
        val bmpH = bitmap.height.toFloat()
        val scale = minOf(pageW / bmpW, pageH / bmpH)
        val drawW = bmpW * scale
        val drawH = bmpH * scale
        val offX = (pageW - drawW) / 2f
        val offY = (pageH - drawH) / 2f
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, null, RectF(offX, offY, offX + drawW, offY + drawH), paint)
        pdfDoc.finishPage(page)
        FileOutputStream(output).use { pdfDoc.writeTo(it) }
        pdfDoc.close()
        bitmap.recycle()
        return output
    }
}
