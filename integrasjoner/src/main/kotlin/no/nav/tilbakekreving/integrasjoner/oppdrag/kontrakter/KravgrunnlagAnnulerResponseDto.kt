package no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter

data class KravgrunnlagAnnulerResponseDto(
    val status: Int,
    val melding: String,
    val vedtakId: Int,
    val saksbehandlerId: String,
)
