package no.nav.tilbakekreving.tilstand

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.VarselbrevJournalføringBehov
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.VarselbrevDistribueringHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevJournalføringHendelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class SendVarselbrevTest {
    private val bigQueryService = BigQueryServiceStub()

    @Test
    fun `tilbakekreving ved AutomatiskVarselbrev er i SendVarselbrev tilstand når bruker er håndtert`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), oppsamler, opprettTilbakekrevingEvent, bigQueryService, EndringObservatørOppsamler(), features = defaultFeatures(Toggle.SendAutomatiskVarselbrev to true))
        val bruker = brukerinfoHendelse()
        val kravgrunnlag = kravgrunnlag()
        val fagsak = fagsysteminfoHendelse()
        tilbakekreving.håndter(kravgrunnlag)
        tilbakekreving.håndter(fagsak)
        tilbakekreving.håndter(bruker)

        tilbakekreving.tilstand shouldBe SendVarselbrev
    }

    @Test
    fun `tilbakekreving tilstand endres til SendVarselbrev når forhåndsvarsel skal sendes`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), oppsamler, opprettTilbakekrevingEvent, bigQueryService, EndringObservatørOppsamler(), features = defaultFeatures(Toggle.SendAutomatiskVarselbrev to false))
        val bruker = brukerinfoHendelse()
        val kravgrunnlag = kravgrunnlag()
        val fagsak = fagsysteminfoHendelse()
        tilbakekreving.håndter(kravgrunnlag)
        tilbakekreving.håndter(fagsak)
        tilbakekreving.håndter(bruker)
        tilbakekreving.tilstand shouldBe TilBehandling

        tilbakekreving.trengerVarselbrev("tekst fra saksbehandler")
        tilbakekreving.tilstand shouldBe SendVarselbrev

        oppsamler.behovListe.size shouldBe 3

        oppsamler.behovListe.filterIsInstance<VarselbrevJournalføringBehov>().shouldHaveSize(1)

        tilbakekreving.håndter(
            VarselbrevJournalføringHendelse(
                varselbrevId = tilbakekreving.brevHistorikk.sisteVarselbrev()!!.id,
                behandlingId = tilbakekreving.behandlingHistorikk.nåværende().entry.id,
                journalpostId = "1234",
                behandlerIdent = "4321",
            ),
        )
        tilbakekreving.tilstand shouldBe DistribuerVarselbrev

        tilbakekreving.håndter(
            VarselbrevDistribueringHendelse(
                behandlingId = tilbakekreving.behandlingHistorikk.nåværende().entry.id,
                behandlerIdent = "4321",
            ),
        )
        tilbakekreving.tilstand shouldBe TilBehandling
    }

    @Test
    fun `tilbakekreving i SendVarselbrev tilstand sender journalføringBehov på nytt`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), oppsamler, opprettTilbakekrevingEvent, bigQueryService, EndringObservatørOppsamler(), features = defaultFeatures(Toggle.SendAutomatiskVarselbrev to false))
        val bruker = brukerinfoHendelse()
        val kravgrunnlag = kravgrunnlag()
        val fagsak = fagsysteminfoHendelse()
        tilbakekreving.håndter(kravgrunnlag)
        tilbakekreving.håndter(fagsak)
        tilbakekreving.håndter(bruker)
        tilbakekreving.tilstand shouldBe TilBehandling

        tilbakekreving.trengerVarselbrev("tekst fra saksbehandler")
        tilbakekreving.tilstand shouldBe SendVarselbrev

        tilbakekreving.håndter(Påminnelse(LocalDateTime.now()))

        val varselbrevBehov = oppsamler.behovListe.filterIsInstance<VarselbrevJournalføringBehov>()

        varselbrevBehov.shouldHaveSize(2)
        varselbrevBehov.map { it.brevId }.distinct().shouldHaveSize(1)

        tilbakekreving.håndter(
            VarselbrevJournalføringHendelse(
                varselbrevId = tilbakekreving.brevHistorikk.sisteVarselbrev()!!.id,
                behandlingId = tilbakekreving.behandlingHistorikk.nåværende().entry.id,
                journalpostId = "1234",
                behandlerIdent = "4321",
            ),
        )
        tilbakekreving.tilstand shouldBe DistribuerVarselbrev

        tilbakekreving.håndter(
            VarselbrevDistribueringHendelse(
                behandlingId = tilbakekreving.behandlingHistorikk.nåværende().entry.id,
                behandlerIdent = "4321",
            ),
        )
        tilbakekreving.tilstand shouldBe TilBehandling
    }
}
