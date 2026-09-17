package com.rememberber.mootool.next.compose.features.crypto

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.AsymmetricAlgorithm
import com.rememberber.mootool.next.compose.domain.BaseAlgorithm
import com.rememberber.mootool.next.compose.domain.CryptoEngine
import com.rememberber.mootool.next.compose.domain.CryptoException
import com.rememberber.mootool.next.compose.domain.CryptoTab
import com.rememberber.mootool.next.compose.domain.DigestAlgorithm
import com.rememberber.mootool.next.compose.domain.RandomKind
import com.rememberber.mootool.next.compose.domain.SymmetricAlgorithm
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.CryptoSession
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.SelectedFileName
import com.rememberber.mootool.next.compose.ui.components.desktopFileDropTarget
import com.rememberber.mootool.next.compose.ui.components.MooGhostButton
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooToolTab
import com.rememberber.mootool.next.compose.ui.components.MooToolTabsRow
import com.rememberber.mootool.next.compose.ui.components.IoThreePaneRow
import com.rememberber.mootool.next.compose.ui.components.IoTwoPaneRow
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooStatusBarBackground
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.onUserInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Serializable
private data class CryptoHistoryMeta(
    val tab: String,
    val operation: String,
    val algorithm: String = ""
)

@Composable
fun CryptoScreen(container: AppContainer, detached: Boolean) {
    val session = remember {
        container.sessionManager.cryptoSession(container.settings.value.tools.randomStringLength)
    }
    DismissModalOverlaysOnDispose(container, ToolId.Crypto) { session.dismissModalOverlays() }
    val revision by container.sessionManager.revision.collectAsState()
    val settings by container.settings.collectAsState()
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()
    var moreOpen by remember { mutableStateOf(false) }

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistCrypto()
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace)) {
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("crypto.title"))
            Spacer(Modifier.weight(1f))
            if (!overflow) {
                MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; refresh() })
                if (!detached) {
                    MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Crypto) })
                }
            } else {
                Box {
                    MooButton(container.t("json.action.overflow"), primary = moreOpen, onClick = { moreOpen = true })
                    MooMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        MooMenuItem(onClick = { moreOpen = false; session.historyOpen = true; refresh() }) {
                            Text(container.t("common.action.history"))
                        }
                        if (!detached) {
                            MooMenuItem(onClick = { moreOpen = false; container.sessionManager.detach(ToolId.Crypto) }) {
                                Text(container.t("app.tool.detach"))
                            }
                        }
                    }
                }
            }
        }
        MooToolTabsRow {
            CryptoTab.entries.forEach { tab ->
                MooToolTab(container.t(tabTitleKey(tab)), selected = session.tab == tab, onClick = {
                    session.tab = tab
                    session.error = ""
                    refresh()
                })
            }
        }
        when (session.tab) {
            CryptoTab.Symmetric -> SymmetricPanel(container, session, settings, Modifier.weight(1f).fillMaxWidth()) { refresh() }
            CryptoTab.Asymmetric -> AsymmetricPanel(container, session, settings, scope, Modifier.weight(1f).fillMaxWidth()) { refresh() }
            CryptoTab.Digest -> DigestPanel(container, session, scope, Modifier.weight(1f).fillMaxWidth()) { refresh() }
            CryptoTab.Base -> BasePanel(container, session, settings, Modifier.weight(1f).fillMaxWidth()) { refresh() }
            CryptoTab.Random -> RandomPanel(container, session, Modifier.weight(1f).fillMaxWidth()) { refresh() }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.statusBar).mooStatusBarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                session.error.ifEmpty { session.notice },
                color = if (session.error.isNotEmpty()) colors.danger else colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.weight(1f))
            if (detached) Text("detached", color = colors.textSecondary, fontSize = 12.sp)
        }
        }
    }

    if (session.historyOpen) {
        HistoryBrowser(
            container = container,
            toolId = ToolId.Crypto.id,
            title = container.t("common.action.history"),
            onRestore = { item ->
                applyHistory(session, item)
                session.historyOpen = false
                refresh()
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
    }
}

@Composable
private fun SymmetricPanel(
    container: AppContainer,
    session: CryptoSession,
    settings: AppSettings,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    val colors = MooTheme.colors
    Column(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(container.t("crypto.algorithm"), color = colors.textSecondary, fontSize = 12.sp)
            CompactChoice(
                current = session.symAlgorithm,
                options = SymmetricAlgorithm.entries,
                label = { it.name },
                onSelect = {
                    session.symAlgorithm = it
                    session.error = ""
                    onChanged()
                }
            )
            Text(container.t("crypto.key"), color = colors.textSecondary, fontSize = 12.sp)
            MooTextField(
                session.symKey,
                { session.onUserInput { session.symKey = it; session.error = ""; onChanged() } },
                modifier = Modifier.width(220.dp)
            )
            Text(container.t("crypto.keyHint"), color = colors.textMuted, fontSize = 10.sp)
            val keyLength = CryptoEngine.symmetricKeyUtf8Length(session.symAlgorithm, session.symKey)
            Text(
                container.t(
                    "crypto.keyBytes",
                    mapOf("current" to keyLength.current.toString(), "required" to keyLength.required.toString())
                ),
                color = if (keyLength.valid) colors.textMuted else colors.warning,
                fontSize = 10.sp
            )
        }
        IoThreePaneRow(
            container = container,
            settings = settings,
            paneKey = "crypto-symmetric",
            middleRatio = 0.28f,
            minRight = 240f,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            left = {
                LabeledField(session, container.t("crypto.plainText"), session.symPlain, { session.symPlain = it; session.error = ""; onChanged() }, Modifier.fillMaxSize())
            },
            middle = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MooButton(container.t("crypto.encrypt"), prominent = true, p5Toolbar = true, onClick = {
                        runCrypto(container, session, "symmetric", "encrypt", session.symAlgorithm.name, session.symPlain) {
                            session.symCipher = CryptoEngine.symmetricEncrypt(session.symAlgorithm, session.symPlain, session.symKey)
                            session.symCipher
                        }
                        onChanged()
                    })
                    MooButton(container.t("crypto.decrypt"), p5Toolbar = true, onClick = {
                        runCrypto(container, session, "symmetric", "decrypt", session.symAlgorithm.name, session.symCipher) {
                            session.symPlain = CryptoEngine.symmetricDecrypt(session.symAlgorithm, session.symCipher, session.symKey)
                            session.symPlain
                        }
                        onChanged()
                    })
                    MooButton(container.t("crypto.copy"), p5Toolbar = true, onClick = { session.notice = copyText(session.symCipher, container); onChanged() })
                }
            },
            right = {
                LabeledField(session, container.t("crypto.cipherText"), session.symCipher, { session.symCipher = it; session.error = ""; onChanged() }, Modifier.fillMaxSize())
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AsymmetricPanel(
    container: AppContainer,
    session: CryptoSession,
    settings: AppSettings,
    scope: kotlinx.coroutines.CoroutineScope,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    val colors = MooTheme.colors
    Column(modifier.padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(container.t("crypto.algorithm"), color = colors.textSecondary, fontSize = 12.sp)
            CompactChoice(
                current = session.asymAlgorithm,
                options = AsymmetricAlgorithm.entries,
                label = { it.name },
                onSelect = {
                    session.asymAlgorithm = it
                    session.error = ""
                    onChanged()
                }
            )
            MooButton(
                if (session.asymBusy) container.t("common.processing") else container.t("crypto.generateKeyPair"),
                prominent = true,
                p5Toolbar = true,
                enabled = !session.asymBusy,
                onClick = {
                    session.asymBusy = true
                    session.error = ""
                    session.notice = container.t("common.processing")
                    onChanged()
                    scope.launch(Dispatchers.Default) {
                        val result = runCatching { CryptoEngine.generateAsymmetricKeyPair(session.asymAlgorithm) }
                        kotlinx.coroutines.withContext(Dispatchers.Swing) {
                            session.asymBusy = false
                            result.onSuccess { pair ->
                                session.publicKey = pair.publicKey
                                session.privateKey = pair.privateKey
                                session.notice = container.t("crypto.keyGenerated")
                                container.toastSuccess(container.t("crypto.keyGenerated"))
                                session.error = ""
                            }.onFailure { error ->
                                session.notice = ""
                                session.error = messageFor(container, error)
                            }
                            onChanged()
                        }
                    }
                }
            )
            MooButton(
                container.t("crypto.restorePublicKey"),
                p5Toolbar = true,
                enabled = session.privateKey.isNotBlank() && !session.asymBusy,
                onClick = {
                    session.error = ""
                    runCatching {
                        CryptoEngine.deriveAsymmetricPublicKey(session.asymAlgorithm, session.privateKey)
                    }.onSuccess { restored ->
                        session.publicKey = restored
                        session.notice = container.t("crypto.publicKeyRestored")
                        container.toastSuccess(container.t("crypto.publicKeyRestored"))
                    }.onFailure { error ->
                        session.notice = ""
                        session.error = messageFor(container, error)
                        container.toastError(session.error)
                    }
                    onChanged()
                }
            )
        }
        val asymKeys = CryptoEngine.asymmetricKeyStatus(session.asymAlgorithm, session.publicKey, session.privateKey)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                container.t(if (asymKeys.publicReady) "crypto.asymPublicReady" else "crypto.asymPublicMissing"),
                color = if (asymKeys.publicReady) colors.textMuted else colors.warning,
                fontSize = 10.sp
            )
            Text(
                container.t(if (asymKeys.privateReady) "crypto.asymPrivateReady" else "crypto.asymPrivateMissing"),
                color = if (asymKeys.privateReady) colors.textMuted else colors.warning,
                fontSize = 10.sp
            )
            when (session.asymAlgorithm) {
                AsymmetricAlgorithm.RSA -> asymKeys.rsaModulusBits?.let { bits ->
                    Text(
                        container.t("crypto.rsaBits", mapOf("bits" to bits.toString())),
                        color = colors.textMuted,
                        fontSize = 10.sp
                    )
                }
                AsymmetricAlgorithm.SM2 -> {
                    if (asymKeys.sm2PublicBytes != null) {
                        Text(
                            container.t(
                                "crypto.sm2KeyBytes",
                                mapOf("label" to "pub", "current" to asymKeys.sm2PublicBytes.toString(), "required" to "64/65")
                            ),
                            color = if (asymKeys.publicReady) colors.textMuted else colors.warning,
                            fontSize = 10.sp
                        )
                    }
                    if (asymKeys.sm2PrivateBytes != null) {
                        Text(
                            container.t(
                                "crypto.sm2KeyBytes",
                                mapOf("label" to "priv", "current" to asymKeys.sm2PrivateBytes.toString(), "required" to "32")
                            ),
                            color = if (asymKeys.privateReady) colors.textMuted else colors.warning,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
        IoTwoPaneRow(
            container = container,
            settings = settings,
            paneKey = "crypto-key-pair",
            minLeft = 260f,
            minRight = 260f,
            defaultLeftFraction = 0.5f,
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
            left = {
                LabeledField(session, container.t("crypto.publicKey"), session.publicKey, { session.publicKey = it; onChanged() }, Modifier.fillMaxSize().height(140.dp))
            },
            right = {
                LabeledField(session, container.t("crypto.privateKey"), session.privateKey, { session.privateKey = it; onChanged() }, Modifier.fillMaxSize().height(140.dp))
            }
        )
        Row(Modifier.fillMaxWidth().heightIn(min = 160.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledField(session, container.t("crypto.plainText"), session.asymPlain, { session.asymPlain = it; onChanged() }, Modifier.weight(1f).height(160.dp))
            LabeledField(session, container.t("crypto.cipherOrSignature"), session.asymCipher, { session.asymCipher = it; onChanged() }, Modifier.weight(1f).height(160.dp))
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            MooButton(container.t("crypto.publicEncrypt"), p5Toolbar = true, onClick = {
                runCrypto(container, session, "asymmetric", "publicEncrypt", session.asymAlgorithm.name, session.asymPlain) {
                    session.asymCipher = CryptoEngine.asymmetricEncrypt(session.asymAlgorithm, session.asymPlain, session.publicKey)
                    session.asymCipher
                }
                onChanged()
            })
            MooButton(container.t("crypto.privateDecrypt"), p5Toolbar = true, onClick = {
                runCrypto(container, session, "asymmetric", "privateDecrypt", session.asymAlgorithm.name, session.asymCipher) {
                    session.asymPlain = CryptoEngine.asymmetricDecrypt(session.asymAlgorithm, session.asymCipher, session.privateKey)
                    session.asymPlain
                }
                onChanged()
            })
            MooButton(container.t("crypto.privateEncrypt"), enabled = session.asymAlgorithm == AsymmetricAlgorithm.RSA, p5Toolbar = true, onClick = {
                runCrypto(container, session, "asymmetric", "privateEncrypt", "RSA", session.asymPlain) {
                    if (session.asymAlgorithm != AsymmetricAlgorithm.RSA) throw CryptoException("rsa-only", container.t("crypto.rsaOnly"))
                    session.asymCipher = CryptoEngine.privateEncrypt(session.asymPlain, session.privateKey)
                    session.asymCipher
                }
                onChanged()
            })
            MooButton(container.t("crypto.publicDecrypt"), enabled = session.asymAlgorithm == AsymmetricAlgorithm.RSA, p5Toolbar = true, onClick = {
                runCrypto(container, session, "asymmetric", "publicDecrypt", "RSA", session.asymCipher) {
                    if (session.asymAlgorithm != AsymmetricAlgorithm.RSA) throw CryptoException("rsa-only", container.t("crypto.rsaOnly"))
                    session.asymPlain = CryptoEngine.publicDecrypt(session.asymCipher, session.publicKey)
                    session.asymPlain
                }
                onChanged()
            })
            MooButton(container.t("crypto.sign"), p5Toolbar = true, onClick = {
                runCrypto(container, session, "asymmetric", "sign", session.asymAlgorithm.name, session.asymPlain) {
                    session.asymCipher = CryptoEngine.signContent(session.asymAlgorithm, session.asymPlain, session.privateKey, session.publicKey)
                    session.asymCipher
                }
                onChanged()
            })
            MooButton(container.t("crypto.verify"), p5Toolbar = true, onClick = {
                session.error = ""
                runCatching {
                    CryptoEngine.verifySignature(session.asymAlgorithm, session.asymPlain, session.asymCipher, session.publicKey)
                }.onSuccess { valid ->
                    val notice = container.t(if (valid) "crypto.verified" else "crypto.notVerified")
                    session.notice = notice
                    if (valid) container.toastSuccess(notice) else container.toastError(notice)
                    if (!valid) session.error = container.t("crypto.notVerified")
                }.onFailure { error ->
                    session.notice = ""
                    val message = messageFor(container, error)
                    session.error = message
                    container.toastError(message)
                }
                onChanged()
            })
            MooButton(container.t("crypto.copy"), p5Toolbar = true, onClick = { session.notice = copyText(session.asymCipher, container); onChanged() })
        }
    }
}

@Composable
private fun DigestPanel(
    container: AppContainer,
    session: CryptoSession,
    scope: kotlinx.coroutines.CoroutineScope,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    val colors = MooTheme.colors
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().desktopFileDropTarget { files ->
                digestPickedFile(container, session, scope, files.first(), onChanged)
            },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(container.t("crypto.algorithm"), color = colors.textSecondary, fontSize = 12.sp)
            CompactChoice(
                current = session.digestAlgorithm,
                options = DigestAlgorithm.entries,
                label = { digestLabel(it) },
                onSelect = {
                    session.digestAlgorithm = it
                    onChanged()
                }
            )
            MooButton(container.t("crypto.textDigest"), prominent = true, p5Toolbar = true, onClick = {
                runCrypto(container, session, "digest", "text", digestLabel(session.digestAlgorithm), session.digestInput) {
                    session.digestOutput = CryptoEngine.digestText(session.digestAlgorithm, session.digestInput)
                    session.digestFileName = ""
                    session.digestOutput
                }
                onChanged()
            })
            MooButton(container.t("crypto.fileDigest"), p5Toolbar = true, onClick = {
                val file = chooseFile(container.t("crypto.fileDigest")) ?: return@MooButton
                digestPickedFile(container, session, scope, file, onChanged)
            })
            SelectedFileName(session.digestFileName, Modifier.weight(1f, fill = false))
        }
        LabeledField(session, container.t("crypto.digestInput"), session.digestInput, { session.digestInput = it; onChanged() }, Modifier.weight(1f).fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(container.t("crypto.digestResult"), color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(session.digestOutput.ifEmpty { "—" }, color = colors.textBody, fontSize = 11.sp, modifier = Modifier.weight(1f))
            MooGhostButton(container.t("crypto.copy"), onClick = { session.notice = copyText(session.digestOutput, container); onChanged() }, size = 24.dp) {
                Text("⎘", color = colors.textMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BasePanel(
    container: AppContainer,
    session: CryptoSession,
    settings: AppSettings,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    Column(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(container.t("crypto.algorithm"), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
            CompactChoice(
                current = session.baseAlgorithm,
                options = BaseAlgorithm.entries,
                label = { it.name },
                onSelect = {
                    session.baseAlgorithm = it
                    onChanged()
                }
            )
        }
        IoThreePaneRow(
            container = container,
            settings = settings,
            paneKey = "crypto-base",
            middleRatio = 0.28f,
            minRight = 240f,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            left = {
                LabeledField(session, container.t("crypto.plainText"), session.basePlain, { session.basePlain = it; onChanged() }, Modifier.fillMaxSize())
            },
            middle = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MooButton(container.t("crypto.encode"), prominent = true, p5Toolbar = true, onClick = {
                        runCrypto(container, session, "base", "encode", session.baseAlgorithm.name, session.basePlain) {
                            session.baseCipher = CryptoEngine.encodeBase(session.baseAlgorithm, session.basePlain)
                            session.baseCipher
                        }
                        onChanged()
                    })
                    MooButton(container.t("crypto.decode"), p5Toolbar = true, onClick = {
                        runCrypto(container, session, "base", "decode", session.baseAlgorithm.name, session.baseCipher) {
                            session.basePlain = CryptoEngine.decodeBase(session.baseAlgorithm, session.baseCipher)
                            session.basePlain
                        }
                        onChanged()
                    })
                    MooButton(container.t("crypto.copy"), p5Toolbar = true, onClick = { session.notice = copyText(session.baseCipher, container); onChanged() })
                }
            },
            right = {
                LabeledField(session, session.baseAlgorithm.name, session.baseCipher, { session.baseCipher = it; onChanged() }, Modifier.fillMaxSize())
            }
        )
    }
}

@Composable
private fun RandomPanel(container: AppContainer, session: CryptoSession, modifier: Modifier, onChanged: () -> Unit) {
    val colors = MooTheme.colors
    Column(modifier.padding(14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            modifier = Modifier.padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(container.t("crypto.length"), color = colors.textMuted, fontSize = 11.sp)
            MooTextField(session.randomLength.toString(), { value ->
                session.randomLength = value.toIntOrNull() ?: session.randomLength
                onChanged()
            }, modifier = Modifier.width(100.dp), compact = true)
        }
        RandomRow(container, container.t("crypto.random.uuid"), session.uuid) {
            generateRandom(container, session, RandomKind.Uuid)
            onChanged()
        }
        RandomRow(container, container.t("crypto.random.digits"), session.digits) {
            generateRandom(container, session, RandomKind.Digits)
            onChanged()
        }
        RandomRow(container, container.t("crypto.random.string"), session.randomText) {
            generateRandom(container, session, RandomKind.String)
            onChanged()
        }
        RandomRow(container, container.t("crypto.random.password"), session.password) {
            generateRandom(container, session, RandomKind.Password)
            onChanged()
        }
    }
}

@Composable
private fun RandomRow(container: AppContainer, label: String, value: String, onGenerate: () -> Unit) {
    val colors = MooTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, color = colors.textBody, fontSize = 12.sp, modifier = Modifier.width(124.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            value.ifEmpty { "—" },
            color = colors.textBody,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f).heightIn(min = 34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.workspace)
                .border(1.dp, colors.borderControl, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
        MooGhostButton(container.t("crypto.copy"), onClick = { copyText(value, container) }, size = 32.dp) {
            Text("⎘", color = colors.textMuted, fontSize = 13.sp)
        }
        MooButton(container.t("crypto.generate"), prominent = true, p5Toolbar = true, onClick = onGenerate, modifier = Modifier.width(84.dp))
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
}

@Composable
private fun <T> CompactChoice(
    current: T,
    options: Iterable<T>,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        MooButton(label(current), onClick = { open = true }, p5Toolbar = true)
        MooMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                MooMenuItem(onClick = {
                    open = false
                    onSelect(option)
                }) {
                    Text(label(option))
                }
            }
        }
    }
}

@Composable
private fun LabeledField(
    session: CryptoSession,
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = MooTheme.colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        MooTextField(
            value,
            { new -> session.onUserInput { onChange(new) } },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            singleLine = false
        )
    }
}

private fun tabTitleKey(tab: CryptoTab): String = when (tab) {
    CryptoTab.Symmetric -> "crypto.tab.symmetric"
    CryptoTab.Asymmetric -> "crypto.tab.asymmetric"
    CryptoTab.Digest -> "crypto.tab.digest"
    CryptoTab.Base -> "crypto.tab.base"
    CryptoTab.Random -> "crypto.tab.random"
}

private fun digestPickedFile(
    container: AppContainer,
    session: CryptoSession,
    scope: kotlinx.coroutines.CoroutineScope,
    file: java.io.File,
    onChanged: () -> Unit
) {
    session.notice = container.t("common.processing")
    onChanged()
    scope.launch(Dispatchers.Default) {
        val result = runCatching { CryptoEngine.digestFile(session.digestAlgorithm, file.toPath()) }
        kotlinx.coroutines.withContext(Dispatchers.Swing) {
            result.onSuccess { digest ->
                session.digestFileName = file.name
                session.digestOutput = digest
                session.error = ""
                session.notice = container.t("crypto.fileDigest")
                container.toastSuccess(session.notice)
                saveHistory(container, "digest", "file", digestLabel(session.digestAlgorithm), file.path, digest)
            }.onFailure { error ->
                session.notice = ""
                val message = messageFor(container, error)
                session.error = message
                container.toastError(message)
            }
            onChanged()
        }
    }
}

private fun digestLabel(algorithm: DigestAlgorithm): String = when (algorithm) {
    DigestAlgorithm.MD5 -> "MD5"
    DigestAlgorithm.SHA1 -> "SHA-1"
    DigestAlgorithm.SHA256 -> "SHA-256"
    DigestAlgorithm.SHA384 -> "SHA-384"
    DigestAlgorithm.SHA512 -> "SHA-512"
    DigestAlgorithm.SM3 -> "SM3"
}

private val historyCodec = Json { ignoreUnknownKeys = true }

private fun runCrypto(
    container: AppContainer,
    session: CryptoSession,
    tab: String,
    operation: String,
    algorithm: String,
    input: String,
    block: () -> String
) {
    runCatching(block).onSuccess { output ->
        session.error = ""
        session.notice = algorithm
        container.toastSuccess(algorithm)
        saveHistory(container, tab, operation, algorithm, input, output)
    }.onFailure { error ->
        session.notice = ""
        val message = messageFor(container, error)
        session.error = message
        container.toastError(message)
    }
}

private fun saveHistory(
    container: AppContainer,
    tab: String,
    operation: String,
    algorithm: String,
    input: String,
    output: String
) {
    val label = when (operation) {
        "encrypt" -> container.t("crypto.encrypt")
        "decrypt" -> container.t("crypto.decrypt")
        "publicEncrypt" -> container.t("crypto.publicEncrypt")
        "privateDecrypt" -> container.t("crypto.privateDecrypt")
        "privateEncrypt" -> container.t("crypto.privateEncrypt")
        "publicDecrypt" -> container.t("crypto.publicDecrypt")
        "sign" -> container.t("crypto.sign")
        "text" -> container.t("crypto.digest")
        "file" -> container.t("crypto.fileDigest")
        "encode" -> container.t("crypto.encode")
        "decode" -> container.t("crypto.decode")
        "uuid" -> container.t("crypto.random.uuid")
        "digits" -> container.t("crypto.random.digits")
        "string" -> container.t("crypto.random.string")
        "password" -> container.t("crypto.random.password")
        else -> algorithm
    }
    val meta = CryptoHistoryMeta(tab = tab, operation = operation, algorithm = algorithm)
    val payload = com.rememberber.mootool.next.compose.storage.HistoryPrivacy.crypto(operation, input, output)
    container.history.save(ToolId.Crypto.id, "$algorithm $label", label, payload.input, payload.output, historyCodec.encodeToString(meta))
}

private fun generateRandom(container: AppContainer, session: CryptoSession, kind: RandomKind) {
    runCatching {
        val output = when (kind) {
            RandomKind.Uuid -> CryptoEngine.randomUuid()
            RandomKind.Digits -> CryptoEngine.randomDigits(session.randomLength)
            RandomKind.String -> CryptoEngine.randomString(session.randomLength)
            RandomKind.Password -> CryptoEngine.randomPassword(session.randomLength)
        }
        when (kind) {
            RandomKind.Uuid -> session.uuid = output
            RandomKind.Digits -> session.digits = output
            RandomKind.String -> session.randomText = output
            RandomKind.Password -> session.password = output
        }
        session.error = ""
        session.notice = container.t("crypto.generate")
        container.toastSuccess(session.notice)
        val operation = kind.name.lowercase()
        saveHistory(container, "random", operation, kind.name, if (kind == RandomKind.Uuid) "" else session.randomLength.toString(), output)
        val length = session.randomLength.coerceIn(CryptoEngine.MIN_RANDOM_LENGTH, CryptoEngine.MAX_RANDOM_LENGTH)
        if (container.settings.value.tools.randomStringLength != length) {
            container.updateSettings { current -> current.copy(tools = current.tools.copy(randomStringLength = length)) }
        }
    }.onFailure { error ->
        session.notice = ""
        val message = messageFor(container, error)
        session.error = message
        container.toastError(message)
    }
}

private fun messageFor(container: AppContainer, error: Throwable): String {
    val code = (error as? CryptoException)?.code
    return when (code) {
        "invalid-hex" -> container.t("crypto.error.hex")
        "invalid-base64", "invalid-base32", "invalid-base" -> container.t("crypto.error.base")
        "invalid-key" -> container.t("crypto.error.key", mapOf("message" to (error.message ?: "")))
        "decrypt" -> container.t("crypto.error.decrypt")
        "invalid-length" -> container.t("crypto.error.length")
        "too-large" -> container.t("crypto.error.large", mapOf("message" to (error.message ?: "")))
        "rsa-only" -> container.t("crypto.rsaOnly")
        "invalid-cipher" -> container.t("crypto.error.cipher")
        else -> error.message ?: container.t("crypto.error.generic")
    }
}

private fun copyText(value: String, container: AppContainer): String {
    if (value.isEmpty()) return container.t("crypto.nothingToCopy")
    return if (container.copyText(value)) container.t("json.notice.copied") else container.t("crypto.error.generic")
}

private fun chooseFile(title: String): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return File(directory, file)
}


private fun applyHistory(session: CryptoSession, item: HistoryRecord) {
    val meta = runCatching { historyCodec.decodeFromString<CryptoHistoryMeta>(item.options) }.getOrNull()
    val tab = meta?.tab.orEmpty()
    val operation = meta?.operation.orEmpty()
    session.tab = when (tab) {
        "asymmetric" -> CryptoTab.Asymmetric
        "digest" -> CryptoTab.Digest
        "base" -> CryptoTab.Base
        "random" -> CryptoTab.Random
        else -> CryptoTab.Symmetric
    }
    when (session.tab) {
        CryptoTab.Symmetric -> {
            if (operation == "decrypt") {
                session.symCipher = item.input
                session.symPlain = item.output
            } else {
                session.symPlain = item.input
                session.symCipher = item.output
            }
            SymmetricAlgorithm.entries.find { it.name == meta?.algorithm }?.let { session.symAlgorithm = it }
        }
        CryptoTab.Asymmetric -> {
            if (operation.contains("Decrypt", ignoreCase = true)) {
                session.asymCipher = item.input
                session.asymPlain = item.output
            } else {
                session.asymPlain = item.input
                session.asymCipher = item.output
            }
            AsymmetricAlgorithm.entries.find { it.name == meta?.algorithm }?.let { session.asymAlgorithm = it }
        }
        CryptoTab.Digest -> {
            session.digestInput = item.input
            session.digestOutput = item.output
            DigestAlgorithm.entries.find { digestLabel(it) == meta?.algorithm }?.let { session.digestAlgorithm = it }
        }
        CryptoTab.Base -> {
            if (operation == "decode") {
                session.baseCipher = item.input
                session.basePlain = item.output
            } else {
                session.basePlain = item.input
                session.baseCipher = item.output
            }
            BaseAlgorithm.entries.find { it.name == meta?.algorithm }?.let { session.baseAlgorithm = it }
        }
        CryptoTab.Random -> when (operation) {
            "uuid" -> session.uuid = item.output
            "digits" -> session.digits = item.output
            "string" -> session.randomText = item.output
            "password" -> session.password = item.output
        }
    }
    session.error = ""
}
