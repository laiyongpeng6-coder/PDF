# PDF 极扫 (PDF Scanner & Saver)

> 一款完全本地离线工作的 Android PDF 工具。
> 把纸质文件 / 屏幕 / 链接，离线变成可搜索、可分享、可管理的 PDF，全部留在本机。

**版本**：v0.1.0
**平台**：Android 7.0+ (API 24+)
**包名**：`com.scantidy.scan`
**License**：Apache-2.0

---

## 核心特性

- 📷 **拍照扫描**：自动边检 + 透视矫正 + 阴影去除；4 种滤镜（原图 / 增强 / 灰度 / 黑白）
- 📄 **多页 PDF**：一次扫描多页 → 合并为单份 PDF
- 🔤 **本地 OCR**：中英日韩 4 种文字识别；完全离线，模型下载到本机
- 🔍 **PDF 可搜索**：OCR 文本写入 PDF 文本层，第三方阅读器可 Ctrl+F 搜索
- 📚 **PDF 阅读**：缩放、夜间、翻页、搜索
- ✏️ **PDF 标注**：高亮 + 涂鸦 + 手写签名
- 🔧 **PDF 工具箱**：合并、拆分、旋转、删页、调序、加密、加密、水印、PDF↔JPG
- 🔗 **链接 → PDF**：粘贴 https 链接，App 内 WebView 渲染为 PDF
- 🏷️ **文档管理**：文件夹、标签、文件名 + 内容 全文搜索
- 🔐 **隐私优先**：**不申请 INTERNET 权限**；不接入任何统计 / 崩溃上报 / 推送 SDK
- 🌍 **多语言**：简体中文、English、日本語、한국어

---

## 快速开始

```bash
# 1. 装 JDK 21
# 2. 装 Android Studio + Android SDK 35
# 3. 在项目根目录跑
.\gradlew.bat assembleDebug

# 4. 装到设备
.\gradlew.bat installDebug
```

详细步骤见 [BUILD.md](./BUILD.md)。

---

## 文档

| 文档 | 说明 |
|------|------|
| [BUILD.md](./BUILD.md) | 本机构建手册 |
| [PRIVACY.md](./PRIVACY.md) | 隐私政策（"本应用不收集任何数据"） |
| [docs/pdf-scanner-market-research.md](./docs/pdf-scanner-market-research.md) | 竞品功能调研 |
| [docs/mvp-scope-local-only.md](./docs/mvp-scope-local-only.md) | MVP 范围（本地化筛选矩阵） |
| [docs/prd-v0.1.md](./docs/prd-v0.1.md) | 产品需求文档 v0.1 |
| [docs/plan/engineering-plan.md](./docs/plan/engineering-plan.md) | 工程实施规划（8 Phase / 34 人日） |
| [CHANGELOG.md](./CHANGELOG.md) | 更新日志 |
| [docs/v0.1-completion.md](./docs/v0.1-completion.md) | v0.1 完成度自检报告 |

---

## 技术栈

- **语言**：Kotlin 2.2.10
- **构建**：Gradle 9.4.1 + AGP 9.2.1
- **UI**：Jetpack Compose + Material 3（Material You 动态色可选）
- **架构**：Hilt + MVVM + Repository + Flow
- **数据**：Room 2.6.1（SQLite + FTS4 全文检索）
- **相机**：CameraX 1.4.1
- **OCR**：ML Kit text-recognition（on-device）
- **图像处理**：OpenCV 4.10.0（边检 / 透视 / 滤镜）
- **PDF**：PdfBox-Android 2.0.27.0（Apache-2.0，规避 iText AGPL）
- **链接渲染**：Android WebView + PrintedPdfDocument

---

## 项目结构

```
app/src/main/java/com/scantidy/scan/
├── MainActivity.kt
├── ScanApp.kt                # Application + Hilt
├── core/
│   ├── di/                   # Hilt Modules
│   ├── fs/                   # AppPaths（私有目录管理）
│   ├── time/                 # 时间格式
│   ├── log/                  # 本地日志（不上传）
│   └── share/                # 系统分享
├── data/
│   ├── db/                   # Room Database + DAO + Entity
│   ├── model/                # 领域模型
│   └── repository/           # DocumentRepository
├── scan/
│   ├── detector/             # OpenCV 边检 + 透视矫正
│   ├── filter/               # 4 种滤镜
│   ├── ocr/                  # ML Kit OCR
│   └── pipeline/             # 扫描 → OCR → 文本层 PDF
├── pdf/
│   ├── core/                 # PdfBox 包装
│   ├── render/               # PdfRenderer 阅读引擎
│   ├── merge/                # 合并/拆分/旋转/删页/调序
│   ├── encrypt/              # AES-128 加密/解密
│   ├── watermark/            # 文字/图片水印
│   ├── signature/            # 手写签名（透明 PNG）
│   ├── annotation/           # 标注（高亮/涂鸦）
│   └── convert/              # PDF↔JPG
├── web/
│   ├── url/                  # URL 校验
│   └── renderer/             # WebView 整页截图 → PDF
└── ui/
    ├── theme/                # Material 3 主题
    ├── nav/                  # 导航目的地
    └── ScanApp.kt            # 顶层 Composable
    └── feature/              # 各页面
        ├── home/
        ├── camera/
        ├── editor/
        ├── reader/
        ├── pdfedit/
        ├── tools/
        ├── link/
        └── settings/
```

---

## 法务声明

### 第三方库 License

| 库 | License |
|----|---------|
| AndroidX | Apache-2.0 |
| Hilt | Apache-2.0 |
| Room | Apache-2.0 |
| CameraX | Apache-2.0 |
| ML Kit | Apache-2.0（按 Google ML Kit Terms） |
| OpenCV | Apache-2.0 |
| PdfBox-Android | Apache-2.0 |
| Compose / Material 3 | Apache-2.0 |
| Coil | Apache-2.0 |
| Timber | Apache-2.0 |

**重要**：本项目**不使用** iText 7（AGPL-3）。PdfBox-Android 是 Apache-2.0，可放心商用闭源。

### 商标

- Android, Google Play, Google ML Kit 均为 Google LLC 商标
- "PDF" 是 Adobe Systems 的商标，本项目作为通用名词使用
- 应用图标 / 名称为自创

---

## 路线图

- **v0.1**（当前）：核心扫描 + OCR + PDF 全套基础 + 链接 → PDF
- **v0.2**：去手写、公式识别、拍照翻译（需要 LLM API）
- **v1.0**：表格还原、试卷对比模式、AI 助手
- **v2.0**：多设备 P2P 同步、团队协作

---

## 贡献

当前 v0.1 由 WorkBuddy 协助开发。如有问题反馈、欢迎提交 Issue / PR。

---

## License

本项目以 Apache-2.0 发布。
