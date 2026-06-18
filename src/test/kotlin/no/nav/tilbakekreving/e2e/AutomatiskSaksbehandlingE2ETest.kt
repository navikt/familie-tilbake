package no.nav.tilbakekreving.e2e

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.frontend.models.UnntakDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VarslingsunntakDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class AutomatiskSaksbehandlingE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Test
    fun `automatisk vurdering av foreldelse blir lagret med begrunnelse`() {
        val fom = LocalDate.now().minusMonths(10).withDayOfMonth(1)
        val tom = fom.withDayOfMonth(fom.lengthOfMonth())
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)

        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                perioder = listOf(KravgrunnlagGenerator.standardPeriode(fom til tom)),
            ),
        )
        fagsystemIntegrasjonService.håndter(
            Ytelse.Tilleggsstønad,
            Testdata.fagsysteminfoSvar(fagsystemId = fagsystemId, utvidPerioder = emptyList()),
        )

        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()

        val frontendDto = tilbakekreving(behandlingId).frontendDtoForBehandling(
            behandlingId = behandlingId,
            sideeffektContext = systemContext(),
            kanBeslutte = false,
            behandlerRolle = BehandlerRolle.SAKSBEHANDLER,
        )

        frontendDto.behandlingsstegsinfo.size shouldBe 2
        frontendDto.behandlingsstegsinfo[0].behandlingssteg shouldBe Behandlingssteg.GRUNNLAG
        frontendDto.behandlingsstegsinfo[1].behandlingssteg shouldBe Behandlingssteg.FAKTA

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingLagreForhaandsvarselUnntak(
                behandlingId = behandlingId,
                unntakDto = UnntakDto(
                    begrunnelseForUnntak = VarslingsunntakDto.IKKE_PRAKTISK_MULIG,
                    beskrivelse = "Ikke praktisk mulig",
                ),
            )
        }

        tilbakekreving(behandlingId).frontendDtoForBehandling(
            behandlingId = behandlingId,
            sideeffektContext = systemContext(),
            kanBeslutte = false,
            behandlerRolle = BehandlerRolle.SAKSBEHANDLER,
        ) shouldNotBeNull {
            behandlingsstegsinfo.size shouldBe 5
            behandlingsstegsinfo[0].behandlingssteg shouldBe Behandlingssteg.GRUNNLAG
            behandlingsstegsinfo[1].behandlingssteg shouldBe Behandlingssteg.FAKTA
            behandlingsstegsinfo[2].behandlingssteg shouldBe Behandlingssteg.FORHÅNDSVARSEL
            behandlingsstegsinfo[3].behandlingssteg shouldBe Behandlingssteg.FORELDELSE
            behandlingsstegsinfo[4].behandlingssteg shouldBe Behandlingssteg.VILKÅRSVURDERING
        }

        tilbakekreving(behandlingId)
            .hentBehandling(behandlingId)
            .foreldelsestegDto.tilFrontendDto(saksbehandlerContext())
            .foreldetPerioder.single() shouldNotBeNull {
            foreldelsesvurderingstype shouldBe Foreldelsesvurderingstype.AUTOMATISK_VURDERT_IKKE_FORELDET
            begrunnelse shouldStartWith "Ingen perioder er foreldet fordi det er mindre enn tre år siden første feilutbetaling fant sted. Dette følger av foreldelsesloven §§ 2 og 3."
        }
    }
}
