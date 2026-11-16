package no.nav.tilbakekreving.api.v1.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.util.UUID

class HentForhåndvisningVedtaksbrevPdfDto(
    var behandlingId: UUID,
    @Size(max = 10000, message = "Oppsummeringstekst er for lang")
    var oppsummeringstekst: String? = null,
    @Size(max = 100, message = "For mange perioder")
    var perioderMedTekst: List<@Valid PeriodeMedTekstDto>,
)
