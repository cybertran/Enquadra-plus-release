package com.cybertran.enquadraplus.data.update

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

object CanonicalJson {

    fun encode(value: Any?): String =
        buildString {
            appendValue(value)
        }

    fun encodeParsedJson(json: String): String =
        encode(toKotlinValue(JSONObject(json)))

    fun sha256Upper(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString(separator = "") { byte -> "%02X".format(byte) }

    fun sha256Upper(text: String): String =
        sha256Upper(text.toByteArray(Charsets.UTF_8))

    fun toKotlinValue(value: Any?): Any? =
        when (value) {
            null, JSONObject.NULL -> null
            is JSONObject -> value.keys().asSequence()
                .associateWith { key -> toKotlinValue(value.get(key)) }
            is JSONArray -> (0 until value.length()).map { index ->
                toKotlinValue(value.get(index))
            }
            else -> value
        }

    private fun StringBuilder.appendValue(value: Any?) {
        when (value) {
            null -> append("null")
            is String -> appendQuoted(value)
            is Boolean -> append(if (value) "true" else "false")
            is Byte, is Short, is Int, is Long -> append(value.toString())
            is Float -> appendFiniteNumber(value.toDouble())
            is Double -> appendFiniteNumber(value)
            is java.math.BigDecimal -> append(value.toPlainString())
            is java.math.BigInteger -> append(value.toString())
            is Map<*, *> -> appendObject(value)
            is Iterable<*> -> appendArray(value)
            is Array<*> -> appendArray(value.asIterable())
            is JSONObject -> appendValue(toKotlinValue(value))
            is JSONArray -> appendValue(toKotlinValue(value))
            else -> error("Tipo JSON canônico não suportado: ${value::class.java.name}")
        }
    }

    private fun StringBuilder.appendFiniteNumber(value: Double) {
        require(value.isFinite()) { "JSON canônico não aceita NaN ou infinito." }
        append(java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString())
    }

    private fun StringBuilder.appendObject(value: Map<*, *>) {
        val keys = value.keys.map { key ->
            require(key is String) { "Objeto JSON canônico exige chaves String." }
            key
        }.sorted()

        append('{')
        keys.forEachIndexed { index, key ->
            if (index > 0) append(',')
            appendQuoted(key)
            append(':')
            appendValue(value[key])
        }
        append('}')
    }

    private fun StringBuilder.appendArray(value: Iterable<*>) {
        append('[')
        value.forEachIndexed { index, item ->
            if (index > 0) append(',')
            appendValue(item)
        }
        append(']')
    }

    private fun StringBuilder.appendQuoted(value: String) {
        append('"')
        value.forEach { char ->
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (char.code < 0x20) {
                        append("\\u")
                        append(char.code.toString(16).padStart(4, '0'))
                    } else {
                        append(char)
                    }
                }
            }
        }
        append('"')
    }
}
