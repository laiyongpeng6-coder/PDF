package com.scantidy.scan.core.di

import com.scantidy.scan.pdf.convert.PdfJpgConverter
import com.scantidy.scan.pdf.encrypt.PdfEncryptor
import com.scantidy.scan.pdf.merge.PdfFromImages
import com.scantidy.scan.pdf.merge.PdfMerger
import com.scantidy.scan.pdf.signature.SignatureProcessor
import com.scantidy.scan.pdf.watermark.Watermarker
import com.scantidy.scan.scan.detector.DocumentDetector
import com.scantidy.scan.scan.detector.PerspectiveCorrector
import com.scantidy.scan.scan.filter.ImageFilter
import com.scantidy.scan.scan.ocr.MlKitOcrEngine
import com.scantidy.scan.scan.ocr.OcrEngine
import com.scantidy.scan.scan.pipeline.ScanPipeline
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    @Binds
    @Singleton
    abstract fun bindOcrEngine(impl: MlKitOcrEngine): OcrEngine

    companion object {

        // 这些类已用 @Inject + @Singleton 自己提供；不需要 Provides
        // 保留扩展点：未来换 OpenCV 版本 / 加 ML Kit 自定义模型时
        // 在这里写 @Provides 替换

        @Provides
        @Singleton
        fun provideScanPipeline(
            @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
            detector: DocumentDetector,
            corrector: PerspectiveCorrector,
            filter: ImageFilter,
            ocr: OcrEngine
        ): ScanPipeline = ScanPipeline(context, detector, corrector, filter, ocr)
    }
}
