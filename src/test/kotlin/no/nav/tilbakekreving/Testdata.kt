package no.nav.tilbakekreving

import no.nav.familie.tilbake.data.Testdata
import no.nav.tilbakekreving.api.v2.MottakerDto
import no.nav.tilbakekreving.api.v2.PeriodeDto
import no.nav.tilbakekreving.api.v2.fagsystem.svar.FagsysteminfoSvarHendelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object Testdata {
    val TESTBRUKER = "20046912345"

    fun fagsysteminfoSvar(
        fagsystemId: String,
        eksternBehandlingId: String = UUID.randomUUID().toString(),
        utvidPerioder: List<FagsysteminfoSvarHendelse.UtvidetPeriodeDto> = listOf(
            FagsysteminfoSvarHendelse.UtvidetPeriodeDto(
                kravgrunnlagPeriode = PeriodeDto(fom = 1.januar(2021), tom = 1.januar(2021)),
                vedtaksperiode = PeriodeDto(fom = 1.januar(2021), tom = 31.januar(2021)),
            ),
        ),
    ) = FagsysteminfoSvarHendelse(
        eksternFagsakId = fagsystemId,
        hendelseOpprettet = LocalDateTime.now(),
        mottaker = MottakerDto(
            ident = Testdata.STANDARD_BRUKERIDENT,
            type = MottakerDto.MottakerType.PERSON,
        ),
        revurdering = FagsysteminfoSvarHendelse.RevurderingDto(
            behandlingId = eksternBehandlingId,
            årsak = FagsysteminfoSvarHendelse.RevurderingDto.Årsak.NYE_OPPLYSNINGER,
            årsakTilFeilutbetaling = "ingen",
            vedtaksdato = LocalDate.now(),
        ),
        utvidPerioder = utvidPerioder,
    )
}
