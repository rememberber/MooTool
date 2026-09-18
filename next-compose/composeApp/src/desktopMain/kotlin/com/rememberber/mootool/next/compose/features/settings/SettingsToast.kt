package com.rememberber.mootool.next.compose.features.settings

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.SettingsValidationToastPresentation

internal fun notifySettingsValidationFailure(container: AppContainer, message: String) {
    if (message.isBlank()) return
    if (SettingsValidationToastPresentation.shouldToastValidationFailure()) {
        container.toastError(message)
    }
}
