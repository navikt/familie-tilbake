package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.HistorikkStub.Companion.fakeReferanse
import no.nav.tilbakekreving.api.v1.dto.FeilutbetalingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurderingAvBrukersUttalelseDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.ytelsesbeløp
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FaktaStegTest {
    @Test
    fun `send inn fakta blir vist i dto-en`() {
        val periode = 1.januar til 1.januar
        val tilbakekrevesBeløp = 2000.kroner
        val faktasteg = Faktasteg.opprett(
            eksternFagsakBehandling = fakeReferanse(eksternFagsakBehandling()),
            kravgrunnlag = fakeReferanse(
                kravgrunnlag(
                    perioder = listOf(
                        kravgrunnlagPeriode(periode, ytelsesbeløp = ytelsesbeløp(tilbakekrevesBeløp = tilbakekrevesBeløp)),
                    ),
                ),
            ),
            BrevHistorikk(historikk = mutableListOf()),
            tilbakekrevingOpprettet = LocalDateTime.now(),
            Opprettelsesvalg.OPPRETT_BEHANDLING_MED_VARSEL,
        )
        val årsak = "Dette er årsaken til tilbakekrevingen"
        val uttalelse = "Ja hvorfor ikke"
        faktasteg.vurder(
            vurdering = Faktasteg.Vurdering(
                perioder = listOf(
                    Faktasteg.FaktaPeriode(
                        periode = periode,
                        hendelsestype = Hendelsestype.ANNET,
                        hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
                    ),
                ),
                årsak = årsak,
                uttalelse = Faktasteg.Uttalelse.Ja(uttalelse),
            ),
        )

        faktasteg.tilFrontendDto().feilutbetaltePerioder shouldBe listOf(
            FeilutbetalingsperiodeDto(
                periode = periode,
                feilutbetaltBeløp = tilbakekrevesBeløp,
                hendelsestype = Hendelsestype.ANNET,
                hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
            ),
        )
        faktasteg.tilFrontendDto().begrunnelse shouldBe årsak
        faktasteg.tilFrontendDto().vurderingAvBrukersUttalelse shouldBe VurderingAvBrukersUttalelseDto(
            harBrukerUttaltSeg = HarBrukerUttaltSeg.JA,
            beskrivelse = uttalelse,
        )
    }
}
