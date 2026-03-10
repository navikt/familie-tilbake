package no.nav.tilbakekreving.e2e

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatDto
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatVurderingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksresultatDto
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class VedtaksresultatE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Test
    fun `hentVedtaksresultat returnerer beregningsresultat via nytt endepunkt`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        lagreUttalelse(behandlingId)
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Z999999")

        somSaksbehandler(ansvarligSaksbehandler.ident) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(),
        )

        lateinit var response: ResponseEntity<BeregningsresultatDto>
        somSaksbehandler(ansvarligSaksbehandler.ident) {
            response = behandlingApiController.behandlingHentVedtaksresultat(behandlingId)
        }

        response.statusCode shouldBe HttpStatus.OK
        val resultat = response.body.shouldNotBeNull()

        resultat.vedtaksresultat shouldBe VedtaksresultatDto.FULL_TILBAKEBETALING
        resultat.beregningsresultatsperioder shouldHaveSize 1

        val periode = resultat.beregningsresultatsperioder.first()
        periode.vurdering shouldBe BeregningsresultatVurderingDto.GROV_UAKTSOMHET
        periode.feilutbetaltBeløp shouldBe 2000
        periode.andelAvBeløp shouldBe "100%"
        periode.renteprosent shouldBe "0%"
        periode.tilbakekrevingsbeløp shouldBe 2000
    }

    @Test
    fun `hentVedtaksresultat returnerer ingen tilbakebetaling under 4x rettsgebyr`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        lagreUttalelse(behandlingId)
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Z999999")

        somSaksbehandler(ansvarligSaksbehandler.ident) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingUnder4xRettsgebyrIngenTilbakekreving(
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
            ),
        )

        lateinit var response: ResponseEntity<BeregningsresultatDto>
        somSaksbehandler(ansvarligSaksbehandler.ident) {
            response = behandlingApiController.behandlingHentVedtaksresultat(behandlingId)
        }

        response.statusCode shouldBe HttpStatus.OK
        val resultat = response.body.shouldNotBeNull()

        resultat.vedtaksresultat shouldBe VedtaksresultatDto.INGEN_TILBAKEBETALING
        resultat.beregningsresultatsperioder.first().tilbakekrevingsbeløp shouldBe 0
    }
}
