package no.nav.tilbakekreving.integrasjoner.oppdrag

data class HentKravgrunnlagDetaljerRequestDto(
    val kodeAksjon: String,
    val kravgrunnlagId: Int,
    val enhetAnsvarlig: String,
    val saksbehandlerId: String,
)
