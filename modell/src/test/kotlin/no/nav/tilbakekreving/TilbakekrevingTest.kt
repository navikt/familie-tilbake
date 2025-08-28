package no.nav.tilbakekreving

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.tilstand.AvventerKravgrunnlag
import no.nav.tilbakekreving.tilstand.AvventerUtsattBehandlingMedVarsel
import no.nav.tilbakekreving.tilstand.AvventerUtsattBehandlingUtenVarsel
import org.junit.jupiter.api.Test
import kotlin.collections.listOf

class TilbakekrevingTest {
    private val bigQueryService = BigQueryServiceStub()
    private val endringObservatør = EndringObservatørOppsamler()

    @Test
    fun `oppretter tilbakekreving`() {
        val opprettEvent = opprettTilbakekrevingHendelse(opprettelsesvalg = Opprettelsesvalg.OPPRETT_BEHANDLING_MED_VARSEL)
        val tilbakekreving = Tilbakekreving.opprett(BehovObservatørOppsamler(), opprettEvent, bigQueryService, endringObservatør)

        tilbakekreving.tilstand shouldBe AvventerKravgrunnlag
    }

    @Test
    fun `oppretter tilbakekreving som avventer oppdatert kravgrunnlag uten varsel`() {
        val opprettEvent = opprettTilbakekrevingHendelse(opprettelsesvalg = Opprettelsesvalg.UTSETT_BEHANDLING_UTEN_VARSEL)
        val tilbakekreving = Tilbakekreving.opprett(BehovObservatørOppsamler(), opprettEvent, bigQueryService, endringObservatør)

        tilbakekreving.tilstand shouldBe AvventerUtsattBehandlingUtenVarsel
    }

    @Test
    fun `oppretter tilbakekreving som avventer oppdatert kravgrunnlag med varsel`() {
        val opprettEvent = opprettTilbakekrevingHendelse(opprettelsesvalg = Opprettelsesvalg.UTSETT_BEHANDLING_MED_VARSEL)
        val tilbakekreving = Tilbakekreving.opprett(BehovObservatørOppsamler(), opprettEvent, bigQueryService, endringObservatør)

        tilbakekreving.tilstand shouldBe AvventerUtsattBehandlingMedVarsel
    }

    @Test
    fun `vurdering fra saksbehandler fører til at informasjon om endring blir delt`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse(
            eksternFagsak = eksternFagsak(
                ytelse = Ytelse.Tilleggsstønad,
            ),
        )
        val tilbakekreving = Tilbakekreving.opprett(BehovObservatørOppsamler(), opprettTilbakekrevingHendelse, bigQueryService, endringObservatør)
        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(brukerinfoHendelse())
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, faktastegVurdering())
        endringObservatør.statusoppdateringerFor(tilbakekreving.behandlingHistorikk.nåværende().entry.internId) shouldBe listOf(
            EndringObservatørOppsamler.Statusoppdatering(
                ansvarligSaksbehandler = Behandler.Vedtaksløsning.ident,
                vedtaksresultat = null,
            ),
            EndringObservatørOppsamler.Statusoppdatering(
                ansvarligSaksbehandler = ANSVARLIG_SAKSBEHANDLER.ident,
                vedtaksresultat = null,
            ),
        )
    }
}
