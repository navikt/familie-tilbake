package no.nav.tilbakekreving.e2e.ytelser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forOne
import io.kotest.inspectors.forSingle
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.config.OppdragClientRestMock
import no.nav.familie.tilbake.config.PdlClientMock
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.api.v1.dto.OpprettRevurderingDto
import no.nav.tilbakekreving.e2e.BehandlingsstegGenerator
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp.Companion.medFeilutbetaling
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.avventerBehandling
import no.nav.tilbakekreving.e2e.kanBehandle
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.integrasjoner.oppdrag.KodeAksjonDto
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.test.FellesTestdata.BESLUTTER_IDENT
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger
import java.time.LocalDate
import kotlin.random.Random

class TilleggsstønaderE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Autowired
    private lateinit var oppdragRestClient: OppdragClientRestMock

    @Test
    fun `kravgrunnlag fører til sak klar til behandling`() {
        val fnr = Random.nextLong(0, 31129999999).toString().padStart(11, '0')
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val vedtakId = KravgrunnlagGenerator.nextPaddedId(6)
        val ansvarligEnhet = KravgrunnlagGenerator.nextPaddedId(4)
        sendKravgrunnlagOgAvventLesing(
            TILLEGGSSTØNADER_KØ_NAVN,
            KravgrunnlagGenerator.forTilleggsstønader(
                fødselsnummer = fnr,
                fagsystemId = fagsystemId,
                vedtakId = vedtakId,
                ansvarligEnhet = ansvarligEnhet,
                perioder = listOf(
                    KravgrunnlagGenerator.Tilbakekrevingsperiode(
                        1.januar(2021) til 1.januar(2021),
                        tilbakekrevingsbeløp = listOf(
                            KravgrunnlagGenerator.Tilbakekrevingsbeløp.forKlassekode(
                                klassekode = KravgrunnlagGenerator.NyKlassekode.TSTBASISP4_OP,
                                beløpTilbakekreves = 2000.kroner,
                                beløpOpprinneligUtbetalt = 20000.kroner,
                            ),
                        ).medFeilutbetaling(KravgrunnlagGenerator.NyKlassekode.KL_KODE_FEIL_ARBYT),
                    ),
                ),
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))

        pdlClient.hentPersoninfoHits(fnr) shouldBe listOf(
            PdlClientMock.PersoninfoHit(
                ident = fnr,
                fagsystem = FagsystemDTO.TS,
            ),
        )

        val frontendDto = tilbakekreving(FagsystemDTO.TS, fagsystemId)
            .shouldNotBeNull()
            .tilFrontendDto(saksbehandlerContext().klokke)
        frontendDto.behandlinger shouldHaveSize 1
        frontendDto.behandlinger.single().status shouldBe Behandlingsstatus.UTREDES
        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()

        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.FAKTA
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.FORHÅNDSVARSEL

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.FORHÅNDSVARSEL
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.FORELDELSE

        lagreUttalelse(behandlingId)

        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.FORELDELSE
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.VILKÅRSVURDERING

        utførSteg(
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(1.januar(2021) til 1.januar(2021)),
        )

        behandling(behandlingId).vilkårsvurderingsstegDto.tilFrontendDto(saksbehandlerContext()).perioder[0].foreldet shouldBe false

        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.VILKÅRSVURDERING
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.FORESLÅ_VEDTAK

        utførSteg(
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(1.januar(2021) til 1.januar(2021)),
        )

        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.FORESLÅ_VEDTAK
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.FATTE_VEDTAK

        utførSteg(behandlingId, BehandlingsstegGenerator.lagForeslåVedtakVurdering())
        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.FATTE_VEDTAK
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.IVERKSETT_VEDTAK

        utførSteg(
            ident = BESLUTTER_IDENT,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagGodkjennVedtakVurdering(),
        )
        oppdragRestClient.shouldHaveIverksettelse(BigInteger(vedtakId)) { vedtak ->
            vedtak.vedtakId shouldBe BigInteger(vedtakId)
            vedtak.kodeAksjon shouldBe KodeAksjonDto.FATTE_VEDTAK
            vedtak.kodeHjemmel shouldBe "22-15"
            vedtak.vedtaksDato shouldBe LocalDate.now()
            vedtak.enhetAnsvarlig shouldBe ansvarligEnhet
            vedtak.kontrollfelt shouldBe "2025-12-24-11.12.13.123456"
            vedtak.saksbehandlerId shouldBe SAKSBEHANDLER_IDENT
            vedtak.perioder.forSingle { periode ->
                periode.periodeFom shouldBe 1.januar(2021)
                periode.periodeTom shouldBe 1.januar(2021)
                periode.belopRenter shouldBe 0.kroner
                periode.posteringer shouldHaveSize 2
                periode.posteringer.forOne { beløp ->
                    beløp.kodeKlasse shouldBe "TSTBASISP4-OP"
                    beløp.belopNy shouldBe 18000.kroner
                    beløp.belopOpprinneligUtbetalt shouldBe 20000.0.kroner
                    beløp.belopTilbakekreves shouldBe 2000.kroner
                    beløp.belopUinnkrevd shouldBe 0.kroner
                    beløp.belopSkatt shouldBe 0.kroner
                    beløp.kodeResultat shouldBe "FULL_TILBAKEKREV"
                    beløp.kodeAarsak shouldBe "ANNET"
                    beløp.kodeSkyld shouldBe "IKKE_FORDELT"
                }
                periode.posteringer.forOne { beløp ->
                    beløp.kodeKlasse shouldBe "KL_KODE_FEIL_ARBYT"
                    beløp.belopNy shouldBe 2000.0.kroner
                    beløp.belopOpprinneligUtbetalt shouldBe 0.kroner
                    beløp.belopTilbakekreves shouldBe 0.kroner
                    beløp.belopUinnkrevd shouldBe 0.kroner
                    beløp.belopSkatt shouldBe 0.kroner
                    beløp.kodeResultat shouldBe ""
                    beløp.kodeAarsak shouldBe ""
                    beløp.kodeSkyld shouldBe ""
                }
            }
        }
    }

    @Test
    fun `revurdering av vedtak med full utbetaling fører til ingen tilbakekreving`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val vedtakId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
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

        val exception = shouldThrow<ModellFeil.UtenforScopeException> {
            behandlingController.opprettRevurdering(
                OpprettRevurderingDto(
                    YtelsestypeDTO.TILLEGGSSTØNAD,
                    behandlingId,
                    Behandlingsårsakstype.REVURDERING_KLAGE_KA,
                ),
            )
        }

        exception.utenforScope shouldBe UtenforScope.Revurdering
    }

    @Test
    fun `underkjenning av vedtak skal tilbakeføre behandling til tidligere steg`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
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
        tilbakekreving(behandlingId).frontendDtoForBehandling(behandlingId, saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).status shouldBe Behandlingsstatus.FATTER_VEDTAK
        utførSteg(
            ident = BESLUTTER_IDENT,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeGodkjennVedtakVurdering(),
        )
        tilbakekreving(behandlingId).frontendDtoForBehandling(behandlingId, saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).status shouldBe Behandlingsstatus.UTREDES

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
        tilbakekreving(behandlingId).frontendDtoForBehandling(behandlingId, saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).status shouldBe Behandlingsstatus.AVSLUTTET
    }

    companion object {
        const val TILLEGGSSTØNADER_KØ_NAVN = "LOCAL_TILLEGGSSTONADER.KRAVGRUNNLAG"
    }
}
