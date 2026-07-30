package no.nav.tilbakekreving.entities

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.ytelsesbeløp
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class FaktastegEntityTest {
    @Test
    fun `vurdering av fakta steg blir lagret`() {
        val periode = 1.januar(2021) til 1.januar(2021)
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
                    beskrivelse = "Oppdaget beskrivelse",
                    av = Faktasteg.Vurdering.Oppdaget.Av.Nav,
                    id = UUID.randomUUID(),
                ),
            ),
        )

        val tilbakekrevingOpprettet = LocalDateTime.now()

        val dtoFør = faktasteg.tilFrontendDto(kravgrunnlag, eksternFagsakRevurdering, Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, tilbakekrevingOpprettet)

        val dtoEtter = faktasteg.tilEntity(UUID.randomUUID()).fraEntity(brevHistorikk).tilFrontendDto(kravgrunnlag, eksternFagsakRevurdering, Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, tilbakekrevingOpprettet)

        dtoEtter shouldBe dtoFør
    }

    @Test
    fun `faktasteg med feil rekkefølge på perioder i db blir gjenopprettet i riktig rekkefølge`() {
        val behandlingRef = UUID.randomUUID()
        val periode1 = DatoperiodeEntity(1.januar(2021), 10.januar(2021))
        val periode2 = DatoperiodeEntity(15.januar(2021), 20.januar(2021))
        val entity = FaktastegEntity(
            id = UUID.randomUUID(),
            behandlingRef = behandlingRef,
            perioder = listOf(
                FaktastegEntity.FaktaPeriodeEntity(
                    id = UUID.randomUUID(),
                    faktavurderingRef = UUID.randomUUID(),
                    periode = periode2,
                    rettsligGrunnlag = Hendelsestype.ANNET,
                    rettsligGrunnlagUnderkategori = Hendelsesundertype.ANNET_FRITEKST,
                ),
                FaktastegEntity.FaktaPeriodeEntity(
                    id = UUID.randomUUID(),
                    faktavurderingRef = UUID.randomUUID(),
                    periode = periode1,
                    rettsligGrunnlag = Hendelsestype.ANNET,
                    rettsligGrunnlagUnderkategori = Hendelsesundertype.ANNET_FRITEKST,
                ),
            ),
            årsakTilFeilutbetaling = "Årsak",
            uttalelse = FaktastegEntity.Uttalelse.Nei,
            vurderingAvBrukersUttalelse = null,
            oppdaget = null,
            trengerNyVurdering = false,
            rettsgebyrÅrFraSaksbehandler = null,
        )

        val gjenopprettetEntity = entity.fraEntity(BrevHistorikk(mutableListOf())).tilEntity(behandlingRef)

        gjenopprettetEntity.perioder[0].periode shouldBe periode1
        gjenopprettetEntity.perioder[1].periode shouldBe periode2
    }
}
