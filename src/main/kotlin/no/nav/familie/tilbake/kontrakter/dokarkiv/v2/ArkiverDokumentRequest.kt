package no.nav.familie.tilbake.kontrakter.dokarkiv.v2

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import no.nav.familie.tilbake.kontrakter.dokarkiv.AvsenderMottaker

data class ArkiverDokumentRequest(
    @field:NotBlank val fnr: String,
    val forsøkFerdigstill: Boolean,
    @field:NotEmpty val hoveddokumentvarianter: List<Dokument>,
    val vedleggsdokumenter: List<Dokument> = emptyList(),
    val fagsakId: String? = null,
    // Skal ikke settes for innkommende hvis Ruting gjøres av BRUT001
    val journalførendeEnhet: String? = null,
    // Ønsker du å generer forside når du tilskriver bruker kan du fylle ut denne
    val førsteside: Førsteside? = null,
    val eksternReferanseId: String? = null,
    val avsenderMottaker: AvsenderMottaker? = null,
)
