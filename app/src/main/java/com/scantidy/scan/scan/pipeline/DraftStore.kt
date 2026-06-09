package com.scantidy.scan.scan.pipeline

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 拍摄 → 编辑的 URI 中转站
 *
 * CameraViewModel 在导航到 Editor 后会被销毁，
 * 所以需要一个 Singleton 临时保存 draftId → URIs 的映射。
 * EditorViewModel 初始化时从中读取，读完后清除。
 */
@Singleton
class DraftStore @Inject constructor() {

    private val map = mutableMapOf<String, List<Uri>>()

    fun save(draftId: String, uris: List<Uri>) {
        map[draftId] = uris
    }

    fun take(draftId: String): List<Uri> {
        return map.remove(draftId) ?: emptyList()
    }

    fun clear() = map.clear()
}
