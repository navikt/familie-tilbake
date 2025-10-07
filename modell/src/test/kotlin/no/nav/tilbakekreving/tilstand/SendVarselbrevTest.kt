package no.nav.tilbakekreving.tilstand

import io.kotest.inspectors.forOne
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.varselbrev
import org.junit.jupiter.api.Test
import java.util.UUID

class SendVarselbrevTest {
    private val bigQueryService = BigQueryServiceStub()

    @Test
    fun `tilbakekreving i SendVarselbrev går videre med Kravgrunnlag`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), oppsamler, opprettTilbakekrevingEvent, bigQueryService, EndringObservatørOppsamler())

        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        tilbakekreving.håndter(brukerinfoHendelse())
        println(oppsamler)
        tilbakekreving.håndter(VarselbrevSendtHendelse(varselbrevId = tilbakekreving.brevHistorikk.nåværende().entry.id, journalpostId = "1234"))

        tilbakekreving.tilstand shouldBe TilBehandling

        val varselbrevITilbakekreving = tilbakekreving.brevHistorikk.nåværende().entry as Varselbrev

        println(oppsamler)
        oppsamler.behovListe.forOne {
            it shouldBeEqual
                VarselbrevBehov(
                    brevId = varselbrevITilbakekreving.id,
                    varselbrev = Varselbrev(
                        id = varselbrevITilbakekreving.id,
                        opprettetDato = varselbrevITilbakekreving.opprettetDato,
                        brevInformasjon = varselbrevITilbakekreving.brevInformasjon,
                        journalpostId = "1234",
                        varsletBeløp = varselbrevITilbakekreving.varsletBeløp,
                        revurderingsvedtaksdato = varselbrevITilbakekreving.revurderingsvedtaksdato,
                        fristdatoForTilbakemelding = varselbrevITilbakekreving.fristdatoForTilbakemelding,
                        varseltekstFraSaksbehandler = varselbrevITilbakekreving.varseltekstFraSaksbehandler,
                        feilutbetaltePerioder = varselbrevITilbakekreving.feilutbetaltePerioder,
                    ),
                )
        }
    }
}
