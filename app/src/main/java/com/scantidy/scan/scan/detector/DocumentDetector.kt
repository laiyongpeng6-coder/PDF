package com.scantidy.scan.scan.detector

import android.graphics.PointF
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * 文档边检结果
 */
data class DocumentQuad(
    val topLeft: PointF,
    val topRight: PointF,
    val bottomRight: PointF,
    val bottomLeft: PointF
) {
    fun toOpenCVPoints(): Array<Point> = arrayOf(
        Point(topLeft.x.toDouble(), topLeft.y.toDouble()),
        Point(topRight.x.toDouble(), topRight.y.toDouble()),
        Point(bottomRight.x.toDouble(), bottomRight.y.toDouble()),
        Point(bottomLeft.x.toDouble(), bottomLeft.y.toDouble())
    )

    fun reorderClockwise(): DocumentQuad {
        val pts = listOf(topLeft, topRight, bottomRight, bottomLeft)
        val sumSorted = pts.sortedBy { it.x + it.y }   // 最小 = top-left, 最大 = bottom-right
        val diffSorted = pts.sortedBy { it.y - it.x }   // 最小 = top-right, 最大 = bottom-left
        return DocumentQuad(
            topLeft = sumSorted[0],
            topRight = diffSorted[0],
            bottomRight = sumSorted[3],
            bottomLeft = diffSorted[3]
        )
    }

    fun isReasonable(imageWidth: Int, imageHeight: Int): Boolean {
        val w = imageWidth.toFloat()
        val h = imageHeight.toFloat()
        val minSide = min(w, h) * 0.20f
        // 四点都必须在画面里
        val allInBounds = listOf(topLeft, topRight, bottomRight, bottomLeft)
            .all { it.x in 0f..w && it.y in 0f..h }
        if (!allInBounds) return false
        // 至少有一条边 ≥ 20% 短边
        val edges = listOf(
            distance(topLeft, topRight),
            distance(topRight, bottomRight),
            distance(bottomRight, bottomLeft),
            distance(bottomLeft, topLeft)
        )
        return edges.any { it >= minSide } && max(edges[0], edges[2]) > 0 && max(edges[1], edges[3]) > 0
    }

    private fun distance(a: PointF, b: PointF): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

/**
 * 边检算法（OpenCV 实现）
 *
 * 流程：
 *  1. 灰度化 + 高斯模糊
 *  2. Canny 边缘
 *  3. 形态学闭运算（让边连起来）
 *  4. findContours → 找面积最大的四边形
 *  5. 排序成顺时针四顶点
 *
 * 失败时返回 null（让 UI 用整张图）
 */
@Singleton
class DocumentDetector @Inject constructor() {

    /**
     * @param bgr 输入图像（OpenCV Mat，BGR 三通道）
     * @return 检测到的四边形，null = 没找到合适的
     */
    fun detect(bgr: org.opencv.core.Mat): DocumentQuad? {
        if (bgr.empty()) return null
        val w = bgr.width()
        val h = bgr.height()
        if (w < 100 || h < 100) return null

        val gray = org.opencv.core.Mat()
        org.opencv.imgproc.Imgproc.cvtColor(bgr, gray, org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY)
        val blurred = org.opencv.core.Mat()
        org.opencv.imgproc.Imgproc.GaussianBlur(gray, blurred, org.opencv.core.Size(5.0, 5.0), 0.0)

        val edged = org.opencv.core.Mat()
        org.opencv.imgproc.Imgproc.Canny(blurred, edged, 75.0, 200.0)

        // 膨胀 + 腐蚀，让断裂的边缘连起来
        val kernel = org.opencv.imgproc.Imgproc.getStructuringElement(
            org.opencv.imgproc.MorphTypes.MORPH_RECT,
            org.opencv.core.Size(5.0, 5.0)
        )
        val closed = org.opencv.core.Mat()
        org.opencv.imgproc.Imgproc.morphologyEx(edged, closed, org.opencv.imgproc.MorphTypes.MORPH_CLOSE, kernel)

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = org.opencv.core.Mat()
        org.opencv.imgproc.Imgproc.findContours(
            closed,
            contours,
            hierarchy,
            org.opencv.imgproc.RetrType.RETR_EXTERNAL,
            org.opencv.imgproc.ChainApproxMethod.CHAIN_APPROX_SIMPLE
        )

        if (contours.isEmpty()) {
            releaseAll(gray, blurred, edged, closed, hierarchy)
            return null
        }

        // 按面积降序，最多检测前 10 个
        val sorted = contours.sortedByDescending { org.opencv.imgproc.Imgproc.contourArea(it) }
            .take(10)

        var best: DocumentQuad? = null
        var bestScore = 0.0
        for (c in sorted) {
            val peri = org.opencv.imgproc.Imgproc.arcLength(org.opencv.core.MatOfPoint2f(*c.toArray()), true)
            val approx = org.opencv.core.MatOfPoint2f()
            org.opencv.imgproc.Imgproc.approxPolyDP(
                org.opencv.core.MatOfPoint2f(*c.toArray()),
                approx,
                peri * 0.02,
                true
            )
            if (approx.total() == 4L) {
                val pts = approx.toArray()
                if (pts.size == 4) {
                    val quad = DocumentQuad(
                        topLeft = android.graphics.PointF(pts[0].x.toFloat(), pts[0].y.toFloat()),
                        topRight = android.graphics.PointF(pts[1].x.toFloat(), pts[1].y.toFloat()),
                        bottomRight = android.graphics.PointF(pts[2].x.toFloat(), pts[2].y.toFloat()),
                        bottomLeft = android.graphics.PointF(pts[3].x.toFloat(), pts[3].y.toFloat())
                    ).reorderClockwise()
                    if (quad.isReasonable(w, h)) {
                        val area = org.opencv.imgproc.Imgproc.contourArea(approx)
                        if (area > bestScore) {
                            bestScore = area
                            best = quad
                        }
                    }
                }
            }
            approx.release()
        }

        releaseAll(gray, blurred, edged, closed, hierarchy)
        contours.forEach { runCatching { it.release() } }
        return best
    }

    private fun releaseAll(vararg mats: org.opencv.core.Mat) {
        mats.forEach { runCatching { it.release() } }
    }
}
