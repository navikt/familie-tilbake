package no.nav.tilbakekreving.entities

data class EksternFagsakEntity(
    val eksternId: String,
    val ytelsestype: String,
    val fagsystem: String,
    val behandlinger: EksternFagsakBehandlingHistorikkEntity,
)
