package no.nav.tilbakekreving.e2e

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.OppdragClientRestMock
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeslåVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.FritekstavsnittDto
import no.nav.tilbakekreving.brev.Vedtaksbrev
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.DetaljerPeriodeDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.DetaljerPosteringDto
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.frontend.models.EtterKlageDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RevurderingsinstansDto
import no.nav.tilbakekreving.kontrakter.frontend.models.TilbakekrevingRevurderingDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.test.FellesTestdata.BESLUTTER_IDENT
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import java.math.BigInteger
import java.time.format.DateTimeFormatter
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

    @Test
    fun `revurdering vedtaksbrev blir opprettet med korrekt brevtype`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val vedtakId = KravgrunnlagGenerator.nextPaddedId(6)
        val opprinneligBehandlingId = gjørBehandlingKlarTilRevurdering(fagsystemId, vedtakId)

        tilbakekreving(opprinneligBehandlingId).brevHistorikk.sisteVedtaksbrev().shouldBeInstanceOf<Vedtaksbrev> {
            it.vedtaksbrevType shouldBe Vedtaksbrev.Vedtaksbrevtype.FØRSTEGANG
        }

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

        utførSteg(
            revurderingBehandlingId,
            BehandlingsstegForeslåVedtaksstegDto(
                fritekstavsnitt = FritekstavsnittDto(
                    oppsummeringstekst = null,
                    perioderMedTekst = emptyList(),
                ),
            ),
        )

        utførSteg(
            ident = BESLUTTER_IDENT,
            behandlingId = revurderingBehandlingId,
            stegData = BehandlingsstegGenerator.lagGodkjennVedtakVurdering(),
        )
        tilbakekreving(revurderingBehandlingId).brevHistorikk.sisteVedtaksbrev().shouldBeInstanceOf<Vedtaksbrev> {
            it.vedtaksbrevType shouldBe Vedtaksbrev.Vedtaksbrevtype.REVURDERING
        }
    }

    private fun gjørBehandlingKlarTilRevurdering(fagsystemId: String, vedtakId: String = KravgrunnlagGenerator.nextPaddedId(6)): UUID {
        val eksisterendePeriode = 1.januar(2021) til 31.januar(2021)
        val kravgrunnlagId = KravgrunnlagGenerator.nextPaddedId(6)
        val periode = KravgrunnlagGenerator.standardPeriode(eksisterendePeriode, feilutbetaltBeløp = 3000.kroner)
        sendKravgrunnlagOgAvventLesing(
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                vedtakId = vedtakId,
                kravgrunnlagId = kravgrunnlagId,
                perioder = listOf(periode),
                kontrollfelt = 1.januar(2021).atTime(12, 0).format(DateTimeFormatter.ofPattern("YYYY-MM-dd-HH.mm.ss.SSSSSS")),
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
        oppdragRestClient.mockHentKravgrunnlag(
            kravgrunnlagId = kravgrunnlagId.toBigInteger(),
            perioder = listOf(
                DetaljerPeriodeDto(
                    periodeFom = periode.periode.fom,
                    periodeTom = periode.periode.tom,
                    belopSkattMnd = periode.beløpSkattMnd,
                    posteringer = periode.tilbakekrevingsbeløp.map { beløp ->
                        DetaljerPosteringDto(
                            kodeKlasse = beløp.klassekode.osNavn(),
                            typeKlasse = beløp.typeKlasse.name,
                            belopOpprinneligUtbetalt = beløp.beløpOpprinneligUtbetalt,
                            belopNy = BigDecimal(10000),
                            belopTilbakekreves = beløp.beløpTilbakekreves,
                            belopUinnkrevd = beløp.beløpUinnkrevd,
                            skattProsent = beløp.skattProsent,
                            kodeResultat = "",
                            kodeAarsak = "",
                            kodeSkyld = "",
                        )
                    },
                ),
            ),
        )
        return behandlingId
    }
}
