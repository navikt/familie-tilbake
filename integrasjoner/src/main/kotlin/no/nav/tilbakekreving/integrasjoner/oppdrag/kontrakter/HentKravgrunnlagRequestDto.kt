package no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter

data class HentKravgrunnlagRequestDto(
    val kodeAksjon: KodeAksjonDto,
    val gjelderId: String,
    val typeGjelder: String,
    val utbetalesTilId: String,
    val typeUtbetalesTil: String,
    val enhetAnsvarlig: String,
    val kodeFaggruppe: String,
    val kodeFagomraade: String,
    val fagsystemId: String,
    val saksbehandlerId: String,
    val kravgrunnlagId: Int? = null,
)
