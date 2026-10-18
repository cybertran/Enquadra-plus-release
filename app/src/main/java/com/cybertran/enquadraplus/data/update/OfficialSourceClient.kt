package com.cybertran.enquadraplus.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class SourceUnavailableException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

class OfficialSourceClient {

    suspend fun fetch(spec: OfficialSourceSpec): OfficialFetchResult =
        withContext(Dispatchers.IO) {
            var lastError: Throwable? = null

            repeat(MAX_ATTEMPTS) { attempt ->
                try {
                    return@withContext fetchOnce(spec)
                } catch (error: IOException) {
                    lastError = error
                    if (attempt + 1 < MAX_ATTEMPTS) {
                        delay((1L shl attempt).coerceAtMost(4L) * 1_000L)
                    }
                }
            }

            throw SourceUnavailableException(
                "Não foi possível obter a fonte oficial após $MAX_ATTEMPTS tentativas.",
                lastError,
            )
        }

    private fun fetchOnce(spec: OfficialSourceSpec): OfficialFetchResult {
        var currentUrl = spec.url
        OfficialSourcePolicy.validate(currentUrl, spec.kind)

        repeat(MAX_REDIRECTS + 1) { redirectIndex ->
            val connection = openConnection(currentUrl, spec.acceptedContentTypes)
            try {
                val status = connection.responseCode

                if (status in REDIRECT_CODES) {
                    require(redirectIndex < MAX_REDIRECTS) {
                        "Fonte oficial excedeu o limite de redirecionamentos."
                    }
                    val location = connection.getHeaderField("Location")
                        ?: throw SourceUnavailableException(
                            "Fonte oficial redirecionou sem informar destino.",
                        )
                    currentUrl = OfficialSourcePolicy.resolveRedirect(
                        currentUrl = currentUrl,
                        location = location,
                        kind = spec.kind,
                    )
                    return@repeat
                }

                if (status != HttpURLConnection.HTTP_OK) {
                    throw SourceUnavailableException(
                        "Fonte oficial respondeu HTTP $status.",
                    )
                }

                OfficialSourcePolicy.validate(currentUrl, spec.kind)

                val contentType = connection.contentType.orEmpty()
                val mediaType = contentType.substringBefore(';').trim().lowercase()
                require(mediaType in spec.acceptedContentTypes) {
                    "Tipo de conteúdo inesperado na fonte oficial: $contentType"
                }

                val declaredSize = connection.contentLengthLong
                require(declaredSize <= spec.maxBytes || declaredSize < 0) {
                    "Fonte oficial excede o limite de tamanho configurado."
                }

                val bytes = connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream(
                        if (declaredSize in 1..spec.maxBytes.toLong()) {
                            declaredSize.toInt()
                        } else {
                            32 * 1024
                        },
                    )
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= spec.maxBytes) {
                            "Fonte oficial excedeu o limite de tamanho durante o download."
                        }
                        output.write(buffer, 0, read)
                    }
                    output.toByteArray()
                }

                require(bytes.size >= spec.minBytes) {
                    "Fonte oficial menor que o mínimo esperado: ${bytes.size} bytes."
                }

                return OfficialFetchResult(
                    requestedUrl = spec.url,
                    finalUrl = currentUrl,
                    contentType = contentType,
                    bytes = bytes,
                    sha256 = CanonicalJson.sha256Upper(bytes),
                )
            } finally {
                connection.disconnect()
            }
        }

        throw SourceUnavailableException("Redirecionamento oficial não resolvido.")
    }

    private fun openConnection(
        url: String,
        acceptedContentTypes: Set<String>,
    ): HttpsURLConnection =
        (URL(url).openConnection() as HttpsURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            useCaches = false
            requestMethod = "GET"
            setRequestProperty(
                "User-Agent",
                "EnquadraPlus/0.7 Android; atualização normativa oficial",
            )
            setRequestProperty(
                "Accept",
                acceptedContentTypes.joinToString(","),
            )
            setRequestProperty("Accept-Language", "pt-BR,pt;q=0.9")
            setRequestProperty("Connection", "close")
        }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 20_000
        const val READ_TIMEOUT_MS = 60_000
        const val MAX_ATTEMPTS = 3
        const val MAX_REDIRECTS = 3
        val REDIRECT_CODES = setOf(
            HttpURLConnection.HTTP_MOVED_PERM,
            HttpURLConnection.HTTP_MOVED_TEMP,
            HttpURLConnection.HTTP_SEE_OTHER,
            307,
            308,
        )
    }
}
