package no.nav.tilbakekreving.entities

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.ytelsesbeløp
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FaktastegEntityTest {
    @Test
    fun `vurdering av fakta steg blir lagret`() {
        val periode = 1.januar til 1.januar
        val tilbakekrevesBeløp = 2000.kroner
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(periode, ytelsesbeløp = ytelsesbeløp(tilbakekrevesBeløp = tilbakekrevesBeløp)),
            ),
        )
        val eksternFagsakRevurdering = eksternFagsakBehandling()
        val brevHistorikk = BrevHistorikk(historikk = mutableListOf())
        val faktasteg = Faktasteg.opprett(
            eksternFagsakRevurdering = eksternFagsakRevurdering,
            kravgrunnlag = kravgrunnlag,
            brevHistorikk = brevHistorikk,
            tilbakekrevingOpprettet = LocalDateTime.now(),
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

        val dtoFør = faktasteg.tilFrontendDto(kravgrunnlag, eksternFagsakRevurdering, Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)

        val dtoEtter = faktasteg.tilEntity().fraEntity(brevHistorikk).tilFrontendDto(kravgrunnlag, eksternFagsakRevurdering, Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)

        dtoEtter shouldBe dtoFør
    }
}
