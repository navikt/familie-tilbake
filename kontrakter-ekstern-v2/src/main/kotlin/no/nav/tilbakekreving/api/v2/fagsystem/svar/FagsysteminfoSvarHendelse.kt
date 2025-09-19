package no.nav.tilbakekreving.api.v2.fagsystem.svar

import no.nav.tilbakekreving.api.v2.MottakerDto
import no.nav.tilbakekreving.api.v2.PeriodeDto
import no.nav.tilbakekreving.api.v2.fagsystem.EventMetadata
import java.time.LocalDate
import java.time.LocalDateTime

data class FagsysteminfoSvarHendelse(
    override val eksternFagsakId: String,
    override val hendelseOpprettet: LocalDateTime,
    val mottaker: MottakerDto,
    val revurdering: RevurderingDto,
) : KafkameldingFraFagsystem {
    data class UtvidetPeriodeDto(
        val kravgrunnlagPeriode: PeriodeDto,
        val vedtaksperiode: PeriodeDto,
    )

    data class RevurderingDto(
        val behandlingId: String,
        val årsak: Årsak,
        val årsakTilFeilutbetaling: String?,
        val vedtaksdato: LocalDate,
        val utvidPerioder: List<UtvidetPeriodeDto>?,
    ) {
        enum class Årsak {
            NYE_OPPLYSNINGER,
            KORRIGERING,
            KLAGE,
            UKJENT,
        }
    }

    companion object {
        val METADATA = EventMetadata(
            hendelsestype = "fagsysteminfo_svar",
            versjon = 1,
        )
    }
}
