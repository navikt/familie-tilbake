package no.nav.tilbakekreving.e2e.tilstand

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.api.v2.MottakerDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.api.v2.PeriodeDto
import no.nav.tilbakekreving.api.v2.fagsystem.svar.FagsysteminfoSvarHendelse
import no.nav.tilbakekreving.e2e.BehandlingsstegGenerator
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.kanBehandle
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
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
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()

        tilbakekreving(behandlingId).tilFrontendDto().behandlinger.single().status shouldBe Behandlingsstatus.UTREDES
        fagsystemIntegrasjonService.håndter(
            Ytelse.Tilleggsstønad,
            FagsysteminfoSvarHendelse(
                eksternFagsakId = fagsystemId,
                hendelseOpprettet = LocalDateTime.now(),
                mottaker = MottakerDto(
                    ident = Testdata.TESTBRUKER,
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
                behandlendeEnhet = "0425",
            ),
        )

        val tilbakekreving = tilbakekreving(behandlingId)
        tilbakekreving.behandlingHistorikk.nåværende().entry.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).feilutbetaltePerioder.map { it.periode } shouldBe listOf(
            1.januar(2021) til 31.januar(2021),
        )
        tilbakekreving.behandlingHistorikk.nåværende().entry.foreldelsestegDto.tilFrontendDto().foreldetPerioder.map { it.periode } shouldBe listOf(
            1.januar(2021) til 31.januar(2021),
        )
        tilbakekreving.behandlingHistorikk.nåværende().entry.vilkårsvurderingsstegDto.tilFrontendDto().perioder.map { it.periode } shouldBe listOf(
            1.januar(2021) til 31.januar(2021),
        )
    }

    @Test
    fun `nullstilling av behandling etter fagsysteminfo er ferdig håndtert`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()

        tilbakekreving(behandlingId).tilFrontendDto().behandlinger.single().status shouldBe Behandlingsstatus.UTREDES
        fagsystemIntegrasjonService.håndter(
            Ytelse.Tilleggsstønad,
            FagsysteminfoSvarHendelse(
                eksternFagsakId = fagsystemId,
                hendelseOpprettet = LocalDateTime.now(),
                mottaker = MottakerDto(
                    ident = Testdata.TESTBRUKER,
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
                behandlendeEnhet = "0425",
            ),
        )

        somSaksbehandler("Z111111") {
            behandlingController.flyttBehandlingTilFakta(behandlingId).status shouldBe Ressurs.Status.SUKSESS
        }

        val tilbakekreving = tilbakekreving(behandlingId)
        tilbakekreving.behandlingHistorikk.nåværende().entry.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).feilutbetaltePerioder.map { it.periode } shouldBe listOf(
            1.januar(2021) til 31.januar(2021),
        )
        tilbakekreving.behandlingHistorikk.nåværende().entry.foreldelsestegDto.tilFrontendDto().foreldetPerioder.map { it.periode } shouldBe listOf(
            1.januar(2021) til 31.januar(2021),
        )
        tilbakekreving.behandlingHistorikk.nåværende().entry.vilkårsvurderingsstegDto.tilFrontendDto().perioder.map { it.periode } shouldBe listOf(
            1.januar(2021) til 31.januar(2021),
        )
    }

    @Test
    fun `tilbakekreving trekkes tilbake fra godkjenning`() {
        val behandlerIdent = "Z111111"
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        lagreUttalelse(behandlingId)

        somSaksbehandler(behandlerIdent) {
            behandlingApiController.oppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        utførSteg(
            ident = behandlerIdent,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )
        utførSteg(
            ident = behandlerIdent,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(),
        )
        utførSteg(
            ident = behandlerIdent,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagForeslåVedtakVurdering(),
        )

        somSaksbehandler(behandlerIdent) {
            behandlingController.angreSendTilBeslutter(behandlingId)
        }
        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.FORESLÅ_VEDTAK
    }
}
