package no.nav.tilbakekreving.e2e.tilstand

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.data.Testdata
import no.nav.tilbakekreving.api.v2.MottakerDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.api.v2.PeriodeDto
import no.nav.tilbakekreving.api.v2.fagsystem.svar.FagsysteminfoSvarHendelse
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TilBehandlingTest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Test
    fun `Mottar fagsysteminfo når sak er klar til saksbehandling`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()

        tilbakekreving(behandlingId).tilFrontendDto().behandlinger.single().status shouldBe Behandlingsstatus.UTREDES
        fagsystemIntegrasjonService.håndter(
            Ytelse.Tilleggsstønad,
            FagsysteminfoSvarHendelse(
                eksternFagsakId = fagsystemId,
                hendelseOpprettet = LocalDateTime.now(),
                mottaker = MottakerDto(
                    ident = Testdata.STANDARD_BRUKERIDENT,
                    type = MottakerDto.MottakerType.PERSON,
                ),
                revurdering = FagsysteminfoSvarHendelse.RevurderingDto(
                    behandlingId = UUID.randomUUID().toString(),
                    årsak = FagsysteminfoSvarHendelse.RevurderingDto.Årsak.NYE_OPPLYSNINGER,
                    årsakTilFeilutbetaling = "ingen",
                    vedtaksdato = LocalDate.now(),
                ),
                utvidPerioder = listOf(
                    FagsysteminfoSvarHendelse.UtvidetPeriodeDto(
                        kravgrunnlagPeriode = PeriodeDto(fom = 1.januar(2021), tom = 1.januar(2021)),
                        vedtaksperiode = PeriodeDto(fom = 1.januar(2021), tom = 31.januar(2021)),
                    ),
                ),
            ),
        )

        val tilbakekreving = tilbakekreving(behandlingId)
        tilbakekreving.behandlingHistorikk.nåværende().entry.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL).feilutbetaltePerioder.map { it.periode } shouldBe listOf(
            1.januar(2021) til 31.januar(2021),
        )
        tilbakekreving.behandlingHistorikk.nåværende().entry.foreldelsestegDto.tilFrontendDto().foreldetPerioder.map { it.periode } shouldBe listOf(
            1.januar(2021) til 31.januar(2021),
        )
        tilbakekreving.behandlingHistorikk.nåværende().entry.vilkårsvurderingsstegDto.tilFrontendDto().perioder.map { it.periode } shouldBe listOf(
            1.januar(2021) til 31.januar(2021),
        )
    }
}
