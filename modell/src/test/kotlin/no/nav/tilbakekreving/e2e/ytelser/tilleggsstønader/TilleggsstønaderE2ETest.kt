package no.nav.tilbakekreving.e2e.ytelser.tilleggsstønader

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v1.dto.FeilutbetalingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelsesperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.util.UUID

class TilleggsstønaderE2ETest {
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
            features = defaultFeatures(),
        )

        tilbakekreving.håndter(
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(3.januar(2021) til 3.januar(2021)),
                    kravgrunnlagPeriode(1.februar(2021) til 1.februar(2021)),
                ),
            ),
        )
        tilbakekreving.håndter(
            fagsysteminfoHendelse(
                utvidPerioder = listOf(
                    FagsysteminfoHendelse.UtvidetPeriode(
                        kravgrunnlagPeriode = 3.januar(2021) til 3.januar(2021),
                        vedtaksperiode = 1.januar(2021) til 31.januar(2021),
                    ),
                    FagsysteminfoHendelse.UtvidetPeriode(
                        kravgrunnlagPeriode = 1.februar(2021) til 1.februar(2021),
                        vedtaksperiode = 1.februar(2021) til 28.februar(2021),
                    ),
                ),
            ),
        )
        tilbakekreving.håndter(brukerinfoHendelse())
        tilbakekreving.behandlingHistorikk.nåværende().entry.lagreUttalelse(UttalelseVurdering.JA, listOf(), "")
        val faktastegDto = tilbakekreving.faktastegFrontendDto()
        faktastegDto.feilutbetaltePerioder shouldBe listOf(
            FeilutbetalingsperiodeDto(
                periode = 1.januar(2021) til 31.januar(2021),
                feilutbetaltBeløp = 2000.kroner,
                hendelsestype = Hendelsestype.ANNET,
                hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
            ),
            FeilutbetalingsperiodeDto(
                periode = 1.februar(2021) til 28.februar(2021),
                feilutbetaltBeløp = 2000.kroner,
                hendelsestype = Hendelsestype.ANNET,
                hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
            ),
        )
        faktastegDto.totalFeilutbetaltPeriode shouldBe (1.januar(2021) til 28.februar(2021))

        tilbakekreving.håndter(behandler, faktastegVurdering(1.januar(2021) til 31.januar(2021)))
        tilbakekreving.håndter(behandler, faktastegVurdering(1.februar(2021) til 28.februar(2021)))

        val foreldelsesstegDto = tilbakekreving.behandlingHistorikk.nåværende().entry.foreldelsestegDto.tilFrontendDto()
        foreldelsesstegDto.foreldetPerioder shouldBe listOf(
            VurdertForeldelsesperiodeDto(
                periode = 1.januar(2021) til 31.januar(2021),
                feilutbetaltBeløp = 2000.kroner,
                begrunnelse = null,
                foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_VURDERT,
                foreldelsesfrist = null,
                oppdagelsesdato = null,
            ),
            VurdertForeldelsesperiodeDto(
                periode = 1.februar(2021) til 28.februar(2021),
                feilutbetaltBeløp = 2000.kroner,
                begrunnelse = null,
                foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_VURDERT,
                foreldelsesfrist = null,
                oppdagelsesdato = null,
            ),
        )

        tilbakekreving.håndter(behandler, 1.januar(2021) til 31.januar(2021), Foreldelsesteg.Vurdering.IkkeForeldet(""))
        tilbakekreving.håndter(behandler, 1.februar(2021) til 28.februar(2021), Foreldelsesteg.Vurdering.IkkeForeldet(""))

        val vilkårsvurderingsstegDto = tilbakekreving.behandlingHistorikk.nåværende().entry.vilkårsvurderingsstegDto.tilFrontendDto()
        vilkårsvurderingsstegDto.perioder shouldBe listOf(
            VurdertVilkårsvurderingsperiodeDto(
                periode = 1.januar(2021) til 31.januar(2021),
                feilutbetaltBeløp = 2000.kroner,
                hendelsestype = Hendelsestype.ANNET,
                reduserteBeløper = emptyList(),
                aktiviteter = emptyList(),
                vilkårsvurderingsresultatInfo = null,
                begrunnelse = null,
                foreldet = false,
            ),
            VurdertVilkårsvurderingsperiodeDto(
                periode = 1.februar(2021) til 28.februar(2021),
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
            features = defaultFeatures(),
        )

        tilbakekreving.håndter(
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(3.januar(2021) til 3.januar(2021)),
                    kravgrunnlagPeriode(1.februar(2021) til 1.februar(2021)),
                ),
            ),
        )
        tilbakekreving.håndter(
            fagsysteminfoHendelse(
                utvidPerioder = listOf(
                    FagsysteminfoHendelse.UtvidetPeriode(
                        kravgrunnlagPeriode = 3.januar(2021) til 3.januar(2021),
                        vedtaksperiode = 1.januar(2021) til 31.januar(2021),
                    ),
                    FagsysteminfoHendelse.UtvidetPeriode(
                        kravgrunnlagPeriode = 1.februar(2021) til 1.februar(2021),
                        vedtaksperiode = 1.februar(2021) til 14.februar(2021),
                    ),
                ),
            ),
        )
        tilbakekreving.håndter(brukerinfoHendelse())

        val faktastegDto = tilbakekreving.faktastegFrontendDto()
        faktastegDto.feilutbetaltePerioder shouldBe listOf(
            FeilutbetalingsperiodeDto(
                periode = 1.januar(2021) til 31.januar(2021),
                feilutbetaltBeløp = 2000.kroner,
                hendelsestype = Hendelsestype.ANNET,
                hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
            ),
            FeilutbetalingsperiodeDto(
                periode = 1.februar(2021) til 14.februar(2021),
                feilutbetaltBeløp = 2000.kroner,
                hendelsestype = Hendelsestype.ANNET,
                hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
            ),
        )
        faktastegDto.totalFeilutbetaltPeriode shouldBe (1.januar(2021) til 14.februar(2021))
    }
}
