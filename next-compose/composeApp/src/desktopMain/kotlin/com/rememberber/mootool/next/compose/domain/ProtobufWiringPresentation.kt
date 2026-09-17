package com.rememberber.mootool.next.compose.domain

/** F07 各 Tab 引擎调用守卫与复制载荷（可单测）。 */
object ProtobufWiringPresentation {
    fun canDecodeWire(input: String): Boolean = input.isNotBlank()

    fun canJsonToBinary(proto: String, messageName: String, json: String): Boolean =
        proto.isNotBlank() && messageName.isNotBlank() && json.isNotBlank()

    fun canBinaryToJson(proto: String, messageName: String, binary: String): Boolean =
        proto.isNotBlank() && messageName.isNotBlank() && binary.isNotBlank()

    fun canConvertHex(hex: String): Boolean = hex.isNotBlank()

    fun canConvertBase64(base64: String): Boolean = base64.isNotBlank()

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
}
