package no.nav.familie.tilbake.kontrakter.dokarkiv

import jakarta.validation.constraints.NotBlank
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.DokumentInfo

data class ArkiverDokumentResponse(
    @field:NotBlank val journalpostId: String,
    val ferdigstilt: Boolean,
    val dokumenter: List<DokumentInfo>? = null,
)
