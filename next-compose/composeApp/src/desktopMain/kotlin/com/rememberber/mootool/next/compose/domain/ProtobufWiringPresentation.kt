package com.rememberber.mootool.next.compose.domain

/** F07 各 Tab 引擎调用守卫、复制载荷与引擎路径（可单测）。 */
object ProtobufWiringPresentation {
    sealed interface OperationOutcome {
        data class Success(val output: String) : OperationOutcome
        data class Failure(val error: Throwable) : OperationOutcome
    }

    private inline fun wrap(block: () -> String): OperationOutcome =
        runCatching { block() }.fold(
            onSuccess = { OperationOutcome.Success(it) },
            onFailure = { OperationOutcome.Failure(it) },
        )

    fun runJsonToBinary(
        proto: String,
        messageName: String,
        json: String,
        format: ProtobufBinaryFormat,
    ): OperationOutcome = wrap { ProtobufEngine.jsonToProtobuf(proto, messageName, json, format) }

    fun runBinaryToJson(
        proto: String,
        messageName: String,
        binary: String,
        format: ProtobufBinaryFormat,
    ): OperationOutcome = wrap { ProtobufEngine.protobufToJson(proto, messageName, binary, format) }

    fun runDecodeWire(input: String, format: ProtobufBinaryFormat): OperationOutcome =
        wrap { ProtobufEngine.decodeWire(input, format) }

    fun runConvertBinary(
        input: String,
        from: ProtobufBinaryFormat,
        to: ProtobufBinaryFormat,
    ): OperationOutcome = wrap { ProtobufEngine.convertBinary(input, from, to) }

    fun canDecodeWire(input: String): Boolean = input.isNotBlank()

    fun canJsonToBinary(proto: String, messageName: String, json: String): Boolean =
        proto.isNotBlank() && messageName.isNotBlank() && json.isNotBlank()

    fun canBinaryToJson(proto: String, messageName: String, binary: String): Boolean =
        proto.isNotBlank() && messageName.isNotBlank() && binary.isNotBlank()

    fun canConvertHex(hex: String): Boolean = hex.isNotBlank()

    fun canConvertBase64(base64: String): Boolean = base64.isNotBlank()

    fun jsonToBinaryActionEnabled(proto: String, messageName: String, json: String): Boolean =
        canJsonToBinary(proto, messageName, json)

    fun binaryToJsonActionEnabled(proto: String, messageName: String, binary: String): Boolean =
        canBinaryToJson(proto, messageName, binary)

    fun decodeWireActionEnabled(input: String): Boolean = canDecodeWire(input)

    fun convertHexActionEnabled(hex: String): Boolean = canConvertHex(hex)

    fun convertBase64ActionEnabled(base64: String): Boolean = canConvertBase64(base64)

    fun shouldToastOperationFailure(error: Throwable): Boolean = true

    fun copyPayload(
        tab: String,
        wireOutput: String,
        base64: String,
        hex: String,
        binary: String,
    ): String = when (tab) {
        "wire" -> wireOutput
        "convert" -> base64.ifEmpty { hex }
        else -> binary
    }

    fun copyPayloadActionEnabled(
        tab: String,
        wireOutput: String,
        base64: String,
        hex: String,
        binary: String,
    ): Boolean = copyPayload(tab, wireOutput, base64, hex, binary).isNotBlank()
}
