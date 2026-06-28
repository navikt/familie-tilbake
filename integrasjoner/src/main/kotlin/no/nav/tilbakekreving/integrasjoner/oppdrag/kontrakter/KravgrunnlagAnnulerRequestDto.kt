package no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter

data class KravgrunnlagAnnulerRequestDto(
    val kodeAksjon: KodeAksjonDto,
    val vedtakId: Int,
    val enhetAnsvarlig: String,
    val saksbehandlerId: String,
)
