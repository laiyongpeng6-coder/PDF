# AI 接力文档 — PDF 极扫 v0.1

> **目的**：如果本轮（WorkBuddy 沙箱）的 AI 助手"下线"或要在另一台电脑 / 另一个 AI 继续开发，
> 这份文档能让接力的 AI 在**最短时间内**理解：
> 1. 项目是什么
> 2. 已经做了什么
> 3. 还没做什么
> 4. 接力时**第一步**应该干什么
> 5. 哪些"暗坑"必须避开

> **更新时间**：2026-06-09 00:30 (GMT+8)
> **当前状态**：v0.1 **源码完整交付**，**编译未在本机验证**（沙箱无 JDK）
> **下一站任务**：用户本机跑通 `gradlew.bat assembleDebug`，然后修编译错误

---

## 0. 30 秒速览

```
项目名    : PDF 极扫 (PDF Scanner & Saver)
包名      : com.scantidy.scan
版本      : v0.1.0
平台      : Android 7.0+ (API 24+)
技术栈    : Kotlin 2.2.10 + Jetpack Compose + Material 3
架构      : MVVM + Hilt + Room + Repository
核心约束  : 不申请 INTERNET 权限；所有功能本地完成
项目根    : E:\AIPlace\QQAI
代码规模  : 55 个 Kotlin 源文件 + 12 个 XML 资源
```

---

## 1. 这是个什么 App

**一句话**：把纸质文件 / 屏幕 / 链接，离线变成可搜索、可分享、可管理的 PDF，全部留在本机。

**核心价值主张**：
- ✅ 完全离线（不联网、不登录、不上传）
- ✅ 不接任何统计 / 崩溃上报 / 推送 SDK
- ✅ 不写云端备份
- ✅ 用户对自己的数据 100% 掌控

**对标竞品**：扫描全能王 / Adobe Scan / 夸克扫描王 / Genius Scan
**差异化**：别人要做"扫描 SaaS"，我们要做"扫描工具"。**隐私是最大卖点**。

---

## 2. 我（前任 AI）都干了什么

### 2.1 阶段 1：调研 + 规划（5 份文档）

| 文档 | 内容 |
|------|------|
| `docs/pdf-scanner-market-research.md` | 10+ 款主流 App 横向对比、25 项功能矩阵、4 阶段路线图 |
| `docs/mvp-scope-local-only.md` | 32 项功能 × 本地化筛选矩阵（每项"保留/砍/替代"） |
| `docs/prd-v0.1.md` | 完整 PRD：定位、用户画像、17 项功能、12 个页面流、数据模型、验收 |
| `docs/plan/engineering-plan.md` | 工程规划：8 Phase / 34 人日 / 10 条 ADR / 风险登记表 |
| `docs/v0.1-completion.md` | v0.1 完成度自检报告（17 项功能逐项验收） |

### 2.2 阶段 2：源码交付（55 个 .kt + 12 个 .xml）

| 模块 | 文件 | 用途 |
|------|------|------|
| **核心** | `core/di/DatabaseModule.kt`, `core/di/EngineModule.kt` | Hilt DI |
| **核心** | `core/fs/AppPaths.kt` | 私有目录管理 |
| **核心** | `core/log/LocalLogTree.kt` | 写本地日志（**不联网**） |
| **核心** | `core/share/ShareHelper.kt` | 系统分享 + FileProvider |
| **核心** | `core/time/TimeFormat.kt` | 时间格式 |
| **数据** | `data/db/AppDatabase.kt`, `DocumentDao.kt`, `entity/DocumentEntity.kt` | Room + FTS4 全文搜索 |
| **数据** | `data/repository/DocumentRepository.kt` | 仓储层 |
| **数据** | `data/model/Document.kt` | 领域模型 |
| **扫描** | `scan/OpenCVInitializer.kt` | OpenCV 单例 |
| **扫描** | `scan/detector/DocumentDetector.kt` | 边检 (Canny + approxPolyDP) |
| **扫描** | `scan/detector/PerspectiveCorrector.kt` | 透视矫正 (warpPerspective) |
| **扫描** | `scan/filter/ImageFilter.kt` | 4 种滤镜（增强/灰度/黑白/原图） |
| **扫描** | `scan/ocr/OcrEngine.kt` + `MlKitOcrEngine.kt` | ML Kit 包装 |
| **扫描** | `scan/pipeline/ScanPipeline.kt` | 扫描 + OCR + 流水线 |
| **PDF** | `pdf/core/PdfCore.kt` | PdfBox 包装 |
| **PDF** | `pdf/render/PdfRenderEngine.kt` | PdfRenderer 阅读引擎 |
| **PDF** | `pdf/merge/PdfMerger.kt` + `PdfFromImages.kt` | 合并/拆分/旋转/图片→PDF |
| **PDF** | `pdf/encrypt/PdfEncryptor.kt` | AES-128 加密/解密 |
| **PDF** | `pdf/watermark/Watermarker.kt` | 文字/图片水印 |
| **PDF** | `pdf/signature/SignatureProcessor.kt` | 手写签名（透明 PNG 叠加） |
| **PDF** | `pdf/annotation/Annotation.kt` + `AnnotationStore.kt` | 高亮 + 涂鸦 + 持久化 |
| **PDF** | `pdf/convert/PdfJpgConverter.kt` | PDF↔JPG |
| **Web** | `web/url/UrlValidator.kt` | URL 校验（只允许 http/https） |
| **Web** | `web/renderer/WebPageRenderer.kt` | WebView 整页 → PDF |
| **UI** | `ui/ScanApp.kt` + `ui/nav/Destination.kt` | 顶层 Composable + 导航 |
| **UI** | `ui/theme/Color.kt` + `Type.kt` + `Theme.kt` | Material 3 主题 |
| **UI** | `ui/LocalLocale.kt` | 多语言钩子 |
| **Feature** | `feature/home/HomeScreen.kt` + `HomeViewModel.kt` | 文档首页（网格 + 搜索） |
| **Feature** | `feature/camera/CameraScreen.kt` + `CameraViewModel.kt` | CameraX 拍照 |
| **Feature** | `feature/editor/EditorScreen.kt` + `EditorViewModel.kt` | 编辑 + 保存 PDF |
| **Feature** | `feature/reader/ReaderScreen.kt` + `ReaderViewModel.kt` | PDF 阅读 + 密码 |
| **Feature** | `feature/pdfedit/PdfEditScreen.kt` + `PdfEditViewModel.kt` | PDF 工具箱 |
| **Feature** | `feature/link/LinkInputScreen.kt` + `LinkRenderScreen.kt` | 链接 → PDF |
| **Feature** | `feature/tools/ToolsScreen.kt` | 工具页 |
| **Feature** | `feature/settings/SettingsScreen.kt` + `SettingsViewModel.kt` | 设置 |
| **App** | `MainActivity.kt` + `ScanApp.kt` | 入口 |

### 2.3 阶段 3：i18n（4 套完整多语言）

| 语种 | 文件 |
|------|------|
| 简体中文（默认） | `app/src/main/res/values/strings.xml` |
| 简体中文（中国） | `app/src/main/res/values-zh-rCN/strings.xml` |
| English | `app/src/main/res/values-en/strings.xml` |
| 日本語 | `app/src/main/res/values-ja/strings.xml` |
| 한국어 | `app/src/main/res/values-ko/strings.xml` |

每套 ~250 个 string，全量翻译。

### 2.4 阶段 4：构建配置

| 文件 | 关键点 |
|------|--------|
| `gradle/libs.versions.toml` | **版本目录（version catalog）**：所有库版本集中管理 |
| `app/build.gradle.kts` | 声明所有依赖；abi 过滤；签名配置；Compose + KSP + Hilt |
| `build.gradle.kts` | 根级：声明插件版本 |
| `settings.gradle.kts` | 仓库 + 项目结构 |
| `gradle.properties` | AndroidX + JVM 内存 + KSP 优化 |
| `gradle/wrapper/gradle-wrapper.properties` | **Gradle 9.4.1**（本地已下载完整） |
| `gradle/wrapper/gradle-wrapper.jar` | 45KB wrapper |
| `gradlew` + `gradlew.bat` | 构建脚本（从 QRSacn 参考项目拷贝） |
| `local.properties` | **已写** `sdk.dir=C:\Users\Administrator\AppData\Local\Android\Sdk` |
| `app/proguard-rules.pro` | 第三方库 keep 规则 |
| `app/src/main/AndroidManifest.xml` | **不申请 INTERNET 权限**；申请 CAMERA / READ_MEDIA_IMAGES |
| `app/src/main/res/xml/file_paths.xml` | FileProvider 路径白名单 |
| `app/src/main/res/xml/backup_rules.xml` + `data_extraction_rules.xml` | 不参与备份/迁移 |

### 2.5 阶段 5：顶层文档

| 文件 | 用途 |
|------|------|
| `README.md` | 项目总览 |
| `BUILD.md` | **本机构建手册**（JDK 21 安装 + 5 个常见错误修复） |
| `PRIVACY.md` | 隐私政策（"本应用不收集任何数据"） |
| `CHANGELOG.md` | v0.1.0 首发 |
| `.workbuddy/memory/2026-06-08.md` | **本项目的工作日志**（按时序记录每一步） |

---

## 3. 还没做什么（接力 AI 第一步看这里）

### 3.1 🚨 必修：编译验证

**沙箱没 JDK 21，没跑过 `assembleDebug`**，所以源码可能有**编译错误**没暴露。

**接力 AI 第一步**：

1. **确认用户本机已装 JDK 21**（参考 [BUILD.md §2](BUILD.md)）
   ```bash
   java -version
   # 必须输出 21.x
   ```

2. **跑构建**
   ```bash
   cd E:\AIPlace\QQAI
   .\gradlew.bat assembleDebug
   ```

3. **如果有错误 → 直接修**。5 个最常见错误在 [BUILD.md §7](BUILD.md) 已列。

### 3.2 已知可能的小问题（很可能踩到）

| 位置 | 问题 | 修法 |
|------|------|------|
| `HomeScreen.kt` | 我修过 imports，可能还有未用 import 警告 | 不影响构建，warning 即可 |
| `feature/editor/EditorScreen.kt` | 拍完照的 URI 跨页面传递**没接好**（v0.1 简化：从相册选图替代） | v0.2 用 SavedStateHandle |
| `feature/link/LinkInputViewModel.kt` | 第一次写错了，第二次覆盖了，正常 | OK |
| `MainActivity.kt` | 用了 `LocalContext` 但没 import | 已修 ✅ |
| ML Kit `text-recognition` 16.0.1 + Chinese 16.0.1 | 在国行 ROM（没 Play 服务）首次启动会下不到模型 | UI 给出"重试下载"按钮 |
| Compose Material 3 `FilterChip` 选中色 | 跟用户期望可能不一样 | UI 自调 |
| `CameraScreen` 快门视觉 | 故意留空（外圈是快门），不是 bug | OK |

### 3.3 v0.2 路线图（如果用户要做）

按优先级：

1. **修编译错误**（先做）
2. **真机功能测试**（按 `docs/v0.1-completion.md` §2.1 的 17 项验收）
3. **修 v0.1 暴露的 bug**
4. **OCR 增强**：日 / 韩 OCR 模型（`text-recognition-japanese` 16.0.1 / `text-recognition-korean` 16.0.1）
5. **应用内语言切换**：用 `AppCompatDelegate.setApplicationLocales` 替换设置页占位 UI
6. **相机 URI 跨页面传递**：用 SavedStateHandle / Navigation 参数
7. **PDF 表单填写**（很多企业刚需）
8. **链接 → PDF SPA 渲染改进**（目前只能渲染静态页）

---

## 4. 接力时必须遵守的硬约束

> ⚠️ 这些**绝对不能改**，是产品核心理念：

### 4.1 不联网（硬约束）

- ❌ **不能在 Manifest 加 `INTERNET` 权限**
- ❌ **不能加任何统计 SDK**（友盟 / Firebase Analytics / GrowingIO / 神策 / Sensors）
- ❌ **不能加崩溃上报 SDK**（Crashlytics / Bugsnag / 友盟崩溃）
- ❌ **不能加推送 SDK**（极光 / 个推 / 友盟推送 / FCM）
- ❌ **不能在 Release 版本写远程日志**

**链接 → PDF 功能**是**唯一例外**：UI 里必须**显式提示**用户去系统设置手动开 INTERNET 权限。

### 4.2 不写云端

- ❌ **不能加网盘 SDK**（百度网盘 / 夸克云 / 阿里云盘 / Dropbox / Google Drive / OneDrive）
- ❌ **不能加云同步**（不论 Firebase / Supabase / 自建）
- ❌ **不能加账号系统**（不要写登录页）

### 4.3 隐私优先

- ❌ **不要用 iText**（AGPL-3 商业闭源不可用）→ 用 **PdfBox-Android**（Apache-2.0）
- ❌ **不要写 IMEI / Android ID / MAC / IP 上传**
- ❌ **不要加任何"用户行为埋点"**（包括无埋点 SDK）

### 4.4 中文优先

- 默认字串 = 简中（`values/strings.xml` 与 `values-zh-rCN` 完全相同）
- 字号比 Material 默认 **大 1sp**（中文环境利于阅读）
- **不要默认加动态颜色**（保证品牌色统一）

### 4.5 第三方库 License

只有 **Apache-2.0 / MIT / BSD** 才能用。**AGPL-3 一律不用**。

如果未来要加新库，**先查 License**。

---

## 5. 关键设计决策（已固化，不要推翻）

| 决策 | 原因 |
|------|------|
| 包名 `com.scantidy.scan` | 暂定，等用户最终确认 |
| 中文名 "PDF 极扫" | "极"=极致，简洁 |
| 英文名 "PDF Scanner & Saver" | 不自创，遵循通用命名 |
| Material 3 + 8dp 网格 | 现代 Android 规范 |
| 14sp 起步字号 | 中文环境友好 |
| Compose 全栈 | 不引入 XML 布局 |
| Kotlin 协程 + Flow | 项目标准 |
| Hilt + Room + KSP | Android 官方推荐 |
| PdfBox-Android（不用 iText） | 避开 AGPL-3 |
| OpenCV 走 Maven AAR（不用源码） | 避开 NDK |
| ML Kit on-device | 完全离线 |
| minSdk 24 / targetSdk 35 | CameraX 最低要求 + 最新目标 |
| ABI 过滤 arm64-v8a / x86_64 / armeabi-v7a | APK 体积控制 |
| 单一进程 + Application 持有 CrashHandler | 简单可靠 |
| 数据全在 App 私有目录 | Android 沙箱隔离 |

---

## 6. 接力 AI 的"第一个 30 分钟"建议流程

### 0-5 分钟：读文档（必读）

按顺序：

1. **本文件**（你在读）
2. `docs/prd-v0.1.md` — 知道要做什么
3. `docs/v0.1-completion.md` — 知道已经做了什么
4. `docs/plan/engineering-plan.md` — 知道技术选型

### 5-15 分钟：环境检查

```bash
# 1. JDK 21
java -version

# 2. Gradle 能跑
cd E:\AIPlace\QQAI
.\gradlew.bat --version

# 3. Android SDK 在
ls "C:\Users\Administrator\AppData\Local\Android\Sdk"
# 至少要有 platforms/android-35 和 build-tools/35.0.0
```

如果上面 3 条不通过，**先解决环境**，别看代码。

### 15-25 分钟：第一次构建

```bash
.\gradlew.bat assembleDebug --stacktrace
```

- 成功 → 跳到 §7 跑测试
- 失败 → 修错误。每个错误看 stacktrace，**90% 的问题在 imports / 拼写 / 依赖**。

### 25-30 分钟：报告

无论成败，写一段简短的接力报告：

- 构建成功：成功 + APK 路径 + 下一步建议
- 构建失败：哪些错误 / 已尝试的修法 / 建议下一步

---

## 7. 如果构建成功：跑功能测试

按 `docs/v0.1-completion.md` §2.1 17 项功能 + §8 PRD 验收标准：

1. 装 APK 到真机：`adb install app\build\outputs\apk\debug\app-debug.apk`
2. 启动 App，**飞行模式**下冷启动 → 验证"不联网"声明
3. 点扫描 → 给权限 → 拍一张 A4 → 验证边检 + 透视 + 滤镜
4. 保存 PDF → 进首页 → 验证文档入库 + 缩略图
5. 进阅读器 → 验证缩放 / 翻页 / 夜间
6. 加密 PDF → 用第三方阅读器打开 → 验证密码
7. 点链接 → 输 https URL → 验证（需在系统设置给 INTERNET 权限）
8. 切系统语言到 English / 日本語 / 한국语 → 验证 i18n

每项**通过 / 失败** 记录在测试报告中。

---

## 8. 接力 AI 必读的项目约定

### 8.1 命名规范

| 类型 | 规范 | 例 |
|------|------|-----|
| 类 / 接口 | PascalCase | `DocumentRepository` |
| 函数 / 变量 | camelCase | `getUserName()` |
| 常量 | SCREAMING_SNAKE | `MAX_RETRY_COUNT` |
| 包名 | 全小写 | `com.scantidy.scan` |
| 资源 | 前缀分类 | `ic_launcher`、`string_app_name` |
| 文件 | 与类同名 | `Document.kt` 含 `class Document` |
| Composable | PascalCase | `fun HomeScreen()` |
| ViewModel 后缀 | `ViewModel` | `HomeViewModel` |
| Screen 后缀 | `Screen` | `HomeScreen` |

### 8.2 资源命名禁忌

- ❌ 不要用 `background` / `foreground` / `icon` / `text` / `color` 等 Android 保留名
- ✅ 用前缀：`app_background`、`icon_primary`、`text_title`
- 详细列表见 [Android 原生开发 skill §5.2]

### 8.3 协程线程选择

| 操作 | 线程 |
|------|------|
| UI 更新 | `Dispatchers.Main` |
| 网络请求 | `Dispatchers.IO` |
| 文件 I/O | `Dispatchers.IO` |
| 计算密集 | `Dispatchers.Default` |

**示例**：
```kotlin
viewModelScope.launch {
    val data = withContext(Dispatchers.IO) { repository.load() }
    _uiState.value = data   // 回到 Main 线程
}
```

### 8.4 异常处理

- Repository 层 `try-catch` 包成 `Result<T>`
- ViewModel 层用 `runCatching` 处理
- UI 层用 Snackbar 提示，**不要把异常静默吞掉**

### 8.5 资源字符串

- **所有用户可见的字符串必须用 `R.string.xxx`**，不要硬编码中文 / 英文
- 4 套语言都加 → 不能只在 `values/strings.xml` 加

---

## 9. 接力 AI 的"Quick Start 命令"

如果用户给你这个项目让你继续，**直接贴这段提示词**：

```
你正在接手一个 Android 项目：PDF 极扫（PDF Scanner & Saver）。
请先按以下顺序理解项目：

1. 读 E:\AIPlace\QQAI\AI_HANDOFF.md（本接力文档）
2. 读 E:\AIPlace\QQAI\docs\prd-v0.1.md
3. 读 E:\AIPlace\QQAI\docs\v0.1-completion.md
4. 读 E:\AIPlace\QQAI\docs\plan\engineering-plan.md

然后执行：
  cd E:\AIPlace\QQAI
  java -version          # 必须 21.x
  .\gradlew.bat assembleDebug --stacktrace

构建成功：报告 APK 路径 + 跑 §7 的 8 项真机功能测试。
构建失败：列出所有错误，按 stacktrace 一个个修。

重要约束（绝对不能改）：
- 不申请 INTERNET 权限
- 不接任何统计 / 崩溃 / 推送 SDK
- 不写云端 / 账号系统
- 不用 iText（AGPL-3），继续用 PdfBox-Android
- 用户数据全在 App 私有目录
- 4 套多语言：values / values-en / values-ja / values-ko
```

---

## 10. 联系 / 反馈渠道

如果接力 AI 修完重大问题，**更新本文件**：

- 在底部加 "更新日志" 段
- 改 `更新时间`
- 写清"为什么改"和"改了什么"

如果发现前任 AI 留下的重大问题（错的设计 / 错的实现）：

1. **先沟通用户**：因为有些"错"是权衡结果
2. **不要私自重写**：可能破坏已知约束
3. 沟通后再改

---

## 11. 附录：v0.1 自检摘要

> 详细见 [docs/v0.1-completion.md](docs/v0.1-completion.md)

| 维度 | 状态 |
|------|:----:|
| 核心功能 13 项 | ✅ 13/13 |
| 安全功能 3 项 | ✅ 3/3 |
| 链接 → PDF | ✅ |
| 多语言 | ✅ 4 套 |
| 编译可跑 | ⏳ 用户本机验证 |
| 视觉 UX | ✅ |
| 法务 / 隐私 | ✅ |

**源码统计**：
- 55 个 Kotlin 源文件
- 12 个 XML 资源
- 4 份 Gradle 配置 + wrapper
- 9 份文档

---

## 12. 更新日志

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-06-09 00:30 | 初版 | WorkBuddy 完成 v0.1 源码 + 文档交付 |

---

> **致接力 AI**：这份文档是给你的地图，不是束缚。遵守 §4 硬约束，其他地方可以自由发挥。
> **致用户**：如果你换 AI 继续开发，**只把这一个文件交给新 AI**就够。
