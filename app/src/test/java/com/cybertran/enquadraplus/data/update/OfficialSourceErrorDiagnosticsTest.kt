package com.cybertran.enquadraplus.data.update

import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialSourceErrorDiagnosticsTest {

    @Test
    fun describesConnectionResetWithoutClaimingItsCause() {
        val result = OfficialSourceErrorDiagnostics.describe(
            IOException("Retries exhausted", SocketException("Connection reset")),
        )
        assertTrue(result.contains("Conexão interrompida"))
    }

    @Test
    fun distinguishesDnsTimeoutAndTls() {
        assertTrue(OfficialSourceErrorDiagnostics.describe(UnknownHostException()).contains("DNS"))
        assertTrue(OfficialSourceErrorDiagnostics.describe(SocketTimeoutException()).contains("Tempo esgotado"))
        assertTrue(OfficialSourceErrorDiagnostics.describe(SSLHandshakeException("handshake")).contains("HTTPS/TLS"))
    }

    @Test
    fun keepsHttpResponseErrorsVisible() {
        assertTrue(
            OfficialSourceErrorDiagnostics.describe(IOException("Fonte oficial respondeu HTTP 403"))
                .contains("HTTP 403"),
        )
    }
}
