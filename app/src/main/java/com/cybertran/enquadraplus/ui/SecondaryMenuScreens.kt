package com.cybertran.enquadraplus.ui

import android.Manifest
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cybertran.enquadraplus.R
import com.cybertran.enquadraplus.data.feedback.FeedbackContextProvider
import com.cybertran.enquadraplus.data.update.UpdateNotificationManager
import com.cybertran.enquadraplus.ui.update.AppUpdateStatus
import com.cybertran.enquadraplus.ui.update.AppUpdateViewModel

private const val PROJECT_URL = "https://github.com/cybertran/Enquadra-plus-release"
private const val FEEDBACK_URL = "https://docs.google.com/forms/d/e/1FAIpQLSf5B-n0P-p6k-n71mOsqf5P15R-n3V9yB7pI7c0wX3fQ3q_xQ/viewform"
private const val FEEDBACK_CONTEXT_ENTRY = "entry.1038453569"
private const val DEVELOPER_EMAIL = "cybertran@proton.me"
private const val DEVELOPER_NAME = "Wanderson Maike Campos Pinto"

@Composable
fun AppSettingsScreen(
    contentPadding: PaddingValues,
    onOpenUpdates: () -> Unit,
) {
    val context = LocalContext.current
    fun notificationPermissionGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    var permissionGranted by remember { mutableStateOf(notificationPermissionGranted()) }
    var notificationsAllowed by remember {
        mutableStateOf(UpdateNotificationManager.notificationsAllowed(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted || notificationPermissionGranted()
        notificationsAllowed = UpdateNotificationManager.notificationsAllowed(context)
    }

    SecondaryScreen(contentPadding = contentPadding) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Base jurídica e atualizações",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Verificar e aplicar atualizações validadas do CTB, MBFT e catálogo CONTRAN/SENATRAN.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenUpdates,
                ) {
                    Text("Gerenciar atualizações")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Alertas de atualização",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = when {
                        notificationsAllowed ->
                            "Ativados. O app pode avisar quando uma candidata normativa validada estiver pronta para sua decisão."
                        permissionGranted ->
                            "A permissão foi concedida, mas as notificações do aplicativo estão desativadas nas configurações do Android."
                        else ->
                            "Desativados. O app continua verificando as fontes oficiais sem enviar alertas."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !permissionGranted
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                    ) {
                        Text("Permitir alertas")
                    }
                }
                Text(
                    text = "O alerta nunca aplica uma atualização automaticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun AboutAppScreen(
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val packageInfo = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val feedbackUrl = remember(context, packageInfo) {
        val installationId = FeedbackContextProvider.installationId(context)
        val diagnosticPayload = FeedbackContextProvider.diagnosticPayload(
            installationId = installationId,
            versionName = packageInfo.versionName ?: "não informada",
            versionCode = packageInfo.versionCodeCompat(),
        )
        FeedbackContextProvider.prefilledUrl(
            baseUrl = FEEDBACK_URL,
            entryKey = FEEDBACK_CONTEXT_ENTRY,
            diagnosticPayload = diagnosticPayload,
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AboutBrandHeader(packageInfo)
        }

        item {
            InfoCard(
                symbol = "ⓘ",
                title = "Sobre o Enquadra+",
                body = "Aplicativo Android de apoio à consulta e fiscalização de trânsito, com conteúdo estruturado a partir de fontes públicas oficiais.",
            )
        }

        item {
            InfoCard(
                symbol = "↗",
                title = "Finalidade",
                body = "Reduzir consultas manuais e reunir, em um fluxo rápido, infrações, CTB, MBFT, normas e ferramentas operacionais. As consultas principais permanecem disponíveis localmente no dispositivo.",
            )
        }

        item {
            InfoCard(
                symbol = "✎",
                title = "Feedback",
                body = "Envie uma sugestão, relate um problema ou informe conteúdo incorreto. Você pode responder sem se identificar. Ao abrir o formulário pelo app, um identificador aleatório da instalação e a versão do Enquadra+ podem ser incluídos para ajudar no diagnóstico.",
                onClick = { uriHandler.openUri(feedbackUrl) },
                actionText = "Enviar feedback",
            )
        }

        item {
            InfoCard(
                symbol = "</>",
                title = "Projeto",
                body = "O código-fonte, o histórico de versões e a documentação de desenvolvimento ficam concentrados no GitHub.",
                onClick = { uriHandler.openUri(PROJECT_URL) },
                actionText = "Abrir repositório do projeto",
            )
        }

        item {
            TechnicalInfoCard(
                packageInfo = packageInfo,
                packageName = context.packageName,
            )
        }

        item {
            InfoCard(
                symbol = "◇",
                title = "Privacidade e dados",
                body = "O Enquadra+ é local-first e não depende de conta ou servidor próprio para as consultas principais. Ao abrir o feedback pelo app, é incluído um identificador aleatório da instalação, sem uso de ANDROID_ID, IMEI, conta Google, telefone ou identificadores de publicidade, além da versão instalada. O e-mail no formulário continua opcional.",
            )
        }

        item {
            InfoCard(
                symbol = "§",
                title = "Bases oficiais",
                body = "CTB, MBFT e catálogo CONTRAN/SENATRAN são mantidos com origem identificável. Uma atualização remota só é aplicada depois de validações determinísticas; em caso de dúvida, a última base validada continua em uso.",
            )
        }
    }
}

@Composable
fun VersionInfoScreen(
    contentPadding: PaddingValues,
    appUpdateViewModel: AppUpdateViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val packageInfo = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val updateState by appUpdateViewModel.state.collectAsStateWithLifecycle()
    val installedVersionCode = packageInfo.versionCodeCompat()

    SecondaryScreen(contentPadding = contentPadding) {
        InfoCard(
            symbol = "v",
            title = "Versão instalada",
            body = buildString {
                append(packageInfo.versionName ?: "Não informada")
                append("\nBuild ")
                append(installedVersionCode)
            },
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CardTitle(
                    symbol = "↻",
                    title = "Atualização do aplicativo",
                )
                HorizontalDivider()

                Text(
                    text = when (updateState.status) {
                        AppUpdateStatus.IDLE ->
                            "Consulte se existe uma versão oficial mais recente do Enquadra+."
                        AppUpdateStatus.CHECKING ->
                            "Consultando a versão oficial publicada…"
                        AppUpdateStatus.UP_TO_DATE ->
                            "Você está usando a versão mais recente disponível."
                        AppUpdateStatus.UPDATE_AVAILABLE ->
                            "Nova versão disponível: " +
                                (updateState.release?.versionName ?: "versão mais recente") +
                                "."
                        AppUpdateStatus.ERROR ->
                            "Não foi possível verificar a atualização agora. Sua versão instalada não foi alterada."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )

                when (updateState.status) {
                    AppUpdateStatus.UPDATE_AVAILABLE -> {
                        val release = updateState.release
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = release != null,
                            onClick = {
                                release?.let { uriHandler.openUri(it.apkUrl) }
                            },
                        ) {
                            Text("Baixar atualização")
                        }
                        Text(
                            text = "O download é aberto no GitHub. A instalação depende da confirmação do Android e nunca é feita silenciosamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AppUpdateStatus.CHECKING -> {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            onClick = {},
                        ) {
                            Text("Verificando…")
                        }
                    }
                    else -> {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                appUpdateViewModel.checkNow(installedVersionCode)
                            },
                        ) {
                            Text(
                                if (updateState.status == AppUpdateStatus.ERROR) {
                                    "Tentar novamente"
                                } else {
                                    "Verificar atualização"
                                },
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "A atualização do aplicativo é independente das atualizações jurídicas. As versões das bases e a última verificação ficam em Configurações > Base jurídica e atualizações.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun DeveloperInfoScreen(
    contentPadding: PaddingValues,
    onOpenUpdates: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    SecondaryScreen(contentPadding = contentPadding) {
        InfoCard(
            symbol = "●",
            title = "Desenvolvimento",
            body = "$DEVELOPER_NAME\n$DEVELOPER_EMAIL",
            onClick = {
                uriHandler.openUri("mailto:$DEVELOPER_EMAIL")
            },
            actionText = "Enviar e-mail",
        )
        InfoCard(
            symbol = "</>",
            title = "Projeto",
            body = "Código-fonte, histórico de versões e documentação do Enquadra+.",
            onClick = {
                uriHandler.openUri(PROJECT_URL)
            },
            actionText = "Abrir repositório no GitHub",
        )
        InfoCard(
            symbol = "⚙",
            title = "Diagnóstico das bases",
            body = "Versões instaladas, diferenças, rollback e outros detalhes técnicos ficam concentrados na área de atualizações.",
            onClick = onOpenUpdates,
            actionText = "Abrir detalhes das bases",
        )
    }
}

@Composable
private fun AboutBrandHeader(
    packageInfo: PackageInfo,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.size(92.dp),
            shape = RoundedCornerShape(24.dp),
            color = colorResource(R.color.enquadra_launcher_background),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
        ) {
            Image(
                painter = painterResource(R.drawable.enquadra_launcher_art),
                contentDescription = "Ícone do Enquadra+",
                modifier = Modifier
                    .fillMaxSize(),
            )
        }

        Text(
            text = "Enquadra+",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Consulta prática de trânsito",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Versão ${packageInfo.versionName ?: "—"} • Build ${packageInfo.versionCodeCompat()}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun TechnicalInfoCard(
    packageInfo: PackageInfo,
    packageName: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CardTitle(
                symbol = "⚙",
                title = "Informações técnicas",
            )
            HorizontalDivider()

            TechnicalRow(
                label = "Versão",
                value = packageInfo.versionName ?: "—",
            )
            TechnicalRow(
                label = "Build",
                value = packageInfo.versionCodeCompat().toString(),
            )
            TechnicalRow(
                label = "Pacote",
                value = packageName,
            )
            TechnicalRow(
                label = "Ano",
                value = "2026",
            )
        }
    }
}

@Composable
private fun TechnicalRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SecondaryScreen(
    contentPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    symbol: String? = null,
    onClick: (() -> Unit)? = null,
    actionText: String? = null,
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardTitle(
                symbol = symbol,
                title = title,
            )
            HorizontalDivider()
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (actionText != null) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun CardTitle(
    symbol: String?,
    title: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (symbol != null) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Suppress("DEPRECATION")
private fun PackageInfo.versionCodeCompat(): Long =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        longVersionCode
    } else {
        versionCode.toLong()
    }
