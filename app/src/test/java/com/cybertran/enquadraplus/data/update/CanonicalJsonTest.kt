package com.cybertran.enquadraplus.data.update

import org.junit.Assert.assertEquals
import org.junit.Test

class CanonicalJsonTest {

    @Test
    fun encodeSortsObjectKeysAndEscapesTextDeterministically() {
        val encoded = CanonicalJson.encode(
            linkedMapOf(
                "b" to 2,
                "a" to "x\n",
            ),
        )

        assertEquals("{\"a\":\"x\\n\",\"b\":2}", encoded)
        assertEquals(
            "83B298DEA3B7A02FEF2F0D52D2B3C96BA06A71AABFD7AD10F8BD03DE91A3A7C1",
            CanonicalJson.sha256Upper(encoded),
        )
    }

    @Test
    fun officialFetchDecodesWindows1252WithoutReplacementCharacters() {
        val text = "Resolução com ação"
        val bytes = text.toByteArray(java.nio.charset.Charset.forName("windows-1252"))
        val fetched = OfficialFetchResult(
            requestedUrl = "https://example.invalid",
            finalUrl = "https://example.invalid",
            contentType = "text/html",
            bytes = bytes,
            sha256 = CanonicalJson.sha256Upper(bytes),
        )

        assertEquals(text, fetched.decodeText())
    }

    @Test
    fun insertionOrderDoesNotChangeCanonicalRepresentation() {
        val first = linkedMapOf<String, Any?>("z" to 1, "a" to listOf(2, 3))
        val second = linkedMapOf<String, Any?>("a" to listOf(2, 3), "z" to 1)

        assertEquals(CanonicalJson.encode(first), CanonicalJson.encode(second))
    }
}
