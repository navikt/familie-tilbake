package no.nav.tilbakekreving.e2e

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.OppdragClientRestMock
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.frontend.models.EtterKlageDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RevurderingsinstansDto
import no.nav.tilbakekreving.kontrakter.frontend.models.TilbakekrevingRevurderingDto
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.test.FellesTestdata.BESLUTTER_IDENT
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.math.BigInteger
import java.util.UUID

@TestPropertySource(
    properties = ["tilbakekreving.toggles.nyModell.EndretKravgrunnlagVisning=true"],
)
class RevurderingAvTilbakekrevingE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var oppdragRestClient: OppdragClientRestMock

    @Test
    fun `revurdering av vedtak med full utbetaling fører til ny behandling`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val behandlingId = gjørBehandlingKlarTilRevurdering(fagsystemId)

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingOpprettRevurdering(
                behandlingId = behandlingId,
                revurderingDto = TilbakekrevingRevurderingDto(
                    revurderingsarsak = EtterKlageDto(
                        instans = RevurderingsinstansDto.Klageinstansen,
                        begrunnelse = "Begrunnelse for revurdering",
                    ),
                ),
            )
        }
        val revurderingBehandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        revurderingBehandlingId shouldNotBe behandlingId
        tilbakekreving(revurderingBehandlingId).frontendDtoForBehandling(revurderingBehandlingId, saksbehandlerContext(), false, BehandlerRolle.SAKSBEHANDLER) shouldNotBeNull {
            status shouldBe Behandlingsstatus.UTREDES
            type shouldBe Behandlingstype.REVURDERING_TILBAKEKREVING
            tilbakekrevingRevurdering shouldBe TilbakekrevingRevurderingDto(
                revurderingsarsak = EtterKlageDto(
                    instans = RevurderingsinstansDto.Klageinstansen,
                    begrunnelse = "Begrunnelse for revurdering",
                ),
            )
        }
    }

    @Test
    fun `opprinnelig behandling kan ikke oppdateres når revurdering er opprettet`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val opprinneligBehandlingId = gjørBehandlingKlarTilRevurdering(fagsystemId)

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingOpprettRevurdering(
                behandlingId = opprinneligBehandlingId,
                revurderingDto = TilbakekrevingRevurderingDto(
                    revurderingsarsak = EtterKlageDto(
                        instans = RevurderingsinstansDto.Klageinstansen,
                        begrunnelse = "Begrunnelse for revurdering",
                    ),
                ),
            )
        }

        val revurderingBehandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingController.hentBehandling(opprinneligBehandlingId).data!!.kanEndres shouldBe false
            behandlingController.hentBehandling(revurderingBehandlingId).data!!.kanEndres shouldBe true
        }

        shouldThrow<Feil> {
            somSaksbehandler(SAKSBEHANDLER_IDENT) {
                behandlingApiController.behandlingOppdaterFakta(
                    behandlingId = opprinneligBehandlingId.toString(),
                    oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(opprinneligBehandlingId)),
                )
            }
        }.message shouldBe "Denne behandlingen kan ikke endres fordi den har allerede en revurdering."
    }

    @Test
    fun `kan ikke opprette to revurderinger på opprinnelig behandling`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val opprinneligBehandlingId = gjørBehandlingKlarTilRevurdering(fagsystemId)

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingOpprettRevurdering(
                behandlingId = opprinneligBehandlingId,
                revurderingDto = TilbakekrevingRevurderingDto(
                    revurderingsarsak = EtterKlageDto(
                        instans = RevurderingsinstansDto.Klageinstansen,
                        begrunnelse = "Begrunnelse for revurdering",
                    ),
                ),
            )
        }

        shouldThrow<Feil> {
            somSaksbehandler(SAKSBEHANDLER_IDENT) {
                behandlingApiController.behandlingOpprettRevurdering(
                    behandlingId = opprinneligBehandlingId,
                    revurderingDto = TilbakekrevingRevurderingDto(
                        revurderingsarsak = EtterKlageDto(
                            instans = RevurderingsinstansDto.Klageinstansen,
                            begrunnelse = "Begrunnelse for revurdering",
                        ),
                    ),
                )
            }
        }.message shouldBe "Denne behandlingen kan ikke endres fordi den har allerede en revurdering."
    }

    private fun gjørBehandlingKlarTilRevurdering(fagsystemId: String): UUID {
        val vedtakId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                vedtakId = vedtakId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))

        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        lagreUttalelse(behandlingId)

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }
        utførSteg(behandlingId, BehandlingsstegGenerator.lagIkkeForeldetVurdering())
        utførSteg(behandlingId, BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving())
        utførSteg(behandlingId, BehandlingsstegGenerator.lagForeslåVedtakVurdering())
        utførSteg(
            ident = BESLUTTER_IDENT,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagGodkjennVedtakVurdering(),
        )

        oppdragRestClient.shouldHaveIverksettelse(BigInteger(vedtakId)) { vedtak ->
            vedtak.perioder shouldHaveSize 1
            val tilbakekrevingsperiode = vedtak.perioder.single()
            tilbakekrevingsperiode.posteringer shouldHaveSize 2
            tilbakekrevingsperiode.posteringer.forOne { beløp ->
                beløp.kodeResultat shouldBe "FULL_TILBAKEKREV"
            }
        }
        return behandlingId
    }
}
