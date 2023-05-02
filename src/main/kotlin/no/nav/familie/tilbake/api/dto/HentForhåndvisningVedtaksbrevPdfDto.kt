package no.nav.familie.tilbake.api.dto

import java.util.UUID
import javax.validation.Valid
import javax.validation.constraints.Size

class HentForhåndvisningVedtaksbrevPdfDto(
    var behandlingId: UUID,
    @Size(max = 10000, message = "Oppsummeringstekst er for lang")
    var oppsummeringstekst: String? = null,
    @Size(max = 100, message = "For mange perioder") @Valid
    var perioderMedTekst: List<PeriodeMedTekstDto>,
)
