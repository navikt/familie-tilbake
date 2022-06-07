package no.nav.familie.tilbake.integration.pdl.internal

data class PdlHentIdenterResponse(
    val data: Data,
    override val errors: List<PdlError>?
) :
    PdlBaseResponse(errors)

data class Data(val pdlIdenter: PdlIdenter?)

data class PdlIdenter(val identer: List<IdentInformasjon>)

data class IdentInformasjon(
    val ident: String,
    val gruppe: String
)
