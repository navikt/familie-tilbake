package no.nav.tilbakekreving.tilstand
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.VarselbrevJournalføringBehov
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.VarselbrevDistribueringHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevJournalføringHendelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.systemContext
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class SendVarselbrevTest {
    @Test
    fun `tilbakekreving tilstand endres til SendVarselbrev når forhåndsvarsel skal sendes`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingEvent,
            sideeffektContext = systemContext(
                behovObservatør = oppsamler,
                features = defaultFeatures(
                    featureOverrides = arrayOf(Toggle.SendAutomatiskVarselbrev to false),
                ),
            ),
        )
        val bruker = brukerinfoHendelse()
        val kravgrunnlag = kravgrunnlag()
        val fagsak = fagsysteminfoHendelse()
        tilbakekreving.håndter(kravgrunnlag, systemContext(behovObservatør = oppsamler))
        tilbakekreving.håndter(fagsak, systemContext(behovObservatør = oppsamler))
        tilbakekreving.håndter(bruker, systemContext(behovObservatør = oppsamler))
        tilbakekreving.tilstand shouldBe TilBehandling

        tilbakekreving.sendVarselbrev(
            tilbakekreving.nåværendeBehandlingId(),
            "tekst fra saksbehandler",
            saksbehandlerContext(behovObservatør = oppsamler),
        )
        tilbakekreving.tilstand shouldBe SendVarselbrev

        oppsamler.behovListe.size shouldBe 3

        val varselbrev = oppsamler.behovListe.filterIsInstance<VarselbrevJournalføringBehov>().shouldHaveSize(1)
        tilbakekreving.håndter(
            VarselbrevJournalføringHendelse(
                varselbrevId = varselbrev.first().info.id,
                journalpostId = "1234",
                dokumentInfoId = "321",
            ),
            systemContext(),
        )
        tilbakekreving.tilstand shouldBe DistribuerVarselbrev

        tilbakekreving.håndter(
            VarselbrevDistribueringHendelse(
                brevId = varselbrev.first().info.id,
                journalpostId = "1234",
                dokumentInfoId = "321",
            ),
            systemContext(),
        )
        tilbakekreving.tilstand shouldBe TilBehandling
    }

    @Test
    fun `tilbakekreving i SendVarselbrev tilstand sender journalføringBehov på nytt`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingEvent,
            sideeffektContext = systemContext(
                behovObservatør = oppsamler,
                features = defaultFeatures(
                    featureOverrides = arrayOf(
                        Toggle.SendAutomatiskVarselbrev to false,
                    ),
                ),
            ),
        )
        val bruker = brukerinfoHendelse()
        val kravgrunnlag = kravgrunnlag()
        val fagsak = fagsysteminfoHendelse()
        tilbakekreving.håndter(kravgrunnlag, systemContext(behovObservatør = oppsamler))
        tilbakekreving.håndter(fagsak, systemContext(behovObservatør = oppsamler))
        tilbakekreving.håndter(bruker, systemContext(behovObservatør = oppsamler))
        tilbakekreving.tilstand shouldBe TilBehandling

        tilbakekreving.sendVarselbrev(tilbakekreving.nåværendeBehandlingId(), "tekst fra saksbehandler", saksbehandlerContext(behovObservatør = oppsamler))
        tilbakekreving.tilstand shouldBe SendVarselbrev

        tilbakekreving.håndter(Påminnelse(LocalDateTime.now()), systemContext(behovObservatør = oppsamler))

        val varselbrevBehov = oppsamler.behovListe.filterIsInstance<VarselbrevJournalføringBehov>()

        varselbrevBehov.shouldHaveSize(2)
        varselbrevBehov.map { it.info.id }.distinct().shouldHaveSize(1)

        tilbakekreving.håndter(
            VarselbrevJournalføringHendelse(
                varselbrevId = varselbrevBehov.first().info.id,
                journalpostId = "1234",
                dokumentInfoId = "321",
            ),
            systemContext(),
        )
        tilbakekreving.tilstand shouldBe DistribuerVarselbrev

        tilbakekreving.håndter(
            VarselbrevDistribueringHendelse(
                brevId = varselbrevBehov.first().info.id,
                journalpostId = "1234",
                dokumentInfoId = "321",
            ),
            systemContext(),
        )
        tilbakekreving.tilstand shouldBe TilBehandling
    }
}
