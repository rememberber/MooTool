package com.rememberber.mootool.next.compose.features.crypto

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.AsymmetricAlgorithm
import com.rememberber.mootool.next.compose.domain.BaseAlgorithm
import com.rememberber.mootool.next.compose.domain.CryptoEngine
import com.rememberber.mootool.next.compose.domain.CryptoException
import com.rememberber.mootool.next.compose.domain.CryptoTab
import com.rememberber.mootool.next.compose.domain.DigestAlgorithm
import com.rememberber.mootool.next.compose.domain.RandomKind
import com.rememberber.mootool.next.compose.domain.SymmetricAlgorithm
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.CryptoSession
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
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
    val revision by container.sessionManager.revision.collectAsState()
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistCrypto()
    }

    LaunchedEffect(session.historyOpen, revision) {
        if (session.historyOpen) historyItems = container.history.list(ToolId.Crypto.id)
    }

    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("crypto.title"), color = colors.textPrimary, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; refresh() })
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Crypto) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.surfaceSubtle).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CryptoTab.entries.forEach { tab ->
                MooButton(container.t(tabTitleKey(tab)), primary = session.tab == tab, onClick = {
                    session.tab = tab
                    session.error = ""
                    refresh()
                })
            }
        }
        when (session.tab) {
            CryptoTab.Symmetric -> SymmetricPanel(container, session, Modifier.weight(1f).fillMaxWidth()) { refresh() }
            CryptoTab.Asymmetric -> AsymmetricPanel(container, session, scope, Modifier.weight(1f).fillMaxWidth()) { refresh() }
            CryptoTab.Digest -> DigestPanel(container, session, scope, Modifier.weight(1f).fillMaxWidth()) { refresh() }
            CryptoTab.Base -> BasePanel(container, session, Modifier.weight(1f).fillMaxWidth()) { refresh() }
            CryptoTab.Random -> RandomPanel(container, session, Modifier.weight(1f).fillMaxWidth()) { refresh() }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(26.dp).background(colors.toolbar).padding(horizontal = 12.dp),
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

    if (session.historyOpen) {
        CryptoHistoryDialog(container, session, historyItems) { refresh() }
    }
}

@Composable
private fun SymmetricPanel(container: AppContainer, session: CryptoSession, modifier: Modifier, onChanged: () -> Unit) {
    val colors = MooTheme.colors
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(container.t("crypto.algorithm"), color = colors.textSecondary, fontSize = 12.sp)
            SymmetricAlgorithm.entries.forEach { algorithm ->
                MooButton(algorithm.name, primary = session.symAlgorithm == algorithm, onClick = {
                    session.symAlgorithm = algorithm
                    session.error = ""
                    onChanged()
                })
            }
            Text(container.t("crypto.key"), color = colors.textSecondary, fontSize = 12.sp)
            MooTextField(session.symKey, { session.symKey = it; session.error = ""; onChanged() }, modifier = Modifier.width(220.dp))
            Text(container.t("crypto.keyHint"), color = colors.textSecondary, fontSize = 12.sp)
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledField(container.t("crypto.plainText"), session.symPlain, { session.symPlain = it; session.error = ""; onChanged() }, Modifier.weight(1f))
            Column(
                modifier = Modifier.width(140.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MooButton(container.t("crypto.encrypt"), primary = true, onClick = {
                    runCrypto(container, session, "symmetric", "encrypt", session.symAlgorithm.name, session.symPlain) {
                        session.symCipher = CryptoEngine.symmetricEncrypt(session.symAlgorithm, session.symPlain, session.symKey)
                        session.symCipher
                    }
                    onChanged()
                })
                MooButton(container.t("crypto.decrypt"), onClick = {
                    runCrypto(container, session, "symmetric", "decrypt", session.symAlgorithm.name, session.symCipher) {
                        session.symPlain = CryptoEngine.symmetricDecrypt(session.symAlgorithm, session.symCipher, session.symKey)
                        session.symPlain
                    }
                    onChanged()
                })
                MooButton(container.t("crypto.copy"), onClick = { session.notice = copyText(session.symCipher, container); onChanged() })
            }
            LabeledField(container.t("crypto.cipherText"), session.symCipher, { session.symCipher = it; session.error = ""; onChanged() }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AsymmetricPanel(
    container: AppContainer,
    session: CryptoSession,
    scope: kotlinx.coroutines.CoroutineScope,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    val colors = MooTheme.colors
    Column(modifier.padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(container.t("crypto.algorithm"), color = colors.textSecondary, fontSize = 12.sp)
            AsymmetricAlgorithm.entries.forEach { algorithm ->
                MooButton(algorithm.name, primary = session.asymAlgorithm == algorithm, onClick = {
                    session.asymAlgorithm = algorithm
                    session.error = ""
                    onChanged()
                })
            }
            MooButton(
                if (session.asymBusy) container.t("common.processing") else container.t("crypto.generateKeyPair"),
                primary = true,
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
        }
        Row(Modifier.height(140.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledField(container.t("crypto.publicKey"), session.publicKey, { session.publicKey = it; onChanged() }, Modifier.weight(1f))
            LabeledField(container.t("crypto.privateKey"), session.privateKey, { session.privateKey = it; onChanged() }, Modifier.weight(1f))
        }
        Row(Modifier.height(160.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledField(container.t("crypto.plainText"), session.asymPlain, { session.asymPlain = it; onChanged() }, Modifier.weight(1f))
            LabeledField(container.t("crypto.cipherOrSignature"), session.asymCipher, { session.asymCipher = it; onChanged() }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MooButton(container.t("crypto.publicEncrypt"), onClick = {
                runCrypto(container, session, "asymmetric", "publicEncrypt", session.asymAlgorithm.name, session.asymPlain) {
                    session.asymCipher = CryptoEngine.asymmetricEncrypt(session.asymAlgorithm, session.asymPlain, session.publicKey)
                    session.asymCipher
                }
                onChanged()
            })
            MooButton(container.t("crypto.privateDecrypt"), onClick = {
                runCrypto(container, session, "asymmetric", "privateDecrypt", session.asymAlgorithm.name, session.asymCipher) {
                    session.asymPlain = CryptoEngine.asymmetricDecrypt(session.asymAlgorithm, session.asymCipher, session.privateKey)
                    session.asymPlain
                }
                onChanged()
            })
            MooButton(container.t("crypto.privateEncrypt"), enabled = session.asymAlgorithm == AsymmetricAlgorithm.RSA, onClick = {
                runCrypto(container, session, "asymmetric", "privateEncrypt", "RSA", session.asymPlain) {
                    if (session.asymAlgorithm != AsymmetricAlgorithm.RSA) throw CryptoException("rsa-only", container.t("crypto.rsaOnly"))
                    session.asymCipher = CryptoEngine.privateEncrypt(session.asymPlain, session.privateKey)
                    session.asymCipher
                }
                onChanged()
            })
            MooButton(container.t("crypto.publicDecrypt"), enabled = session.asymAlgorithm == AsymmetricAlgorithm.RSA, onClick = {
                runCrypto(container, session, "asymmetric", "publicDecrypt", "RSA", session.asymCipher) {
                    if (session.asymAlgorithm != AsymmetricAlgorithm.RSA) throw CryptoException("rsa-only", container.t("crypto.rsaOnly"))
                    session.asymPlain = CryptoEngine.publicDecrypt(session.asymCipher, session.publicKey)
                    session.asymPlain
                }
                onChanged()
            })
            MooButton(container.t("crypto.sign"), onClick = {
                runCrypto(container, session, "asymmetric", "sign", session.asymAlgorithm.name, session.asymPlain) {
                    session.asymCipher = CryptoEngine.signContent(session.asymAlgorithm, session.asymPlain, session.privateKey, session.publicKey)
                    session.asymCipher
                }
                onChanged()
            })
            MooButton(container.t("crypto.verify"), onClick = {
                session.error = ""
                runCatching {
                    CryptoEngine.verifySignature(session.asymAlgorithm, session.asymPlain, session.asymCipher, session.publicKey)
                }.onSuccess { valid ->
                    session.notice = container.t(if (valid) "crypto.verified" else "crypto.notVerified")
                    if (!valid) session.error = container.t("crypto.notVerified")
                }.onFailure { error ->
                    session.notice = ""
                    session.error = messageFor(container, error)
                }
                onChanged()
            })
            MooButton(container.t("crypto.copy"), onClick = { session.notice = copyText(session.asymCipher, container); onChanged() })
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
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(container.t("crypto.algorithm"), color = colors.textSecondary, fontSize = 12.sp)
            DigestAlgorithm.entries.forEach { algorithm ->
                MooButton(digestLabel(algorithm), primary = session.digestAlgorithm == algorithm, onClick = {
                    session.digestAlgorithm = algorithm
                    onChanged()
                })
            }
            MooButton(container.t("crypto.textDigest"), primary = true, onClick = {
                runCrypto(container, session, "digest", "text", digestLabel(session.digestAlgorithm), session.digestInput) {
                    session.digestOutput = CryptoEngine.digestText(session.digestAlgorithm, session.digestInput)
                    session.digestFileName = ""
                    session.digestOutput
                }
                onChanged()
            })
            MooButton(container.t("crypto.fileDigest"), onClick = {
                val file = chooseFile(container.t("crypto.fileDigest")) ?: return@MooButton
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
                            saveHistory(container, "digest", "file", digestLabel(session.digestAlgorithm), file.path, digest)
                        }.onFailure { error ->
                            session.notice = ""
                            session.error = messageFor(container, error)
                        }
                        onChanged()
                    }
                }
            })
            if (session.digestFileName.isNotEmpty()) {
                Text(session.digestFileName, color = colors.textSecondary, fontSize = 12.sp)
            }
        }
        LabeledField(container.t("crypto.digestInput"), session.digestInput, { session.digestInput = it; onChanged() }, Modifier.weight(1f).fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(container.t("crypto.digestResult"), color = colors.textSecondary, fontSize = 12.sp)
            Text(session.digestOutput.ifEmpty { "—" }, color = colors.textPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            MooButton(container.t("crypto.copy"), onClick = { session.notice = copyText(session.digestOutput, container); onChanged() })
        }
    }
}

@Composable
private fun BasePanel(container: AppContainer, session: CryptoSession, modifier: Modifier, onChanged: () -> Unit) {
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(container.t("crypto.algorithm"), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
            BaseAlgorithm.entries.forEach { algorithm ->
                MooButton(algorithm.name, primary = session.baseAlgorithm == algorithm, onClick = {
                    session.baseAlgorithm = algorithm
                    onChanged()
                })
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledField(container.t("crypto.plainText"), session.basePlain, { session.basePlain = it; onChanged() }, Modifier.weight(1f))
            Column(
                modifier = Modifier.width(140.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MooButton(container.t("crypto.encode"), primary = true, onClick = {
                    runCrypto(container, session, "base", "encode", session.baseAlgorithm.name, session.basePlain) {
                        session.baseCipher = CryptoEngine.encodeBase(session.baseAlgorithm, session.basePlain)
                        session.baseCipher
                    }
                    onChanged()
                })
                MooButton(container.t("crypto.decode"), onClick = {
                    runCrypto(container, session, "base", "decode", session.baseAlgorithm.name, session.baseCipher) {
                        session.basePlain = CryptoEngine.decodeBase(session.baseAlgorithm, session.baseCipher)
                        session.basePlain
                    }
                    onChanged()
                })
                MooButton(container.t("crypto.copy"), onClick = { session.notice = copyText(session.baseCipher, container); onChanged() })
            }
            LabeledField(session.baseAlgorithm.name, session.baseCipher, { session.baseCipher = it; onChanged() }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RandomPanel(container: AppContainer, session: CryptoSession, modifier: Modifier, onChanged: () -> Unit) {
    val colors = MooTheme.colors
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(container.t("crypto.length"), color = colors.textSecondary, fontSize = 12.sp)
            MooTextField(session.randomLength.toString(), { value ->
                session.randomLength = value.toIntOrNull() ?: session.randomLength
                onChanged()
            }, modifier = Modifier.width(80.dp))
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = MooTheme.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.width(100.dp))
        Text(value.ifEmpty { "—" }, color = MooTheme.colors.textPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        MooButton(container.t("crypto.copy"), onClick = { copyText(value, container) })
        MooButton(container.t("crypto.generate"), primary = true, onClick = onGenerate)
    }
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
        MooTextField(value, onChange, modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = false)
    }
}

private fun tabTitleKey(tab: CryptoTab): String = when (tab) {
    CryptoTab.Symmetric -> "crypto.tab.symmetric"
    CryptoTab.Asymmetric -> "crypto.tab.asymmetric"
    CryptoTab.Digest -> "crypto.tab.digest"
    CryptoTab.Base -> "crypto.tab.base"
    CryptoTab.Random -> "crypto.tab.random"
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
        saveHistory(container, tab, operation, algorithm, input, output)
    }.onFailure { error ->
        session.notice = ""
        session.error = messageFor(container, error)
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
    container.history.save(ToolId.Crypto.id, "$algorithm $label", label, input, output, historyCodec.encodeToString(meta))
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
        val operation = kind.name.lowercase()
        saveHistory(container, "random", operation, kind.name, if (kind == RandomKind.Uuid) "" else session.randomLength.toString(), output)
        val length = session.randomLength.coerceIn(CryptoEngine.MIN_RANDOM_LENGTH, CryptoEngine.MAX_RANDOM_LENGTH)
        if (container.settings.value.tools.randomStringLength != length) {
            container.updateSettings { current -> current.copy(tools = current.tools.copy(randomStringLength = length)) }
        }
    }.onFailure { error ->
        session.notice = ""
        session.error = messageFor(container, error)
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
    return runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
        container.t("json.notice.copied")
    }.getOrElse { container.t("crypto.error.generic") }
}

private fun chooseFile(title: String): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return File(directory, file)
}

@Composable
private fun CryptoHistoryDialog(
    container: AppContainer,
    session: CryptoSession,
    items: List<HistoryRecord>,
    onChanged: () -> Unit
) {
    Dialog(onDismissRequest = { session.historyOpen = false; onChanged() }) {
        Column(
            Modifier.width(560.dp).height(420.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("common.action.history"), color = MooTheme.colors.textPrimary)
            if (items.isEmpty()) {
                Text(container.t("json.history.empty"), color = MooTheme.colors.textSecondary)
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(items) { item ->
                        Column(Modifier.fillMaxWidth().clickable {
                            applyHistory(session, item)
                            session.notice = container.t("json.notice.restored")
                            session.historyOpen = false
                            onChanged()
                        }.padding(8.dp)) {
                            Text(item.summary, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                            Text("${item.input} → ${item.output}", color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.clear"), onClick = {
                    container.history.clear(ToolId.Crypto.id)
                    onChanged()
                })
                MooButton(container.t("common.close"), onClick = { session.historyOpen = false; onChanged() })
            }
        }
    }
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
