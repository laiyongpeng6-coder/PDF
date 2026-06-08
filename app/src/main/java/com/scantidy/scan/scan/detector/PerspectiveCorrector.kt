package com.scantidy.scan.scan.detector

import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 透视矫正
 * 把四边形 (quad) → 矩形输出图
 */
@Singleton
class PerspectiveCorrector @Inject constructor() {

    /**
     * @param bgr 输入原图（BGR）
     * @param quad 文档四顶点
     * @return 矫正后的图（保持原图 DPI，宽度对齐短边/长边）
     */
    fun correct(bgr: Mat, quad: DocumentQuad): Mat {
        val pts = quad.toOpenCVPoints()
        val (widthA, widthB) = edgeLengths(pts[0], pts[1], pts[3], pts[2])
        val (heightA, heightB) = edgeLengths(pts[0], pts[3], pts[1], pts[2])
        val maxW = max(widthA, widthB).roundToInt().coerceAtLeast(1)
        val maxH = max(heightA, heightB).roundToInt().coerceAtLeast(1)

        val src = org.opencv.core.MatOfPoint2f(*pts)
        val dst = org.opencv.core.MatOfPoint2f(
            Point(0.0, 0.0),
            Point((maxW - 1).toDouble(), 0.0),
            Point((maxW - 1).toDouble(), (maxH - 1).toDouble()),
            Point(0.0, (maxH - 1).toDouble())
        )

        val m = Imgproc.getPerspectiveTransform(src, dst)
        val out = Mat()
        Imgproc.warpPerspective(
            bgr, out, m,
            Size(maxW.toDouble(), maxH.toDouble())
        )
        src.release()
        dst.release()
        m.release()
        return out
    }

    private fun edgeLengths(
        p1: Point, p2: Point,
        p3: Point, p4: Point
    ): Pair<Double, Double> {
        val d1 = distance(p1, p2)
        val d2 = distance(p3, p4)
        return d1 to d2
    }

    private fun distance(a: Point, b: Point): Double {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
