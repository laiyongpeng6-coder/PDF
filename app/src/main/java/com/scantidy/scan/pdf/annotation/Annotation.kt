package com.scantidy.scan.pdf.annotation

/**
 * 标注类型
 */
enum class AnnotationType {
    HIGHLIGHT,   // 半透明高亮
    SCRIBBLE     // 自由涂鸦（笔迹）
}

/**
 * 标注（PDF 坐标，0-1 归一化）
 *  - x, y 是矩形左上角（0-1）
 *  - w, h 是宽高（0-1）
 *  - scribblePath 仅在 SCRIBBLE 时用
 */
data class Annotation(
    val type: AnnotationType,
    val pageIndex: Int,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val color: Int = 0xFFFFEB3B.toInt(),  // 黄色
    val alpha: Float = 0.4f,
    val strokeWidth: Float = 0.005f,     // 相对页面宽度
    val scribblePath: List<Pair<Float, Float>> = emptyList()  // 0-1 坐标
)

/**
 * 一份 PDF 的全部标注
 */
data class AnnotationSet(
    val documentId: String,
    val annotations: List<Annotation>
) {
    fun forPage(index: Int): List<Annotation> = annotations.filter { it.pageIndex == index }
}
