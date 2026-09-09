import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val appVersion: String = providers.gradleProperty("appVersion").get()
// macOS jpackage rejects versions whose first integer is 0 (JDK 21). Keep the
// product SemVer in gradle.properties and map only the native package field.
val nativePackageVersion: String = if (appVersion.startsWith("0.")) {
    "1${appVersion.removePrefix("0")}"
} else {
    appVersion
}

val osName = System.getProperty("os.name").orEmpty().lowercase()
val osArch = System.getProperty("os.arch").orEmpty().lowercase()
val desktopNative = when {
    osName.contains("mac") && (osArch == "aarch64" || osArch == "arm64") -> libs.compose.desktop.macos.arm64
    osName.contains("mac") -> libs.compose.desktop.macos.x64
    osName.contains("win") -> libs.compose.desktop.windows.x64
    else -> libs.compose.desktop.linux.x64
}

val protocVersion: String = libs.versions.protobuf.get()
val protocClassifiers = listOf("osx-x86_64", "osx-aarch_64", "linux-x86_64", "linux-aarch_64", "windows-x86_64")
val protocHelpers: Configuration = configurations.create("protocHelpers") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}
dependencies {
    protocClassifiers.forEach { classifier ->
        protocHelpers("com.google.protobuf:protoc:$protocVersion:$classifier@exe")
    }
}
val protocHelperDir = layout.buildDirectory.dir("generated/protoc-helpers")
val syncProtocHelpers by tasks.registering(Copy::class) {
    from(protocHelpers)
    into(protocHelperDir.map { it.dir("helpers") })
    rename { name ->
        name.replace(Regex("^protoc-$protocVersion-"), "protoc-").removeSuffix(".exe")
    }
}

kotlin {
    jvmToolchain(21)
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val desktopMain by getting {
            resources.srcDir(protocHelperDir)
            dependencies {
                implementation(libs.compose.desktop)
                implementation(desktopNative)
                implementation(libs.compose.material)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.runtime)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.jackson.core)
                implementation(libs.jackson.databind)
                implementation(libs.json.path)
                implementation(libs.sqlite.jdbc)
                implementation(libs.rsyntaxtextarea)
                implementation(libs.slf4j.nop)
                implementation(libs.uap.java)
                implementation(libs.cron.utils)
                implementation(libs.java.diff.utils)
                implementation(libs.javaparser.core)
                implementation(libs.jsoup)
                implementation(libs.snakeyaml)
                implementation(libs.protobuf.java)
                implementation(libs.protobuf.java.util)
                implementation(libs.bouncy.castle)
                implementation(libs.zxing.core)
                implementation(libs.pdfbox)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.compose.desktop)
                implementation(desktopNative)
                implementation(libs.compose.ui.test.junit4)
                implementation(libs.junit)
                implementation(kotlin("test"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.rememberber.mootool.next.compose.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "MooTool Next Compose"
            packageVersion = nativePackageVersion
            description = "MooTool Next Compose developer toolbox"
            vendor = "RememBerBer"
            copyright = "Copyright (c) 2017 MooTool"
            modules(
                "java.sql",
                "java.xml",
                "java.naming",
                "java.logging",
                "java.management",
                "java.net.http",
                "jdk.crypto.ec",
                "jdk.unsupported",
                "jdk.accessibility",
                "jdk.charsets"
            )
            appResourcesRootDir.set(rootProject.layout.projectDirectory.dir("resources"))
            macOS {
                bundleID = "com.rememberber.mootool.next.compose"
                dockName = "MooTool Next Compose"
                val macIcon = rootProject.file("resources/macos/AppIcon.icns")
                if (macIcon.exists()) iconFile.set(macIcon)
            }
            windows {
                menuGroup = "MooTool Next Compose"
                upgradeUuid = "D6574BAD-FF7C-4038-8D17-B9C7988787BA"
            }
            linux {
                packageName = "mootool-next-compose"
                debMaintainer = "rememberber@users.noreply.github.com"
                val linuxIcon = rootProject.file("resources/linux/AppIcon.png")
                if (linuxIcon.exists()) iconFile.set(linuxIcon)
            }
        }
        buildTypes.release.proguard {
            isEnabled.set(false)
        }
    }
}

tasks.register("printTooling") {
    group = "verification"
    doLast {
        println("appVersion=$appVersion")
        println("nativePackageVersion=$nativePackageVersion")
        println("kotlin=${libs.versions.kotlin.get()}")
        println("compose=${libs.versions.composeMultiplatform.get()}")
        println("java.home=${System.getProperty("java.home")}")
        println("java.version=${System.getProperty("java.version")}")
        println("os.name=${System.getProperty("os.name")}")
        println("os.arch=${System.getProperty("os.arch")}")
        println("protobuf=$protocVersion")
    }
}

tasks.matching { it.name == "desktopProcessResources" || it.name == "compileKotlinDesktop" }.configureEach {
    dependsOn(syncProtocHelpers)
}
