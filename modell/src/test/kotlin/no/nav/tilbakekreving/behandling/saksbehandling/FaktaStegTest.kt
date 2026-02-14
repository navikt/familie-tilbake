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
import no.nav.tilbakekreving.kontrakter.frontend.models.OppdagetDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OppdaterFaktaPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RettsligGrunnlagDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.ytelsesbeløp
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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
        )
        val årsak = "Dette er årsaken til tilbakekrevingen"
        val uttalelse = "Ja hvorfor ikke"
        faktasteg.vurder(
            vurdering = Faktasteg.Vurdering(
                perioder = listOf(
                    Faktasteg.FaktaPeriode(
                        id = UUID.randomUUID(),
                        periode = periode,
                        rettsligGrunnlag = Hendelsestype.ANNET,
                        rettsligGrunnlagUnderkategori = Hendelsesundertype.ANNET_FRITEKST,
                    ),
                ),
                årsakTilFeilutbetaling = årsak,
                uttalelse = Faktasteg.Uttalelse.Ja(uttalelse),
                oppdaget = Faktasteg.Vurdering.Oppdaget.Vurdering(
                    dato = LocalDate.now(),
                    beskrivelse = "Hva som helst",
                    av = Faktasteg.Vurdering.Oppdaget.Av.Nav,
                    id = UUID.randomUUID(),
                ),
            ),
        )

        faktasteg.tilFrontendDto(kravgrunnlag, eksternFagsakRevurdering, Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).feilutbetaltePerioder shouldBe listOf(
            FeilutbetalingsperiodeDto(
                periode = periode,
                feilutbetaltBeløp = tilbakekrevesBeløp,
                hendelsestype = Hendelsestype.ANNET,
                hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
            ),
        )
        faktasteg.tilFrontendDto(kravgrunnlag, eksternFagsakRevurdering, Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).begrunnelse shouldBe årsak
        faktasteg.tilFrontendDto(kravgrunnlag, eksternFagsakRevurdering, Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse shouldBe VurderingAvBrukersUttalelseDto(
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
        )
        faktasteg.erFullstendig() shouldBe false

        faktasteg.vurder(
            vurdering = Faktasteg.Vurdering(
                perioder = listOf(
                    Faktasteg.FaktaPeriode(
                        id = UUID.randomUUID(),
                        periode = periode,
                        rettsligGrunnlag = Hendelsestype.ANNET,
                        rettsligGrunnlagUnderkategori = Hendelsesundertype.ANNET_FRITEKST,
                    ),
                ),
                årsakTilFeilutbetaling = "Årsak",
                uttalelse = Faktasteg.Uttalelse.Ja("Uttalelse"),
                oppdaget = Faktasteg.Vurdering.Oppdaget.Vurdering(
                    dato = LocalDate.now(),
                    beskrivelse = "Hva som helst",
                    av = Faktasteg.Vurdering.Oppdaget.Av.Nav,
                    id = UUID.randomUUID(),
                ),
            ),
        )

        faktasteg.erFullstendig() shouldBe true
    }

    @Test
    fun `vurdering av faktaperioder blir oppdatert`() {
        val periode = 1.januar til 1.januar
        val tilbakekrevesBeløp = 9000.kroner
        val bestemmelse = Hendelsestype.ANNET
        val grunnlag = Hendelsesundertype.ANNET_FRITEKST
        val revurdering = eksternFagsakBehandling()
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(kravgrunnlagPeriode(periode, ytelsesbeløp = ytelsesbeløp(tilbakekrevesBeløp = tilbakekrevesBeløp))),
        )

        val faktasteg = Faktasteg.opprett(
            eksternFagsakRevurdering = revurdering,
            kravgrunnlag = kravgrunnlag,
            brevHistorikk = BrevHistorikk(historikk = mutableListOf()),
        )
        val perioder = faktasteg.nyTilFrontendDto(
            kravgrunnlag = kravgrunnlag,
            revurdering = revurdering,
            varselbrev = null,
        ).perioder

        perioder.single().rettsligGrunnlag shouldBe listOf(
            RettsligGrunnlagDto(
                bestemmelse = bestemmelse.name,
                grunnlag = grunnlag.name,
            ),
        )

        faktasteg.vurder(
            listOf(
                OppdaterFaktaPeriodeDto(
                    id = perioder.single().id,
                    rettsligGrunnlag = listOf(
                        RettsligGrunnlagDto(
                            bestemmelse = Hendelsestype.VILKÅR_SØKER.name,
                            grunnlag = Hendelsesundertype.KONTANTSTØTTE.name,
                        ),
                    ),
                ),
            ),
        )

        faktasteg.nyTilFrontendDto(
            kravgrunnlag = kravgrunnlag,
            revurdering = revurdering,
            varselbrev = null,
        ).perioder.single().rettsligGrunnlag shouldBe listOf(
            RettsligGrunnlagDto(
                bestemmelse = Hendelsestype.VILKÅR_SØKER.name,
                grunnlag = Hendelsesundertype.KONTANTSTØTTE.name,
            ),
        )
    }

    @Test
    fun `vurdering av oppdaget blir oppdatert`() {
        val periode = 1.januar til 1.januar
        val tilbakekrevesBeløp = 9000.kroner
        val revurdering = eksternFagsakBehandling()
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(kravgrunnlagPeriode(periode, ytelsesbeløp = ytelsesbeløp(tilbakekrevesBeløp = tilbakekrevesBeløp))),
        )

        val faktasteg = Faktasteg.opprett(
            eksternFagsakRevurdering = revurdering,
            kravgrunnlag = kravgrunnlag,
            brevHistorikk = BrevHistorikk(historikk = mutableListOf()),
        )

        faktasteg.nyTilFrontendDto(
            kravgrunnlag = kravgrunnlag,
            revurdering = revurdering,
            varselbrev = null,
        ).vurdering.oppdaget shouldBe OppdagetDto(
            dato = null,
            av = OppdagetDto.Av.IKKE_VURDERT,
            beskrivelse = null,
        )

        val oppdagetDato = LocalDate.now()
        faktasteg.vurder(
            OppdagetDto(
                dato = oppdagetDato,
                beskrivelse = "beskrivelse",
                av = OppdagetDto.Av.NAV,
            ),
        )

        faktasteg.nyTilFrontendDto(
            kravgrunnlag = kravgrunnlag,
            revurdering = revurdering,
            varselbrev = null,
        ).vurdering.oppdaget shouldBe OppdagetDto(
            dato = oppdagetDato,
            av = OppdagetDto.Av.NAV,
            beskrivelse = "beskrivelse",
        )
    }
}
