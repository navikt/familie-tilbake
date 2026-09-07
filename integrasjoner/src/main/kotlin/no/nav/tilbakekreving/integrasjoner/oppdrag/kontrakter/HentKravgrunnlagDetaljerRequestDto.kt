package no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter

data class HentKravgrunnlagDetaljerRequestDto(
    val kodeAksjon: KodeAksjonDto,
    val kravgrunnlagId: Int,
    val enhetAnsvarlig: String,
    val saksbehandlerId: String,
)
