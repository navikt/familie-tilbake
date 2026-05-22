package no.nav.tilbakekreving.e2e

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BehandlingskontrollE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Test
    fun `flytt behandling tilbake til fakta`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Z999999")

        val dtoFørUtførtFakta = tilbakekreving(behandlingId).frontendDtoForBehandling(behandlingId, ansvarligSaksbehandler, true)
        dtoFørUtførtFakta.behandlingsstegsinfo.find { it.behandlingssteg == Behandlingssteg.FAKTA }
            .shouldNotBeNull()
            .behandlingsstegstatus shouldBe Behandlingsstegstatus.KLAR

        somSaksbehandler(ansvarligSaksbehandler.ident) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        val dtoEtterUtførtFakta = tilbakekreving(behandlingId).frontendDtoForBehandling(behandlingId, ansvarligSaksbehandler, true)
        dtoEtterUtførtFakta.behandlingsstegsinfo.find { it.behandlingssteg == Behandlingssteg.FAKTA }
            .shouldNotBeNull()
            .behandlingsstegstatus shouldBe Behandlingsstegstatus.UTFØRT

        somSaksbehandler(ansvarligSaksbehandler.ident) {
            behandlingController.flyttBehandlingTilFakta(behandlingId) shouldBe Ressurs.success("OK")
        }

        val dtoEtterTilbakeTilFakta = tilbakekreving(behandlingId).frontendDtoForBehandling(behandlingId, ansvarligSaksbehandler, true)
        dtoEtterTilbakeTilFakta.behandlingsstegsinfo.find { it.behandlingssteg == Behandlingssteg.FAKTA }
            .shouldNotBeNull()
            .behandlingsstegstatus shouldBe Behandlingsstegstatus.KLAR
    }
}
