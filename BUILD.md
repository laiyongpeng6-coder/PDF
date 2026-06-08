# BUILD.md — PDF Scanner & Saver 构建手册

> 本文档指导你**在本机构建并运行**这个 Android App。
> 由于开发环境（沙箱）没有 JDK，构建动作必须由你在本地完成。

---

## 1. 前置依赖

| 依赖 | 版本要求 | 用途 | 安装方式 |
|------|----------|------|----------|
| **JDK 21** | 21.x | 编译 Kotlin + Android | <https://adoptium.net/temurin/releases/?version=21> |
| **Android Studio** | Koala (2024.1.1) 或更新 | 集成开发 | <https://developer.android.com/studio> |
| **Android SDK** | API 35 + Build Tools 35.0.0 | 编译目标 | AS 自带 SDK Manager |
| **Gradle** | 9.4.1（项目自带 wrapper） | 构建 | 项目自带 |

> **JDK 一定要装 Temurin 21**（不要用 JDK 8/11/17，AGP 9.x 不支持）。
> 装完后**设置 `JAVA_HOME`** 环境变量，并在 `PATH` 中加上 `%JAVA_HOME%\bin`。

---

## 2. 验证 JDK

打开 PowerShell / Git Bash：

```bash
java -version
# 应输出：openjdk version "21.0.x" ...

echo $JAVA_HOME
# 应输出 JDK 21 安装路径，例如：
# C:\Program Files\Eclipse Adoptium\jdk-21.0.4.7-hotspot\
```

如果 `java -version` 不存在：
1. 重新打开 PowerShell（让新环境变量生效）
2. 检查 `%JAVA_HOME%\bin\java.exe` 是不是存在

---

## 3. 验证 Gradle 能跑

```bash
cd E:\AIPlace\QQAI
.\gradlew.bat --version
# 应输出 Gradle 9.4.1 + JDK 21
```

---

## 4. 在 Android Studio 中打开

1. 启动 Android Studio
2. **File → Open** → 选择 `E:\AIPlace\QQAI` 目录
3. 等待 Gradle 同步完成（**首次会下载很多依赖**，约 5-10 分钟）
4. 选择设备：连接真机 或 启动模拟器（API 24+）
5. 点击 **Run ▶** 按钮

---

## 5. 命令行构建

```bash
cd E:\AIPlace\QQAI

# Debug APK
.\gradlew.bat assembleDebug

# Release APK（需配置签名）
.\gradlew.bat assembleRelease

# 安装到连接的设备
.\gradlew.bat installDebug

# 清理
.\gradlew.bat clean
```

构建产物路径：

```
app\build\outputs\apk\debug\app-debug.apk
```

---

## 6. 首次启动会下载什么？

| 资源 | 大小 | 时机 | 备注 |
|------|------|------|------|
| AGP 9.2.1 + Kotlin 2.2.10 + Compose | ~50 MB | 首次构建 | Gradle 缓存，二次构建秒过 |
| Hilt + Room + KSP | ~10 MB | 首次构建 | 同上 |
| CameraX 1.4.1 | ~3 MB | 首次构建 | 同上 |
| ML Kit text-recognition | ~3 MB AAR | 首次构建 | on-device OCR 模型在 App 首次启动时下载到设备本地 |
| ML Kit text-recognition-chinese | ~3 MB AAR | 同上 | 中文模型 ~25 MB，首次启动下载 |
| **OpenCV 4.10.0** | ~30 MB | 首次构建 | Maven AAR |
| **PdfBox-Android 2.0.27.0** | ~10 MB | 首次构建 | Maven AAR |

> 这些下载**只在构建机和 App 启动时发生**，不会上传任何数据。
> ML Kit 模型下载需要**网络**；如果设备没网，OCR 会失败（其他功能正常）。

---

## 7. 常见构建错误

### 7.1 `Unsupported class file major version 65`

**原因**：JDK 版本不对
**修复**：

```bash
# 检查 java 版本
java -version
# 必须是 21.x；如果是 17 或更低，去 Adoptium 装 21
```

### 7.2 `SDK location not found`

**原因**：`local.properties` 里的 SDK 路径不对
**修复**：编辑 `local.properties`：

```properties
sdk.dir=C\:\\Users\\<你的用户名>\\AppData\\Local\\Android\\Sdk
```

### 7.3 `Plugin [id: 'com.google.devtools.ksp'] was not found`

**原因**：网络问题或 Maven 仓库被墙
**修复**：检查网络；或用镜像（如阿里云 Maven）。

### 7.4 `OpenCV 4.10.0 not found`

**原因**：`org.opencv:opencv:4.10.0` Maven 路径失败
**修复**：

```bash
# 在 gradle.properties 里加：
systemProp.org.gradle.internal.http.connectionTimeout=120000
systemProp.org.gradle.internal.http.socketTimeout=120000
```

或在 `settings.gradle.kts` 里加 Maven 镜像：

```kotlin
repositories {
    google()
    mavenCentral()
    maven { url = uri("https://maven.aliyun.com/repository/public") }
}
```

### 7.5 `Could not download opencv-4.10.0.aar`

**原因**：maven 仓库网络问题
**修复**：手动下载：

```bash
# 用 curl/wget 下载到 ~/.gradle/caches/modules-2/files-2.1/org.opencv/opencv/4.10.0/
# 然后再 build
```

---

## 8. 设备权限

App 启动时**只申请相机权限**（CAMERA）。其他权限：
- `READ_MEDIA_IMAGES` (API 33+) / `READ_EXTERNAL_STORAGE` (≤32) - 从相册导入时申请
- 不申请 `INTERNET` 权限（硬约束）

**链接 → PDF 功能**需要 `INTERNET` 权限，**默认 App 不申请**。
如果你要用这个功能：
- 设置 → 应用 → PDF 极扫 → 权限 → 开启"网络"（部分厂商/系统在 "特殊权限" 或 "其他权限"）

---

## 9. 应用商店发布

打包 Release APK：

```bash
# 1. 生成签名密钥
keytool -genkey -v -keystore release.keystore -alias scantidy -keyalg RSA -keysize 2048 -validity 10000

# 2. 在 app/build.gradle.kts 加 signingConfigs
# 3. assembleRelease
.\gradlew.bat assembleRelease
```

---

## 10. 调试技巧

- 打开 `Build → Select Build Variant` 选 `debug`
- 用 Layout Inspector 看 Compose 树
- 用 Database Inspector 看 Room 数据（`files/databases/pdfscanner.db`）
- 用 App Inspection → Network Inspector（**应该是空的**，因为没联网）
- 日志在 Logcat，Tag: `ScanApp` / `OpenCV` / `Timber`

---

## 11. 如果你只是想在手机上用

> 不想写代码、不想构建、只想装上 App 用？

构建好的 `app-debug.apk` 可以直接发到手机安装（打开 USB 调试）。
也可以用 ADB 推：

```bash
adb install app\build\outputs\apk\debug\app-debug.apk
```

---

## 12. 目录速查

```
E:\AIPlace\QQAI\
├── docs\                           # 文档
│   ├── pdf-scanner-market-research.md
│   ├── mvp-scope-local-only.md
│   ├── prd-v0.1.md
│   └── plan\engineering-plan.md
├── app\                            # Android 源码
│   └── src\main\java\com\scantidy\scan\
│       ├── MainActivity.kt
│       ├── ScanApp.kt              # Application
│       ├── core\                   # 工具与 DI
│       ├── data\                   # 数据层
│       ├── scan\                   # 扫描核心
│       ├── pdf\                    # PDF 能力
│       ├── web\                    # 链接 → PDF
│       └── ui\                     # 导航 + Compose
├── gradle\libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── BUILD.md                        # 本文件
```
