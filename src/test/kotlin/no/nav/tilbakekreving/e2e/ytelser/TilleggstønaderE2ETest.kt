package no.nav.tilbakekreving.e2e.ytelser

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.config.PdlClientMock
import no.nav.tilbakekreving.api.v1.dto.AktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFaktaDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeslåVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.ForeldelsesperiodeDto
import no.nav.tilbakekreving.api.v1.dto.FritekstavsnittDto
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertTotrinnDto
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp.Companion.medFeilutbetaling
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.avventerBehandling
import no.nav.tilbakekreving.e2e.kanBehandle
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random

class TilleggstønaderE2ETest : TilbakekrevingE2EBase() {
    @Test
    fun `kravgrunnlag fører til sak klar til behandling`() {
        val fnr = Random.nextLong(0, 31129999999).toString().padStart(11, '0')
        val fagsystemId = UUID.randomUUID().toString()
        sendKravgrunnlagOgAvventLesing(
            "LOCAL_TILLEGGSSTONADER.KRAVGRUNNLAG",
            KravgrunnlagGenerator.forTillegstønader(
                fødselsnummer = fnr,
                fagsystemId = fagsystemId,
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

        // TODO: må implementeres
//        tilbakekreving kanBehandle Behandlingssteg.FAKTA
//        tilbakekreving avventerBehandling Behandlingssteg.FORELDELSE
        utførSteg(
            ident = "Z999999",
            behandlingId = behandlingId,
            stegData = BehandlingsstegFaktaDto(
                feilutbetaltePerioder = listOf(
                    FaktaFeilutbetalingsperiodeDto(
                        periode = 1.januar(2021) til 1.januar(2021),
                        hendelsestype = Hendelsestype.ANNET,
                        hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
                    ),
                ),
                begrunnelse = "Begrunnelse",
            ),
        )

        behandling(behandlingId) kanBehandle Behandlingssteg.FORELDELSE
        behandling(behandlingId) avventerBehandling Behandlingssteg.VILKÅRSVURDERING

        utførSteg(
            ident = "Z999999",
            behandlingId = behandlingId,
            stegData = BehandlingsstegForeldelseDto(
                foreldetPerioder = listOf(
                    ForeldelsesperiodeDto(
                        periode = 1.januar(2021) til 1.januar(2021),
                        begrunnelse = "Utbetalingen er ikke foreldet",
                        foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_FORELDET,
                        foreldelsesfrist = null,
                        oppdagelsesdato = null,
                    ),
                ),
            ),
        )

        behandling(behandlingId) kanBehandle Behandlingssteg.VILKÅRSVURDERING
        behandling(behandlingId) avventerBehandling Behandlingssteg.FORESLÅ_VEDTAK

        utførSteg(
            ident = "Z999999",
            behandlingId = behandlingId,
            stegData = BehandlingsstegVilkårsvurderingDto(
                vilkårsvurderingsperioder = listOf(
                    VilkårsvurderingsperiodeDto(
                        periode = 1.januar(2021) til 1.januar(2021),
                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                        begrunnelse = "Jepp",
                        godTroDto = null,
                        aktsomhetDto = AktsomhetDto(
                            aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                            ileggRenter = false,
                            andelTilbakekreves = null,
                            beløpTilbakekreves = null,
                            begrunnelse = "Jaha",
                            særligeGrunner = emptyList(),
                            særligeGrunnerTilReduksjon = false,
                            tilbakekrevSmåbeløp = true,
                            særligeGrunnerBegrunnelse = "Særlige grunner",
                        ),
                    ),
                ),
            ),
        )

        behandling(behandlingId) kanBehandle Behandlingssteg.FORESLÅ_VEDTAK
        behandling(behandlingId) avventerBehandling Behandlingssteg.FATTE_VEDTAK

        utførSteg(
            ident = "Z999999",
            behandlingId = behandlingId,
            stegData = BehandlingsstegForeslåVedtaksstegDto(
                fritekstavsnitt = FritekstavsnittDto(
                    oppsummeringstekst = null,
                    perioderMedTekst = emptyList(),
                ),
            ),
        )
        behandling(behandlingId) kanBehandle Behandlingssteg.FATTE_VEDTAK
        behandling(behandlingId) avventerBehandling Behandlingssteg.IVERKSETT_VEDTAK

        utførSteg(
            ident = "Z111111",
            behandlingId = behandlingId,
            stegData = BehandlingsstegFatteVedtaksstegDto(
                totrinnsvurderinger = listOf(
                    VurdertTotrinnDto(behandlingssteg = Behandlingssteg.FAKTA, godkjent = true, begrunnelse = null),
                    VurdertTotrinnDto(behandlingssteg = Behandlingssteg.FORELDELSE, godkjent = true, begrunnelse = null),
                    VurdertTotrinnDto(behandlingssteg = Behandlingssteg.VILKÅRSVURDERING, godkjent = true, begrunnelse = null),
                    VurdertTotrinnDto(behandlingssteg = Behandlingssteg.FORESLÅ_VEDTAK, godkjent = true, begrunnelse = null),
                ),
            ),
        )
        oppdragClient.shouldHaveIverksettelse(behandlingId)
    }
}
