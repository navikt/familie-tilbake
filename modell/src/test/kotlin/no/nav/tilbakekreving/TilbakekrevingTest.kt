package no.nav.tilbakekreving

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.api.v2.Opprettelsevalg
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.tilstand.AvventerKravgrunnlag
import no.nav.tilbakekreving.tilstand.AvventerUtsattBehandlingMedVarsel
import no.nav.tilbakekreving.tilstand.AvventerUtsattBehandlingUtenVarsel
import no.nav.tilbakekreving.tilstand.Start
import org.junit.jupiter.api.Test

class TilbakekrevingTest {
    @Test
    fun `oppretter tilbakekreving`() {
        val opprettEvent = opprettTilbakekrevingEvent(opprettelsevalg = Opprettelsevalg.OPPRETT_BEHANDLING_MED_VARSEL)
        val tilbakekreving = Tilbakekreving.opprett(BehovObservatørOppsamler(), opprettEvent)

        tilbakekreving.tilstand shouldBe Start
        tilbakekreving.håndter(opprettEvent)
        tilbakekreving.tilstand shouldBe AvventerKravgrunnlag
    }

    @Test
    fun `oppretter tilbakekreving som avventer oppdatert kravgrunnlag uten varsel`() {
        val opprettEvent = opprettTilbakekrevingEvent(opprettelsevalg = Opprettelsevalg.UTSETT_BEHANDLING_UTEN_VARSEL)
        val tilbakekreving = Tilbakekreving.opprett(BehovObservatørOppsamler(), opprettEvent)

        tilbakekreving.håndter(opprettEvent)
        tilbakekreving.tilstand shouldBe AvventerUtsattBehandlingUtenVarsel
    }

    @Test
    fun `oppretter tilbakekreving som avventer oppdatert kravgrunnlag med varsel`() {
        val opprettEvent = opprettTilbakekrevingEvent(opprettelsevalg = Opprettelsevalg.UTSETT_BEHANDLING_MED_VARSEL)
        val tilbakekreving = Tilbakekreving.opprett(BehovObservatørOppsamler(), opprettEvent)

        tilbakekreving.håndter(opprettEvent)
        tilbakekreving.tilstand shouldBe AvventerUtsattBehandlingMedVarsel
    }
}
