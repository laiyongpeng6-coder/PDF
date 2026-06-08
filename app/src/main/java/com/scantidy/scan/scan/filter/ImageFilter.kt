package com.scantidy.scan.scan.filter

import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 滤镜模式
 */
enum class FilterMode {
    ORIGINAL,    // 原图
    ENHANCED,    // 增强（去阴影 + 自适应对比度）
    GRAYSCALE,   // 灰度
    BLACK_WHITE  // 黑白二值化
}

/**
 * 图像滤镜
 * 全部在 OpenCV Mat 上操作
 */
@Singleton
class ImageFilter @Inject constructor() {

    fun apply(bgr: Mat, mode: FilterMode): Mat {
        return when (mode) {
            FilterMode.ORIGINAL -> bgr.clone()
            FilterMode.ENHANCED -> enhance(bgr)
            FilterMode.GRAYSCALE -> grayscale(bgr)
            FilterMode.BLACK_WHITE -> blackAndWhite(bgr)
        }
    }

    /**
     * 增强：
     *  1. 灰度
     *  2. 自适应阈值（局部）
     *  3. 反相（黑底白字）→ 视情况
     *  4. 转回 3 通道（BGR 看起来跟灰度一样，但适配 PDF 渲染）
     */
    private fun enhance(src: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

        // 形态学梯度去阴影（简单的形态学 top-hat）
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, org.opencv.core.Size(15.0, 15.0))
        val tophat = Mat()
        Imgproc.morphologyEx(gray, tophat, Imgproc.MORPH_TOPHAT, kernel)
        val sharpened = Mat()
        org.opencv.core.Core.add(gray, tophat, sharpened)
        tophat.release()
        kernel.release()

        // 自适应阈值
        val bw = Mat()
        Imgproc.adaptiveThreshold(
            sharpened, bw, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY, 21, 8.0
        )
        sharpened.release()

        // 转回 BGR（便于统一处理）
        val out = Mat()
        Imgproc.cvtColor(bw, out, Imgproc.COLOR_GRAY2BGR)
        gray.release()
        bw.release()
        return out
    }

    private fun grayscale(src: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        val out = Mat()
        Imgproc.cvtColor(gray, out, Imgproc.COLOR_GRAY2BGR)
        gray.release()
        return out
    }

    private fun blackAndWhite(src: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        val bw = Mat()
        Imgproc.threshold(gray, bw, 0.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)
        val out = Mat()
        Imgproc.cvtColor(bw, out, Imgproc.COLOR_GRAY2BGR)
        gray.release()
        bw.release()
        return out
    }
}
