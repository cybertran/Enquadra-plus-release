package com.cybertran.enquadraplus.domain.model

data class OperationalGuide(
    val id: String,
    val title: String,
    val summary: String,
    val legalReferences: List<String>,
    val steps: List<String>,
    val normativeSearchTerms: List<String>,
)

object OperationalGuideCatalog {
    val entries: List<OperationalGuide> = listOf(
        OperationalGuide(
            id = "retencao-remocao",
            title = "Retenção, remoção e liberação do veículo",
            summary = "Fluxo de conferência da medida administrativa, sem substituir a ficha específica da infração.",
            legalReferences = listOf("CTB, arts. 269 a 271", "MBFT da infração aplicada"),
            steps = listOf(
                "Abra a ficha do enquadramento e confirme a medida administrativa prevista para a conduta.",
                "Diferencie retenção para regularização, remoção e eventual liberação conforme a situação concreta e a regra vigente.",
                "Registre no AIT somente os fatos efetivamente constatados e as providências adotadas.",
                "Quando houver dúvida sobre condição de liberação ou procedimento, consulte a norma oficial vinculada à ficha antes de concluir o fluxo.",
            ),
            normativeSearchTerms = listOf("retenção veículo", "remoção veículo", "art. 270", "art. 271"),
        ),
        OperationalGuide(
            id = "toxicologico",
            title = "Exame toxicológico",
            summary = "Roteiro para localizar exigibilidade, infração e consequência administrativa nas fontes atuais.",
            legalReferences = listOf("CTB, art. 148-A", "CTB, art. 165-B", "MBFT vigente"),
            steps = listOf(
                "Confirme a categoria da habilitação e a situação que torna o exame exigível.",
                "Pesquise a ficha vigente do código aplicável antes de qualquer autuação.",
                "Confira no catálogo normativo atos atuais relacionados ao exame toxicológico e à habilitação.",
                "Registre apenas dados verificáveis da abordagem ou consulta oficial utilizada.",
            ),
            normativeSearchTerms = listOf("toxicológico", "148-A", "165-B"),
        ),
        OperationalGuide(
            id = "capacete",
            title = "Capacete e motocicletas",
            summary = "Conferência rápida de condutor, passageiro, equipamento e conduta.",
            legalReferences = listOf("CTB, arts. 54, 55 e 244", "MBFT vigente"),
            steps = listOf(
                "Identifique se a irregularidade envolve condutor, passageiro ou característica do equipamento.",
                "Consulte a ficha correspondente e diferencie ausência, uso inadequado e outras hipóteses previstas no MBFT.",
                "Confira eventual medida administrativa e observação oficial exigida.",
                "Descreva no AIT o fato concreto observado, sem transformar orientação genérica em fato presumido.",
            ),
            normativeSearchTerms = listOf("capacete", "motocicleta", "art. 244"),
        ),
        OperationalGuide(
            id = "criancas",
            title = "Transporte de crianças",
            summary = "Fluxo para idade, posição no veículo, dispositivo de retenção e enquadramento.",
            legalReferences = listOf("CTB, arts. 64 e 168", "Normas CONTRAN vigentes sobre transporte de crianças"),
            steps = listOf(
                "Confirme idade e, quando relevante, altura da criança a partir dos dados disponíveis.",
                "Identifique posição ocupada no veículo e dispositivo de retenção efetivamente utilizado.",
                "Consulte a ficha MBFT e a norma vigente antes de concluir o enquadramento.",
                "Registre os elementos observáveis que justificam a constatação.",
            ),
            normativeSearchTerms = listOf("criança", "dispositivo de retenção", "art. 168"),
        ),
        OperationalGuide(
            id = "vagas-reservadas",
            title = "Vagas reservadas",
            summary = "Conferência de sinalização, credencial e uso indevido de vaga especial.",
            legalReferences = listOf("CTB, art. 181", "Normas CONTRAN vigentes sobre credenciais e vagas reservadas"),
            steps = listOf(
                "Confirme que a vaga está caracterizada pela sinalização aplicável.",
                "Verifique a presença e validade da credencial quando exigida.",
                "Selecione a ficha específica do art. 181 correspondente à situação constatada.",
                "Registre local, sinalização e circunstância objetiva relevante para a autuação.",
            ),
            normativeSearchTerms = listOf("vaga reservada", "credencial estacionamento", "art. 181"),
        ),
        OperationalGuide(
            id = "motofrete",
            title = "Motofrete e mototáxi",
            summary = "Roteiro para habilitação, veículo, equipamentos e requisitos da atividade.",
            legalReferences = listOf("CTB, arts. 139-A e 139-B", "CTB, art. 244", "Normas CONTRAN vigentes"),
            steps = listOf(
                "Identifique se a atividade observada é transporte remunerado de carga ou passageiro em motocicleta/motoneta.",
                "Confira requisitos do condutor e do veículo nas fontes oficiais vigentes.",
                "Verifique equipamentos ou características exigidas para a atividade.",
                "Use a ficha MBFT correspondente à irregularidade concreta; não trate a atividade, por si só, como infração.",
            ),
            normativeSearchTerms = listOf("motofrete", "mototáxi", "139-A", "139-B"),
        ),
        OperationalGuide(
            id = "carga-rack",
            title = "Carga externa, bagageiro e rack",
            summary = "Conferência de dimensões, fixação, visibilidade, iluminação e segurança.",
            legalReferences = listOf("CTB, arts. 102, 230 e 231", "Normas CONTRAN vigentes sobre transporte de cargas"),
            steps = listOf(
                "Observe como a carga está acondicionada e fixada ao veículo.",
                "Confira se há comprometimento de placas, luzes, visibilidade, estabilidade ou dimensões regulamentares.",
                "Pesquise a regra específica no catálogo normativo e a ficha MBFT aplicável.",
                "Descreva posição, dimensão ou condição efetivamente observada quando isso for necessário para caracterizar o fato.",
            ),
            normativeSearchTerms = listOf("carga externa", "bagageiro", "rack", "transporte de carga"),
        ),
        OperationalGuide(
            id = "licenciamento",
            title = "Licenciamento e CRLV",
            summary = "Fluxo rápido para validade do CRLV, pendências e calendário aplicável.",
            legalReferences = listOf("CTB, arts. 130 a 133", "CTB, art. 230, V", "Calendário oficial da UF"),
            steps = listOf(
                "Identifique a UF de registro e o final da placa.",
                "Consulte o calendário estadual auditado quando disponível; caso contrário, use a referência nacional e a fonte oficial da UF.",
                "Confirme a situação documental do veículo no canal oficial disponível para a fiscalização.",
                "Se houver irregularidade, consulte a ficha MBFT antes de lavrar o AIT e registrar a medida administrativa.",
            ),
            normativeSearchTerms = listOf("licenciamento", "CRLV", "art. 230 V"),
        ),
    )
}
