package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.shouldBe
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
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(periode, ytelsesbeløp = ytelsesbeløp(tilbakekrevesBeløp = tilbakekrevesBeløp)),
            ),
        )
        val eksternFagsakRevurdering = eksternFagsakBehandling()
        val faktasteg = Faktasteg.opprett(
            eksternFagsakRevurdering = eksternFagsakRevurdering,
            kravgrunnlag = kravgrunnlag,
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
                        rettsligGrunnlag = Hendelsestype.ANNET,
                        rettsligGrunnlagUnderkategori = Hendelsesundertype.ANNET_FRITEKST,
                    ),
                ),
                årsakTilFeilutbetaling = årsak,
                uttalelse = Faktasteg.Uttalelse.Ja(uttalelse),
            ),
        )

        faktasteg.tilFrontendDto(kravgrunnlag, eksternFagsakRevurdering).feilutbetaltePerioder shouldBe listOf(
            FeilutbetalingsperiodeDto(
                periode = periode,
                feilutbetaltBeløp = tilbakekrevesBeløp,
                hendelsestype = Hendelsestype.ANNET,
                hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
            ),
        )
        faktasteg.tilFrontendDto(kravgrunnlag, eksternFagsakRevurdering).begrunnelse shouldBe årsak
        faktasteg.tilFrontendDto(kravgrunnlag, eksternFagsakRevurdering).vurderingAvBrukersUttalelse shouldBe VurderingAvBrukersUttalelseDto(
            harBrukerUttaltSeg = HarBrukerUttaltSeg.JA,
            beskrivelse = uttalelse,
        )
    }

    @Test
    fun `fakta-steget er fullstendig når det er vurdert`() {
        val periode = 1.januar til 1.januar
        val tilbakekrevesBeløp = 2000.kroner
        val faktasteg = Faktasteg.opprett(
            eksternFagsakRevurdering = eksternFagsakBehandling(),
            kravgrunnlag = kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(periode, ytelsesbeløp = ytelsesbeløp(tilbakekrevesBeløp = tilbakekrevesBeløp)),
                ),
            ),
            brevHistorikk = BrevHistorikk(historikk = mutableListOf()),
            tilbakekrevingOpprettet = LocalDateTime.now(),
            opprettelsesvalg = Opprettelsesvalg.OPPRETT_BEHANDLING_MED_VARSEL,
        )
        faktasteg.erFullstendig() shouldBe false

        faktasteg.vurder(
            vurdering = Faktasteg.Vurdering(
                perioder = listOf(
                    Faktasteg.FaktaPeriode(
                        periode = periode,
                        rettsligGrunnlag = Hendelsestype.ANNET,
                        rettsligGrunnlagUnderkategori = Hendelsesundertype.ANNET_FRITEKST,
                    ),
                ),
                årsakTilFeilutbetaling = "Årsak",
                uttalelse = Faktasteg.Uttalelse.Ja("Uttalelse"),
            ),
        )

        faktasteg.erFullstendig() shouldBe true
    }
}
