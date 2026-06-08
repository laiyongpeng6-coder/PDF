# 工程实施规划：PDF Scanner & Saver v0.1

> 文档目的：把 PRD v0.1 拆解为可执行的工程任务，给后续实现提供路线图。
> 文档配套：`prd-v0.1.md` / `mvp-scope-local-only.md`
> 规划时间：2026-06-08
> 平台：Android（minSdk 24 / targetSdk 35 / Kotlin 2.2.10 / AGP 9.2.1 / Gradle 9.4.1）

---

## 0. 现状与关键约束

### 0.1 已有资产

| 资产 | 位置 / 状态 |
|------|------------|
| Android SDK | `C:\Users\Administrator\AppData\Local\Android\Sdk`（platforms: 35, 36.1；build-tools: 34.0.0, 36.0.0, 36.1.0, 37.0.0） |
| Gradle wrapper 缓存 | `~/.gradle/wrapper/dists/` 已有 **gradle-8.7-bin / 9.0.0-bin / 9.4.1-bin**（全部下载完整） |
| 现有参考项目 | `C:\Users\Administrator\AndroidStudioProjects\QRSacn`（AGP 9.2.1 + Kotlin 2.2.10 + Compose BOM 2026.02.01 + minSdk 24） |
| 本地 Gradle 依赖缓存 | AGP 8.5.0/9.2.1、Kotlin 2.0.0/2.2.10、Compose BOM、Room 2.6.1、Hilt 2.51.1、CameraX 1.3.4/1.4.1、ML Kit vision-common/barcode、zxing core、Bouncy Castle 1.77/1.79 |

### 0.2 缺失/需要补齐的资产

| 资产 | 状态 | 影响 | 解决 |
|------|------|------|------|
| **JDK 21** | ❌ 未安装（已扫描 `Program Files`、`Program Files (x86)`、`AppData`、用户目录，均无 `java.exe`） | **致命**：Gradle 跑不起来 → 无法 `assembleDebug` → 无法编译 | 由用户安装 Temurin 21 / Zulu 21 / Microsoft OpenJDK 21 任一；`org.gradle.daemon.idletimeout` 不需要 |
| **NDK** | ❌ 未安装 | OpenCV Android SDK 走预编译 .aar（Maven Central）可绕过；走源码则需要 NDK | v0.1 用 OpenCV Maven AAR，不走源码 |
| **OpenCV 4.x Android AAR** | ❌ 未缓存 | 首次构建会下载 ~30 MB | 允许网络下载一次（构建机需要） |
| **PdfBox-Android** | ❌ 未缓存 | 首次构建下载 ~10 MB | 允许网络下载一次 |
| **ML Kit text-recognition（中文 + Latin）** | ❌ 仅 vision-common 已缓存 | OCR 模型 ~30 MB | 首次启动从 Play 服务下载到设备本地；构建只下 ~5 MB AAR |
| **ML Kit document-scanner** | ❌ 未缓存 | 走 OpenCV 兜底可绕过 | **v0.1 不用**，统一用 OpenCV 边检 |
| **Coil / Hilt / Room KSP** | 部分缓存 | KSP 需 `com.google.devtools.ksp` 处理器 | 已缓存，OK |

### 0.3 沙箱与本地的边界

> 当前 WorkBuddy 沙箱**没有 JDK**，无法直接 `gradle assembleDebug` 验证。
> **沙箱能做的**：
> - 写完整源码（Kotlin / XML / 资源 / Gradle 配置）
> - 跑 `./gradlew --offline tasks` 等只查缓存的子命令
> - 跑文档生成、依赖解析
> - 跑 OpenCV / 算法逻辑的纯 JVM 单测（如果 JUnit + Robolectric 都能用）
> **沙箱不能做的**：
> - 跑 Android 编译（AAPT2 / D8 / R8 需要 JDK + 真实 Android SDK 工具）
> - 跑 instrumentation 测试
> - 真机/模拟器验证
>
> **所以本规划的交付物 = "完整可编译的源码 + 构建产物"**，验证步骤在用户本地完成。

---

## 1. 项目目录结构

```
E:\AIPlace\QQAI\
├── docs/                          # 文档（已有）
│   ├── pdf-scanner-market-research.md
│   ├── mvp-scope-local-only.md
│   ├── prd-v0.1.md
│   └── plan/
│       └── engineering-plan.md    # 本文件
├── app/                           # 源码（待生成）
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/scantidy/scan/
│       │   ├── ScanApp.kt                # Application + Hilt
│       │   ├── MainActivity.kt
│       │   ├── core/                     # 工具与基类
│       │   │   ├── di/                   # Hilt Module
│       │   │   ├── fs/                   # 文件路径、FileProvider
│       │   │   ├── time/                 # 时间格式
│       │   │   └── log/                  # 本地日志
│       │   ├── data/                     # 数据层
│       │   │   ├── db/                   # Room Database + DAO + Entity
│       │   │   ├── repository/           # DocumentRepository
│       │   │   └── model/                # 领域模型
│       │   ├── feature/                  # 业务模块（按页面分）
│       │   │   ├── home/                 # 文档首页
│       │   │   ├── camera/               # 拍照扫描
│       │   │   ├── editor/               # 扫描结果编辑（裁剪/旋转/滤镜）
│       │   │   ├── reader/               # PDF 阅读 + 标注
│       │   │   ├── pdfedit/              # PDF 合并/拆分/旋转/删页/调序
│       │   │   ├── tools/                # 工具集合（加密/水印/转换）
│       │   │   ├── link2pdf/             # 链接 → PDF
│       │   │   ├── settings/             # 设置
│       │   │   └── share/                # 分享 / FileProvider
│       │   ├── scan/                     # 扫描核心管线
│       │   │   ├── detector/             # OpenCV 边检/透视
│       │   │   ├── filter/               # 滤镜
│       │   │   ├── ocr/                  # ML Kit OCR 包装
│       │   │   └── pipeline/             # 扫描→OCR→PDF 流水线
│       │   ├── pdf/                      # PDF 能力
│       │   │   ├── core/                 # PdfBox-Android 包装
│       │   │   ├── render/               # PdfRenderer 渲染
│       │   │   ├── merge/                # 合并/拆分/旋转/删页
│       │   │   ├── encrypt/              # 加密
│       │   │   ├── watermark/            # 水印
│       │   │   ├── signature/            # 手写签名
│       │   │   └── convert/              # PDF↔JPG
│       │   ├── web/                      # 链接 → PDF
│       │   │   ├── renderer/             # WebView 整页渲染
│       │   │   └── url/                  # URL 解析 + 校验
│       │   └── ui/                       # UI 通用
│       │       ├── theme/                # Material 3 主题
│       │       ├── components/           # 自定义组件
│       │       └── nav/                  # 导航
│       └── res/
│           ├── values/
│           │   ├── strings.xml
│           │   ├── colors.xml
│           │   └── themes.xml
│           ├── xml/                      # FileProvider / backup
│           └── mipmap-*/                 # 启动图标
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradle.properties
├── settings.gradle.kts
├── build.gradle.kts
├── gradlew
├── gradlew.bat
└── local.properties                # sdk.dir 指向本地 SDK
```

> **包名**：`com.scantidy.scan`（扫描 + tidy 的组合，暂用，待用户最终确认）
> **App 名称**：PDF Scanner & Saver（中文：极扫 / 净扫 / 本本扫描 等暂未定）

---

## 2. 任务分解（按交付顺序）

> 每项任务都有 [Id] 编号 + 标题 + 交付物 + 估时（人日）
> 估时基于中级 Android 工程师；含 50% buffer（首次集成、踩坑）

### Phase A — 项目脚手架（3 人日）

| Id | 任务 | 交付物 | 估时 |
|----|------|--------|:----:|
| A1 | 写 `gradle.properties` / `settings.gradle.kts` / 根 `build.gradle.kts` | 三份配置文件 | 0.5 |
| A2 | 写 `gradle/libs.versions.toml`（版本目录） | 版本目录文件 | 0.5 |
| A3 | 写 `app/build.gradle.kts`（含 KSP、Hilt、Room、CameraX、PdfBox-Android、OpenCV 依赖） | app build 文件 | 1 |
| A4 | 写 `gradle/wrapper/gradle-wrapper.properties`（**复用已缓存的 9.4.1**） | wrapper 配置 | 0.25 |
| A5 | 拷贝 `gradle-wrapper.jar`（从 QRSacn 项目拷贝） | wrapper jar | 0.25 |
| A6 | 拷贝 `gradlew` / `gradlew.bat`（从 QRSacn） | 启动脚本 | 0.25 |
| A7 | 写 `AndroidManifest.xml`（不申请 INTERNET 权限！） | manifest | 0.25 |
| A8 | 写 `local.properties`（`sdk.dir` 指向 `C:\Users\Administrator\AppData\Local\Android\Sdk`） | local properties | 0.1 |
| A9 | 写 Application + MainActivity + Theme + Color + Type 5 个 Compose 主题文件 | 主题文件 | 0.5 |
| A10 | 写 Hilt 基础：ScanApp 注解、AppModule、空 Database | Hilt 骨架 | 0.5 |
| A11 | 在本机执行 `gradlew.bat assembleDebug` 验证 | 编译产物 | 0.5（沙箱无法跑，需要用户在本机跑） |

**Phase A 总计：3 人日**

### Phase B — 数据层（2 人日）

| Id | 任务 | 交付物 | 估时 |
|----|------|--------|:----:|
| B1 | Room Entity：`Document` / `DocumentTag` / `DocumentFolder` | 3 个 entity | 0.5 |
| B2 | Room DAO：CRUD + 全文搜索（FTS4） | DAO 接口 | 0.5 |
| B3 | Room Database + 类型转换器（Instant/List<String>） | Database + Converters | 0.5 |
| B4 | Repository + Flow API | Repository 类 | 0.5 |
| B5 | Hilt Module 注入 | DI | 0.25 |
| B6 | 写一个 Room schema export 验证 | schema 文件 | 0.25 |

**Phase B 总计：2 人日**

### Phase C — 扫描核心管线（8 人日）

| Id | 任务 | 交付物 | 估时 |
|----|------|--------|:----:|
| C1 | CameraX 集成：预览 + 拍照 + 自动捕获 | CameraScreen | 1.5 |
| C2 | OpenCV 初始化 + 边检（自适应阈值 + 轮廓提取） | DocumentDetector | 2 |
| C3 | 透视矫正（4 点 homography） | PerspectiveCorrector | 1 |
| C4 | 阴影去除 + 自适应二值化（增强/灰度/黑白 4 模式） | ImageFilter | 1 |
| C5 | 场景预设（文档/证件/白板：长宽比 + 颜色策略） | ScenePreset | 0.5 |
| C6 | ML Kit OCR 包装：Latin + 中文 on-device | OcrEngine | 1 |
| C7 | 扫描管线：拍照 → 边检 → 透视 → 滤镜 → OCR → 文本层 PDF | ScanPipeline | 1 |

**Phase C 总计：8 人日**

### Phase D — PDF 能力（5 人日）

| Id | 任务 | 交付物 | 估时 |
|----|------|--------|:----:|
| D1 | PdfBox-Android 集成 + 基础读写 | PdfCore | 0.5 |
| D2 | PDF 合并/拆分/旋转/删页/调序 | MergeSplitEditor | 1.5 |
| D3 | PDF 加密（128-bit AES） | Encryptor | 0.5 |
| D4 | 文字/图片水印 | Watermarker | 1 |
| D5 | 手写签名（透明 PNG 叠加到任意页位置） | Signature | 0.5 |
| D6 | PDF ↔ JPG 互转 | PdfJpgConverter | 0.5 |
| D7 | 文本层写入 PDF（让 PDF 搜索可用） | TextLayerWriter | 0.5 |

**Phase D 总计：5 人日**

### Phase E — PDF 阅读 + 标注（4 人日）

| Id | 任务 | 交付物 | 估时 |
|----|------|--------|:----:|
| E1 | PdfRenderer 包装：异步加载 + 缩放 + 双指 | ReaderScreen | 1.5 |
| E2 | 夜间模式 + 搜索 + 目录 | ReaderFeatures | 1 |
| E3 | 高亮 + 涂鸦（自绘 + 序列化到 PDF） | AnnotationLayer | 1.5 |

**Phase E 总计：4 人日**

### Phase F — 链接 → PDF（3 人日）

| Id | 任务 | 交付物 | 估时 |
|----|------|--------|:----:|
| F1 | 链接输入页（URL 校验 + 历史记录） | LinkInputScreen | 0.5 |
| F2 | App 内 WebView 加载（沙箱化：禁用 JS、限制 cookie、屏蔽子资源跨域） | WebViewContainer | 1 |
| F3 | 整页截图 → PDF（Bitmap → PdfDocument） | PageCapture | 1 |
| F4 | 历史 URL 管理（Room 表） | LinkHistory | 0.5 |

**Phase F 总计：3 人日**

### Phase G — UI 整合（5 人日）

| Id | 任务 | 交付物 | 估时 |
|----|------|--------|:----:|
| G1 | 文档首页（列表 + 搜索 + 标签筛选 + FAB） | HomeScreen + ViewModel | 1.5 |
| G2 | 扫描结果编辑页（多页缩略图 + 单页编辑） | EditorScreen | 1 |
| G3 | 工具页（合并/拆分/转换/加密/水印/链接→PDF 入口） | ToolsScreen | 0.5 |
| G4 | 设置页（主题/默认滤镜/OCR 语言/缓存清理/关于） | SettingsScreen | 0.5 |
| G5 | 导航图（Navigation Compose）+ 底部 Tab | NavGraph | 0.5 |
| G6 | 权限请求流程（CAMERA + 媒体读） | PermissionGate | 0.5 |
| G7 | 系统分享 + FileProvider 配置 | ShareHelper | 0.5 |

**Phase G 总计：5 人日**

### Phase H — 打磨与发布（4 人日）

| Id | 任务 | 交付物 | 估时 |
|----|------|--------|:----:|
| H1 | Material 3 暗黑模式适配 + 8dp 网格 + 中文字号 | 主题 | 1 |
| H2 | 错误处理：Toast / Snackbar / 空态 / 加载态 | 全局 | 1 |
| H3 | 本地崩溃日志（写文件，不上传） | LogWriter | 0.5 |
| H4 | 应用图标（adaptive icon） | mipmap | 0.5 |
| H5 | ProGuard / R8 配置 | proguard-rules | 0.5 |
| H6 | 验收用例自测（按 PRD §8.1） | 自测报告 | 0.5 |

**Phase H 总计：4 人日**

### 总计

| Phase | 内容 | 人日 |
|-------|------|:----:|
| A | 脚手架 | 3 |
| B | 数据层 | 2 |
| C | 扫描核心 | 8 |
| D | PDF 能力 | 5 |
| E | 阅读 + 标注 | 4 |
| F | 链接 → PDF | 3 |
| G | UI 整合 | 5 |
| H | 打磨 | 4 |
| **合计** | | **34 人日** ≈ 7 周单人 / 3.5 周双人 |

---

## 3. 关键技术决策记录（ADR）

### ADR-001：用 OpenCV Maven AAR 而不是源码编译

- **状态**：✅ 采纳
- **背景**：OpenCV 4.x Android 走 NDK 源码编译需要 NDK + CMake 工具链，环境更重
- **决策**：`implementation("org.opencv:opencv:4.10.0")` 走 Maven Central
- **后果**：边检 / 透视 / 阴影等核心算法用 OpenCV 的 `Imgproc.findContours` + `Imgproc.warpPerspective` + `photo` 模块

### ADR-002：用 PdfBox-Android 而不是 iText 7

- **状态**：✅ 采纳
- **背景**：iText 7 是 AGPL-3，商业闭源 App 静态链接 iText 必须开源整个 App
- **决策**：`implementation("com.tom-roush:pdfbox-android:2.0.27.0")`（Apache-2.0）
- **后果**：API 比 iText 略繁琐（PDDocument / PDPage / PDPageContentStream），但法务安全

### ADR-003：OCR 用 ML Kit on-device Latin + Chinese

- **状态**：✅ 采纳
- **背景**：ML Kit on-device 文本识别完全离线；模型 ~30 MB 首次启动下载到本地
- **决策**：`com.google.mlkit:text-recognition:16.0.1` (Latin) + `com.google.mlkit:text-recognition-chinese:16.0.1` (中文)
- **后果**：日 / 韩 OCR v0.1 暂不支持（用 Latin 兜底），v0.2 再加 `text-recognition-japanese` / `text-recognition-korean`

### ADR-004：不申请 INTERNET 权限

- **状态**：✅ 采纳
- **背景**：PRD 要求"完全本地"
- **决策**：`AndroidManifest.xml` 不声明 `android.permission.INTERNET`
- **后果**：
  - 优点：保证不会上传任何数据
  - 缺点：ML Kit 模型下载、链接→PDF 抓取**理论上**需要网络 → **必须在 UI 里禁用相关功能 + 给用户清晰提示**
  - 妥协：链接→PDF 实际需要 `WebView.loadUrl()`，**没有 INTERNET 权限会加载失败** → **方案**：链接→PDF v0.1 只在 Manifest 加一个 `uses-feature` 标记 + 检测到无网时给出"此功能需要联网，请在系统设置中临时授权"提示
  - 替代方案 v0.2：用本地 DNS 解析 + 自建 WebViewClient 不发请求（复杂、不推荐）

### ADR-005：链接 → PDF 用 App 内 WebView

- **状态**：✅ 采纳
- **背景**：v0.1 不接任何云端抓取服务
- **决策**：App 内 WebView 加载 URL → 等 onPageFinished → 截整页 Bitmap → 转 PDF
- **后果**：
  - 优点：完全本地
  - 缺点：SPA 复杂页面渲染可能不完整；需要 INTERNET 权限（**与 ADR-004 冲突**）
  - 妥协：v0.1 把"链接 → PDF"作为可选功能，App 启动时检测 `INTERNET` 权限（用户手动开过/关过），有则启用，无则禁用并提示

### ADR-006：Hilt + Room + KSP 组合

- **状态**：✅ 采纳
- **理由**：Hilt 是 Android 官方 DI；Room 必须用 KSP 编译（kapt 慢且已 deprecated）
- **依赖**：`com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.2.10-1.0.27`

### ADR-007：Compose 全栈，不引入 XML 布局

- **状态**：✅ 采纳
- **理由**：现代 Android 推荐；本项目 UI 不复杂，Compose 写起来更快；Material 3 一等公民

### ADR-008：协程 + Flow 异步

- **状态**：✅ 采纳
- **理由**：项目标准；viewModelScope / Dispatchers.IO / StateFlow

### ADR-009：Crash 上传**不做**

- **状态**：✅ 采纳
- **决策**：崩溃日志写本地文件 `files/logs/crash-YYYYMMDD.log`；设置页提供"导出日志"按钮（走系统分享）
- **后果**：无法远程监控崩溃率；用户报问题需要主动拉日志

### ADR-010：i18n v0.1 只出中文

- **状态**：✅ 采纳
- **决策**：`values/strings.xml` 只有中文版；`values-en/strings.xml` v0.2 再加

---

## 4. 风险登记表

| # | 风险 | 概率 | 影响 | 应对 |
|---|------|:----:|:----:|------|
| R1 | JDK 21 用户本机没装，构建失败 | 高 | 高 | 在 README 顶部写明 JDK 21 安装要求 + 给 Adoptium/Temurin 链接 |
| R2 | OpenCV 4.10.0 Maven 仓库访问慢/被墙 | 中 | 中 | 备选：本地放 .aar；或换 OpenCV 4.9.0 |
| R3 | PdfBox-Android 对大 PDF（>200 页）性能差 | 中 | 中 | 文档说明 + 限制单文件 200 页 + 进度条 |
| R4 | ML Kit 中文模型国内下载失败 | 中 | 中 | 设置页加"重试下载模型"按钮；离线识别退化为"仅 Latin" |
| R5 | WebView 链接 → PDF 复杂 SPA 渲染失败 | 中 | 低 | 文档说明"仅保证静态页面"；提供手动选区域截图 |
| R6 | Hilt + KSP 在 AGP 9.2.1 + Kotlin 2.2.10 下偶发编译错误 | 低 | 中 | 锁定 Hilt 2.51.1 + KSP 2.2.10-1.0.27；遇错先 clean + invalidate caches |
| R7 | Compose 1.7 + Material 3 在 minSdk 24 上有兼容问题 | 低 | 中 | minSdk 升到 24 是必须的（CameraX 需要），但避免用 24 一下才有的 API |
| R8 | 链接 → PDF 与"不申请 INTERNET"冲突 | 高 | 中 | 见 ADR-004 妥协方案：检测权限 + 提示用户手动开关 |
| R9 | 沙箱里 Gradle 跑不起来，源码可能在用户本机首次构建失败 | 高 | 高 | 写一份 `BUILD.md` 详细列出前置依赖；`assembleDebug` 失败的 debug 步骤 |
| R10 | 项目工期 34 人日，用户期望更短 | 中 | 中 | 给出 Phase 1 优先版（17 人日）作为 v0.1-rc |

---

## 5. 里程碑（按 Phase 推进）

| 里程碑 | 范围 | 交付物 | 状态 |
|--------|------|--------|:----:|
| **M0**：方案确认 | PRD v0.1 + 本规划 | 本文件 + 3 份 docs | ✅ |
| **M1**：项目脚手架可跑 | Phase A | 空白 Compose App + 编译通过 | 🟡 准备开工 |
| **M2**：数据 + 扫描核心 | Phase B + C | 拍照 → OCR → PDF 完整流水线 | ⬜ |
| **M3**：PDF 全套能力 | Phase D | 合并/拆分/加密/水印/签名/转换 | ⬜ |
| **M4**：阅读 + 链接 → PDF | Phase E + F | 完整 PDF 阅读 + 链接下载 | ⬜ |
| **M5**：UI 整合 | Phase G | 所有页面 + 导航 + 分享 | ⬜ |
| **M6**：发布候选 | Phase H | v0.1.0-rc | ⬜ |

---

## 6. 立即行动（下一步）

按 Phase A 顺序推进，先把项目骨架 + 编译跑通：

1. 写 `gradle.properties` / `settings.gradle.kts` / 根 `build.gradle.kts` / `gradle/libs.versions.toml`
2. 写 `app/build.gradle.kts`
3. 写 `AndroidManifest.xml`（**不申请 INTERNET**）
4. 写 `local.properties`
5. 拷贝 `gradle-wrapper.jar` + `gradlew` + `gradlew.bat`（从 QRSacn）
6. 写 Application + MainActivity + Theme + Color + Type
7. 写 Hilt 基础（ScanApp + AppModule）
8. 写 `BUILD.md` 告知用户如何在本机构建

> 由于沙箱无 JDK，**M1 的"编译通过"由用户在本机执行 `gradlew.bat assembleDebug` 完成**。
> 沙箱里我提供完整源码 + 一份"用户操作手册"。

---

## 7. 文档清单

| 文档 | 路径 | 状态 |
|------|------|:----:|
| 竞品调研 | `docs/pdf-scanner-market-research.md` | ✅ |
| MVP 范围（本地化筛选） | `docs/mvp-scope-local-only.md` | ✅ |
| PRD v0.1 | `docs/prd-v0.1.md` | ✅ |
| 工程实施规划 | `docs/plan/engineering-plan.md` | ✅ 本文件 |
| 用户构建手册 | `BUILD.md` | ⬜ M1 时写 |
| 隐私政策 | `PRIVACY.md` | ⬜ M1 时写（"本 App 不收集任何数据"） |
| 更新日志 | `CHANGELOG.md` | ⬜ M6 时写 |
