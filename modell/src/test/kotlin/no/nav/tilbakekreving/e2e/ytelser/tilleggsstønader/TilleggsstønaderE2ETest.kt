package no.nav.tilbakekreving.e2e.ytelser.tilleggsstønader
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v1.dto.FeilutbetalingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelsesperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.util.UUID

class TilleggsstønaderE2ETest {
    @Test
    fun `utvidelse av feilutbetalingsperiode`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()

        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse,
            sideeffektContext = systemContext(),
        )

        tilbakekreving.håndter(
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(3.januar(2021) til 3.januar(2021)),
                    kravgrunnlagPeriode(1.februar(2021) til 1.februar(2021)),
                ),
            ),
            systemContext(),
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
            systemContext(),
        )
        tilbakekreving.håndter(brukerinfoHendelse(), systemContext())
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            lagreUttalelse(UttalelseVurdering.JA, null, "")
            val faktastegDto = tilbakekreving.faktastegFrontendDto(tilbakekreving.nåværendeBehandlingId())
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
            vurderFakta(faktastegVurdering(1.januar(2021) til 31.januar(2021)))
            vurderFakta(faktastegVurdering(1.februar(2021) til 28.februar(2021)))
            val foreldelsesstegDto = tilbakekreving.hentBehandling(tilbakekreving.nåværendeBehandlingId()).foreldelsestegDto.tilFrontendDto(saksbehandlerContext())
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
            vurderForeldelse(1.januar(2021) til 31.januar(2021), Foreldelsesteg.Vurdering.IkkeForeldet(""))
            vurderForeldelse(1.februar(2021) til 28.februar(2021), Foreldelsesteg.Vurdering.IkkeForeldet(""))
        }

        val vilkårsvurderingsstegDto = tilbakekreving.hentBehandling(tilbakekreving.nåværendeBehandlingId()).vilkårsvurderingsstegDto.tilFrontendDto(saksbehandlerContext())
        vilkårsvurderingsstegDto.perioder shouldBe listOf(
            VurdertVilkårsvurderingsperiodeDto(
                periode = 1.januar(2021) til 28.februar(2021),
                feilutbetaltBeløp = 4000.kroner,
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
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse,
            sideeffektContext = systemContext(),
        )

        tilbakekreving.håndter(
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(3.januar(2021) til 3.januar(2021)),
                    kravgrunnlagPeriode(1.februar(2021) til 1.februar(2021)),
                ),
            ),
            systemContext(),
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
            systemContext(),
        )
        tilbakekreving.håndter(brukerinfoHendelse(), systemContext())

        val faktastegDto = tilbakekreving.faktastegFrontendDto(tilbakekreving.nåværendeBehandlingId())
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
