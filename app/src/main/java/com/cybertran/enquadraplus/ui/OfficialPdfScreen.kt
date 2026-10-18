package com.cybertran.enquadraplus.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OfficialPdfRequest(
    val url: String,
    val title: String,
    val initialPageIndex: Int = 0,
)

private data class PdfPageResult(
    val bitmap: Bitmap,
    val pageIndex: Int,
    val pageCount: Int,
    val file: File,
)

@Composable
fun OfficialPdfScreen(
    contentPadding: PaddingValues,
    request: OfficialPdfRequest,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var pageIndex by rememberSaveable(request.url) {
        mutableIntStateOf(request.initialPageIndex.coerceAtLeast(0))
    }
    var result by remember(request.url) { mutableStateOf<PdfPageResult?>(null) }
    var error by remember(request.url) { mutableStateOf<String?>(null) }
    var saveMessage by remember(request.url) { mutableStateOf<String?>(null) }
    var loading by remember(request.url) { mutableStateOf(true) }
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        val file = result?.file
        if (uri != null && file != null) {
            scope.launch {
                saveMessage = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                            FileInputStream(file).use { input ->
                                input.copyTo(output)
                            }
                        } ?: error("Destino indisponível.")
                    }
                    "PDF salvo."
                }.getOrElse {
                    "Não foi possível salvar o PDF."
                }
            }
        }
    }

    LaunchedEffect(request.url, pageIndex) {
        loading = true
        error = null
        result = runCatching {
            withContext(Dispatchers.IO) {
                loadOfficialPdfPage(
                    context = context,
                    url = request.url,
                    requestedPageIndex = pageIndex,
                )
            }
        }.onFailure {
            error = it.message ?: "Não foi possível abrir o PDF oficial."
        }.getOrNull()
        result?.let { pageIndex = it.pageIndex }
        loading = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = request.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            Text(
                text = "Documento oficial • gov.br",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (loading) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator()
                    Text("Carregando página oficial…")
                }
            }
        }

        error?.let { message ->
            item {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        result?.let { page ->
            item {
                Text(
                    text = "Página ${page.pageIndex + 1} de ${page.pageCount}",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            item {
                Image(
                    bitmap = page.bitmap.asImageBitmap(),
                    contentDescription = request.title,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        enabled = page.pageIndex > 0 && !loading,
                        onClick = { pageIndex = (page.pageIndex - 1).coerceAtLeast(0) },
                    ) {
                        Text("Página anterior")
                    }
                    Button(
                        enabled = page.pageIndex < page.pageCount - 1 && !loading,
                        onClick = {
                            pageIndex = (page.pageIndex + 1).coerceAtMost(page.pageCount - 1)
                        },
                    ) {
                        Text("Próxima página")
                    }
                    OutlinedButton(
                        onClick = {
                            shareOfficialPdf(
                                context = context,
                                file = page.file,
                                title = request.title,
                            )
                        },
                    ) {
                        Text("Compartilhar PDF")
                    }
                    OutlinedButton(
                        onClick = {
                            saveLauncher.launch(suggestedPdfFileName(request.title))
                        },
                    ) {
                        Text("Salvar cópia")
                    }
                }
            }
        }

        saveMessage?.let { message ->
            item {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            OutlinedButton(
                onClick = { uriHandler.openUri(request.url) },
            ) {
                Text("Abrir no navegador")
            }
        }
    }
}

internal fun validateOfficialPdfUrl(url: String): URI {
    val uri = URI(url)
    val host = uri.host?.lowercase()
    require(uri.scheme.equals("https", ignoreCase = true)) {
        "Somente PDFs oficiais via HTTPS são permitidos."
    }
    require(host in setOf("gov.br", "www.gov.br")) {
        "Origem do PDF não permitida."
    }
    require(uri.port == -1 || uri.port == 443) {
        "Porta do PDF oficial não permitida."
    }
    require(uri.userInfo == null && uri.fragment == null) {
        "URL do PDF oficial inválida."
    }
    require(uri.path.startsWith("/transportes/pt-br/assuntos/transito/")) {
        "Caminho do PDF oficial não permitido."
    }
    require(uri.path.endsWith(".pdf", ignoreCase = true)) {
        "O documento oficial precisa ser um PDF."
    }
    return uri
}

private fun loadOfficialPdfPage(
    context: Context,
    url: String,
    requestedPageIndex: Int,
): PdfPageResult {
    val file = cachedOfficialPdf(context, url)
    val descriptor = ParcelFileDescriptor.open(
        file,
        ParcelFileDescriptor.MODE_READ_ONLY,
    )
    val renderer = PdfRenderer(descriptor)

    try {
        require(renderer.pageCount > 0) { "O PDF oficial não possui páginas." }
        val safeIndex = requestedPageIndex.coerceIn(0, renderer.pageCount - 1)
        val page = renderer.openPage(safeIndex)
        try {
            val targetWidth = minOf(PDF_RENDER_WIDTH, page.width.coerceAtLeast(1) * 2)
            val targetHeight = (
                page.height.toDouble() *
                    targetWidth.toDouble() /
                    page.width.coerceAtLeast(1).toDouble()
                ).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(
                targetWidth,
                targetHeight,
                Bitmap.Config.ARGB_8888,
            )
            page.render(
                bitmap,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
            )
            return PdfPageResult(
                bitmap = bitmap,
                pageIndex = safeIndex,
                pageCount = renderer.pageCount,
                file = file,
            )
        } finally {
            page.close()
        }
    } finally {
        renderer.close()
        descriptor.close()
    }
}

private fun cachedOfficialPdf(
    context: Context,
    url: String,
): File {
    var current = validateOfficialPdfUrl(url)
    val cacheName = "official-" + sha256(url) + ".pdf"
    val cacheDirectory = File(context.cacheDir, OFFICIAL_PDF_CACHE_DIR).apply {
        require(exists() || mkdirs()) {
            "Não foi possível preparar o cache de PDFs oficiais."
        }
    }
    val cached = File(cacheDirectory, cacheName)

    if (cached.isFile && cached.length() in 5..MAX_PDF_BYTES && hasPdfHeader(cached)) {
        return cached
    }

    repeat(MAX_REDIRECTS + 1) { redirectIndex ->
        val connection = current.toURL().openConnection() as HttpsURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("User-Agent", "EnquadraPlus/0.17")
        connection.setRequestProperty("Accept", "application/pdf")

        try {
            val status = connection.responseCode
            if (status in 300..399) {
                require(redirectIndex < MAX_REDIRECTS) {
                    "Muitos redirecionamentos ao abrir o PDF oficial."
                }
                val location = connection.getHeaderField("Location")
                    ?: error("Redirecionamento oficial sem destino.")
                current = validateOfficialPdfUrl(current.resolve(location).toString())
                return@repeat
            }

            require(status in 200..299) {
                "Fonte oficial respondeu HTTP $status."
            }

            val declaredLength = connection.contentLengthLong
            require(declaredLength <= MAX_PDF_BYTES || declaredLength < 0) {
                "PDF oficial excede o limite de tamanho."
            }

            val temp = File.createTempFile("official-", ".part", cacheDirectory)
            try {
                connection.inputStream.use { input ->
                    FileOutputStream(temp).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var total = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            require(total <= MAX_PDF_BYTES) {
                                "PDF oficial excede o limite de tamanho."
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }

                require(hasPdfHeader(temp)) {
                    "A fonte oficial não retornou um arquivo PDF válido."
                }

                if (cached.exists()) cached.delete()
                require(temp.renameTo(cached)) {
                    "Não foi possível armazenar o PDF oficial em cache."
                }
                return cached
            } finally {
                if (temp.exists()) temp.delete()
            }
        } finally {
            connection.disconnect()
        }
    }

    error("Não foi possível obter o PDF oficial.")
}

private fun shareOfficialPdf(
    context: Context,
    file: File,
    title: String,
) {
    require(file.isFile && hasPdfHeader(file)) {
        "Somente o PDF oficial validado pode ser compartilhado."
    }
    val uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar PDF oficial"))
}

internal fun suggestedPdfFileName(title: String): String {
    val base = title
        .trim()
        .replace(Regex("[^A-Za-z0-9À-ÿ._ -]+"), " ")
        .replace(Regex("\\s+"), " ")
        .take(80)
        .trim()
        .ifBlank { "documento-oficial" }

    return if (base.endsWith(".pdf", ignoreCase = true)) base else "$base.pdf"
}

private fun hasPdfHeader(file: File): Boolean {
    if (!file.isFile || file.length() < 5) return false
    val header = ByteArray(5)
    val read = FileInputStream(file).use { it.read(header) }
    return read == 5 && header.decodeToString() == "%PDF-"
}

private fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

private const val OFFICIAL_PDF_CACHE_DIR = "official-pdfs"
private const val PDF_RENDER_WIDTH = 1200
private const val MAX_PDF_BYTES = 50L * 1024L * 1024L
private const val MAX_REDIRECTS = 3
private const val CONNECT_TIMEOUT_MS = 15_000
private const val READ_TIMEOUT_MS = 30_000
