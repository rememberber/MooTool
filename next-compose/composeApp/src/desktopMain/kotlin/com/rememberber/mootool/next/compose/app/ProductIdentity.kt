package com.rememberber.mootool.next.compose.app

object ProductIdentity {
    const val PRODUCT_ID = "next-compose"
    const val DISPLAY_NAME = "MooTool Next Compose"
    const val APPLICATION_ID = "com.rememberber.mootool.next.compose"
    const val LINUX_PACKAGE = "mootool-next-compose"
    const val LINUX_DESKTOP_ENTRY = "com.rememberber.mootool.next.compose.desktop"
    const val WINDOWS_INSTALL_NAME = "MooTool Next Compose"
    const val MACOS_APP_DIR = "MooTool Next Compose.app"
    const val SETTINGS_FILE = "settings.json"
    const val PRODUCT_MARKER = "product.json"
    const val DATABASE_NAME = "mootool-compose.sqlite"
    const val LOCK_NAME = "instance.lock"
    const val DATA_DIR_ENV = "MOOTOOL_COMPOSE_DATA_DIR"
    const val VERSION: String = BuildConfig.VERSION
    const val SCHEMA_VERSION: Int = BuildConfig.SCHEMA_VERSION
}

object BuildConfig {
    const val VERSION: String = "0.1.0"
    const val SCHEMA_VERSION: Int = 1
}
