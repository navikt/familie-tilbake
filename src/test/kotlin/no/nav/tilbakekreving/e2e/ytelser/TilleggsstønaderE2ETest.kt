package no.nav.tilbakekreving.e2e.ytelser

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forOne
import io.kotest.inspectors.forSingle
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.config.PdlClientMock
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.UtenforScope
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
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger
import java.time.LocalDate
import kotlin.random.Random

class TilleggsstønaderE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

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

        pdlClient.hentPersoninfoHits shouldBe listOf(
            PdlClientMock.PersoninfoHit(
                ident = fnr,
                fagsystem = FagsystemDTO.TS,
            ),
        )

        val frontendDto = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId)
            .shouldNotBeNull()
            .tilFrontendDto()
        frontendDto.behandlinger shouldHaveSize 1
        frontendDto.behandlinger.single().status shouldBe Behandlingsstatus.UTREDES
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()

        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.FAKTA
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.FORELDELSE

        utførSteg(
            ident = "Z999999",
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(1.januar(2021) til 1.januar(2021)),
        )

        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.FORELDELSE
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.VILKÅRSVURDERING

        utførSteg(
            ident = "Z999999",
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(1.januar(2021) til 1.januar(2021)),
        )

        behandling(behandlingId).vilkårsvurderingsstegDto.tilFrontendDto().perioder[0].foreldet shouldBe false

        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.VILKÅRSVURDERING
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.FORESLÅ_VEDTAK

        utførSteg(
            ident = "Z999999",
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(1.januar(2021) til 1.januar(2021)),
        )

        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.FORESLÅ_VEDTAK
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.FATTE_VEDTAK

        utførSteg(
            ident = "Z999999",
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagForeslåVedtakVurdering(),
        )
        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.FATTE_VEDTAK
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.IVERKSETT_VEDTAK

        utførSteg(
            ident = "Z111111",
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagGodkjennVedtakVurdering(),
        )
        oppdragClient.shouldHaveIverksettelse(behandlingId) { vedtak ->
            vedtak.vedtakId shouldBe BigInteger(vedtakId)
            vedtak.kodeAksjon shouldBe KodeAksjon.FATTE_VEDTAK.kode
            vedtak.kodeHjemmel shouldBe "22-15"
            vedtak.datoVedtakFagsystem shouldBe LocalDate.now()
            vedtak.enhetAnsvarlig = ansvarligEnhet
            vedtak.kontrollfelt = "2025-12-24-11.12.13.123456"
            vedtak.saksbehId = "Z999999"
            vedtak.tilbakekrevingsperiode.forSingle { periode ->
                periode.periode.fom shouldBe 1.januar(2021)
                periode.periode.tom shouldBe 1.januar(2021)
                periode.belopRenter shouldBe 0.kroner
                periode.tilbakekrevingsbelop shouldHaveSize 2
                periode.tilbakekrevingsbelop.forOne { beløp ->
                    beløp.kodeKlasse shouldBe "TSTBASISP4-OP"
                    beløp.belopNy = 18000.kroner
                    beløp.belopOpprUtbet = 20000.kroner
                    beløp.belopTilbakekreves = 2000.kroner
                    beløp.belopUinnkrevd = 0.kroner
                    beløp.belopSkatt = 0.kroner
                    beløp.kodeResultat shouldBe "FULL_TILBAKEKREV"
                    beløp.kodeAarsak = "ANNET"
                    beløp.kodeSkyld = "IKKE_FORDELT"
                }
                periode.tilbakekrevingsbelop.forOne { beløp ->
                    beløp.kodeKlasse shouldBe "KL_KODE_FEIL_ARBYT"
                    beløp.belopNy = 2000.kroner
                    beløp.belopOpprUtbet = 0.kroner
                    beløp.belopTilbakekreves = 0.kroner
                    beløp.belopUinnkrevd = 0.kroner
                    beløp.belopSkatt = 0.kroner
                    beløp.kodeResultat shouldBe null
                    beløp.kodeAarsak = "ANNET"
                    beløp.kodeSkyld = "IKKE_FORDELT"
                }
            }
        }
    }

    @Test
    fun `revurdering av vedtak med full utbetaling fører til ingen tilbakekreving`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()

        utførSteg(
            ident = "Z999999",
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(),
        )
        utførSteg(
            ident = "Z999999",
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )
        utførSteg(
            ident = "Z999999",
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(),
        )
        utførSteg(
            ident = "Z999999",
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagForeslåVedtakVurdering(),
        )
        utførSteg(
            ident = "Z111111",
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagGodkjennVedtakVurdering(),
        )

        oppdragClient.shouldHaveIverksettelse(behandlingId) { vedtak ->
            vedtak.tilbakekrevingsperiode shouldHaveSize 1
            val tilbakekrevingsperiode = vedtak.tilbakekrevingsperiode.single()
            tilbakekrevingsperiode.tilbakekrevingsbelop shouldHaveSize 2
            tilbakekrevingsperiode.tilbakekrevingsbelop.forOne { beløp ->
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

    companion object {
        const val TILLEGGSSTØNADER_KØ_NAVN = "LOCAL_TILLEGGSSTONADER.KRAVGRUNNLAG"
    }
}
