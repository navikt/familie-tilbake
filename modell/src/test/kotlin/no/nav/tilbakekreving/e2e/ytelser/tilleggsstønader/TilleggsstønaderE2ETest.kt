package no.nav.tilbakekreving.e2e.ytelser.tilleggsstønader

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v1.dto.FeilutbetalingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelsesperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.eksternFagsak
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.februar
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.junit.jupiter.api.Test
import java.util.UUID

class TilleggsstønaderE2ETest {
    private val bigQueryService = BigQueryServiceStub()

    @Test
    fun `hopper over innhenting av fagsystem info`() {
        val observatør = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse(
            eksternFagsak = eksternFagsak(
                ytelse = Ytelse.Tilleggsstønad,
            ),
        )
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), observatør, opprettTilbakekrevingHendelse, bigQueryService, EndringObservatørOppsamler())
        tilbakekreving.håndter(kravgrunnlag())

        observatør.behovListe.size shouldBe 2
        observatør.behovListe.filterIsInstance<BrukerinfoBehov>().size shouldBe 1
        observatør.behovListe.filterIsInstance<FagsysteminfoBehov>().size shouldBe 1
    }

    @Test
    fun `utvidelse av feilutbetalingsperiode`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val behandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")

        val tilbakekreving = Tilbakekreving.opprett(
            UUID.randomUUID().toString(),
            BehovObservatørOppsamler(),
            opprettTilbakekrevingHendelse,
            BigQueryServiceStub(),
            EndringObservatørOppsamler(),
        )

        tilbakekreving.håndter(
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(3.januar til 3.januar),
                    kravgrunnlagPeriode(1.februar til 1.februar),
                ),
            ),
        )
        tilbakekreving.håndter(
            fagsysteminfoHendelse(
                utvidPerioder = listOf(
                    FagsysteminfoHendelse.UtvidetPeriode(
                        kravgrunnlagPeriode = 3.januar til 3.januar,
                        vedtaksperiode = 1.januar til 31.januar,
                    ),
                    FagsysteminfoHendelse.UtvidetPeriode(
                        kravgrunnlagPeriode = 1.februar til 1.februar,
                        vedtaksperiode = 1.februar til 28.februar,
                    ),
                ),
            ),
        )
        tilbakekreving.håndter(brukerinfoHendelse())

        val faktastegDto = tilbakekreving.faktastegFrontendDto()
        faktastegDto.feilutbetaltePerioder shouldBe listOf(
            FeilutbetalingsperiodeDto(
                periode = 1.januar til 31.januar,
                feilutbetaltBeløp = 2000.kroner,
                hendelsestype = Hendelsestype.ANNET,
                hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
            ),
            FeilutbetalingsperiodeDto(
                periode = 1.februar til 28.februar,
                feilutbetaltBeløp = 2000.kroner,
                hendelsestype = Hendelsestype.ANNET,
                hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
            ),
        )
        faktastegDto.totalFeilutbetaltPeriode shouldBe (1.januar til 28.februar)

        tilbakekreving.håndter(behandler, faktastegVurdering(1.januar til 31.januar))
        tilbakekreving.håndter(behandler, faktastegVurdering(1.februar til 28.februar))

        val foreldelsesstegDto = tilbakekreving.behandlingHistorikk.nåværende().entry.foreldelsestegDto.tilFrontendDto()
        foreldelsesstegDto.foreldetPerioder shouldBe listOf(
            VurdertForeldelsesperiodeDto(
                periode = 1.januar til 31.januar,
                feilutbetaltBeløp = 2000.kroner,
                begrunnelse = null,
                foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_VURDERT,
                foreldelsesfrist = null,
                oppdagelsesdato = null,
            ),
            VurdertForeldelsesperiodeDto(
                periode = 1.februar til 28.februar,
                feilutbetaltBeløp = 2000.kroner,
                begrunnelse = null,
                foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_VURDERT,
                foreldelsesfrist = null,
                oppdagelsesdato = null,
            ),
        )

        tilbakekreving.håndter(behandler, 1.januar til 31.januar, Foreldelsesteg.Vurdering.IkkeForeldet(""))
        tilbakekreving.håndter(behandler, 1.februar til 28.februar, Foreldelsesteg.Vurdering.IkkeForeldet(""))

        val vilkårsvurderingsstegDto = tilbakekreving.behandlingHistorikk.nåværende().entry.vilkårsvurderingsstegDto.tilFrontendDto()
        vilkårsvurderingsstegDto.perioder shouldBe listOf(
            VurdertVilkårsvurderingsperiodeDto(
                periode = 1.januar til 31.januar,
                feilutbetaltBeløp = 2000.kroner,
                hendelsestype = Hendelsestype.ANNET,
                reduserteBeløper = emptyList(),
                aktiviteter = emptyList(),
                vilkårsvurderingsresultatInfo = null,
                begrunnelse = null,
                foreldet = false,
            ),
            VurdertVilkårsvurderingsperiodeDto(
                periode = 1.februar til 28.februar,
                feilutbetaltBeløp = 2000.kroner,
                hendelsestype = Hendelsestype.ANNET,
                reduserteBeløper = emptyList(),
                aktiviteter = emptyList(),
                vilkårsvurderingsresultatInfo = null,
                begrunnelse = null,
                foreldet = false,
            ),
        )
    }

    @Test
    fun `utvider ikke til full måned`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()

        val tilbakekreving = Tilbakekreving.opprett(
            UUID.randomUUID().toString(),
            BehovObservatørOppsamler(),
            opprettTilbakekrevingHendelse,
            BigQueryServiceStub(),
            EndringObservatørOppsamler(),
        )

        tilbakekreving.håndter(
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(3.januar til 3.januar),
                    kravgrunnlagPeriode(1.februar til 1.februar),
                ),
            ),
        )
        tilbakekreving.håndter(
            fagsysteminfoHendelse(
                utvidPerioder = listOf(
                    FagsysteminfoHendelse.UtvidetPeriode(
                        kravgrunnlagPeriode = 3.januar til 3.januar,
                        vedtaksperiode = 1.januar til 31.januar,
                    ),
                    FagsysteminfoHendelse.UtvidetPeriode(
                        kravgrunnlagPeriode = 1.februar til 1.februar,
                        vedtaksperiode = 1.februar til 14.februar,
                    ),
                ),
            ),
        )
        tilbakekreving.håndter(brukerinfoHendelse())

        val faktastegDto = tilbakekreving.faktastegFrontendDto()
        faktastegDto.feilutbetaltePerioder shouldBe listOf(
            FeilutbetalingsperiodeDto(
                periode = 1.januar til 31.januar,
                feilutbetaltBeløp = 2000.kroner,
                hendelsestype = Hendelsestype.ANNET,
                hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
            ),
            FeilutbetalingsperiodeDto(
                periode = 1.februar til 14.februar,
                feilutbetaltBeløp = 2000.kroner,
                hendelsestype = Hendelsestype.ANNET,
                hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
            ),
        )
        faktastegDto.totalFeilutbetaltPeriode shouldBe (1.januar til 14.februar)
    }
}
