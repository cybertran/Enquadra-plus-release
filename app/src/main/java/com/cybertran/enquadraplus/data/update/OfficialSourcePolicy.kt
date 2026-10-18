package com.cybertran.enquadraplus.data.update

import java.net.URI

enum class OfficialSourceKind {
    CTB_HTML,
    TRANSPORT_HTML,
    MBFT_PDF,
}

data class OfficialSourceSpec(
    val url: String,
    val kind: OfficialSourceKind,
    val minBytes: Int,
    val maxBytes: Int,
    val acceptedContentTypes: Set<String> = setOf("text/html", "application/xhtml+xml"),
)

object OfficialSourcePolicy {

    const val CTB_URL =
        "https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm"

    const val CONTRAN_INDEX_URL =
        "https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-Senatran/resolucoes-contran"

    const val CONTRAN_DELIBERATIONS_INDEX_URL =
        "https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/deliberacoes-contran"

    fun validate(url: String, kind: OfficialSourceKind): URI {
        val uri = runCatching { URI(url) }
            .getOrElse { throw IllegalArgumentException("URL oficial inválida.", it) }

        require(uri.scheme.equals("https", ignoreCase = true)) {
            "Fonte oficial deve usar HTTPS."
        }
        require(uri.userInfo == null) {
            "Fonte oficial não pode conter credenciais na URL."
        }
        require(uri.port == -1 || uri.port == 443) {
            "Fonte oficial usa porta não autorizada."
        }
        require(uri.fragment == null) {
            "Fonte oficial não pode conter fragmento."
        }
        require(uri.rawPath?.contains("..") != true) {
            "Fonte oficial contém caminho relativo não permitido."
        }

        val host = uri.host?.lowercase()
            ?: throw IllegalArgumentException("Fonte oficial sem host.")

        when (kind) {
            OfficialSourceKind.CTB_HTML -> {
                require(host in setOf("planalto.gov.br", "www.planalto.gov.br")) {
                    "CTB deve ser obtido exclusivamente do Planalto."
                }
                require(uri.path.equals("/ccivil_03/leis/l9503compilado.htm", ignoreCase = true)) {
                    "Caminho oficial do CTB não reconhecido."
                }
                require(uri.rawQuery == null) {
                    "URL do CTB não deve conter parâmetros."
                }
            }

            OfficialSourceKind.TRANSPORT_HTML -> {
                require(host in setOf("gov.br", "www.gov.br")) {
                    "Fonte normativa deve pertencer ao domínio oficial gov.br."
                }
                require(uri.path.startsWith("/transportes/pt-br/assuntos/transito/")) {
                    "Fonte normativa fora da área oficial de trânsito."
                }
                require(uri.rawQuery == null) {
                    "Página-fonte normativa não deve conter parâmetros."
                }
            }

            OfficialSourceKind.MBFT_PDF -> {
                require(host in setOf("gov.br", "www.gov.br")) {
                    "Ato MBFT deve pertencer ao domínio oficial gov.br."
                }
                require(
                    uri.path ==
                        "/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/resolucao10312026.pdf",
                ) {
                    "PDF MBFT não consta entre os artefatos auditados por esta versão."
                }
                require(uri.rawQuery == null) {
                    "PDF MBFT não deve conter parâmetros."
                }
            }

        }

        return uri
    }

    fun resolveRedirect(
        currentUrl: String,
        location: String,
        kind: OfficialSourceKind,
    ): String {
        require(location.isNotBlank()) { "Redirecionamento sem destino." }
        val current = validate(currentUrl, kind)
        val resolved = current.resolve(location).toString()
        validate(resolved, kind)
        return resolved
    }

    fun normativeSources(currentYear: Int): List<OfficialSourceSpec> {
        require(currentYear in 2026..2100) {
            "Ano corrente fora da faixa suportada pelo catálogo normativo."
        }

        val sources = mutableListOf(
            OfficialSourceSpec(
                url = CONTRAN_INDEX_URL,
                kind = OfficialSourceKind.TRANSPORT_HTML,
                minBytes = 5_000,
                maxBytes = 8_000_000,
            ),
            OfficialSourceSpec(
                url = CONTRAN_DELIBERATIONS_INDEX_URL,
                kind = OfficialSourceKind.TRANSPORT_HTML,
                minBytes = 5_000,
                maxBytes = 8_000_000,
            ),
        )

        for (year in 2021..currentYear) {
            val path = if (year >= 2024) {
                "senatran/portarias-$year"
            } else {
                "conteudo-Senatran/portarias-$year"
            }
            sources += OfficialSourceSpec(
                url = "https://www.gov.br/transportes/pt-br/assuntos/transito/$path",
                kind = OfficialSourceKind.TRANSPORT_HTML,
                minBytes = 2_000,
                maxBytes = 8_000_000,
            )
        }

        return sources
    }

    fun ctbSource(): OfficialSourceSpec =
        OfficialSourceSpec(
            url = CTB_URL,
            kind = OfficialSourceKind.CTB_HTML,
            minBytes = 100_000,
            maxBytes = 10_000_000,
        )

    fun mbftResolutionSource(url: String): OfficialSourceSpec =
        OfficialSourceSpec(
            url = url,
            kind = OfficialSourceKind.MBFT_PDF,
            minBytes = 100_000,
            maxBytes = 12_000_000,
            acceptedContentTypes = setOf("application/pdf"),
        ).also { validate(it.url, it.kind) }

}
