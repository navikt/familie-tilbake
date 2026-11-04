package no.nav.tilbakekreving.statusmeldinger

import io.kotest.inspectors.forSingle
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.FeatureToggles
import no.nav.tilbakekreving.KlokkeStub
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.InfobrevJournalføringBehov
import no.nav.tilbakekreving.bortfaltStatusmelding
import no.nav.tilbakekreving.brev.Informasjonsbrev
import no.nav.tilbakekreving.brev.Informasjonsbrevtype
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.sendOgDistribuerForhåndsvarsel
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.juni
import no.nav.tilbakekreving.test.mai
import no.nav.tilbakekreving.tilbakekrevingTilBehandling
import no.nav.tilbakekreving.tilstand.JournalførInfobrevForBortfaltKrav
import no.nav.tilbakekreving.tilstand.KravgrunnlagBortfalt
import no.nav.tilbakekreving.tilstand.TilBehandling
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InformasjonsbrevBortfaltKravTest {
    @Test
    fun `bortfalt krav etter sendt forhåndsvarsel`() {
        val oppsamler = BehovObservatørOppsamler()
        val tilbakekreving = tilbakekrevingMedForhåndsvarsel(varsletDato = 17.mai(2026))

        tilbakekreving.håndter(
            bortfaltStatusmelding(),
            systemContext(behovObservatør = oppsamler, features = kravBortfaltPå(), klokke = KlokkeStub(2.juni(2026))),
        )

        tilbakekreving.tilstand shouldBe JournalførInfobrevForBortfaltKrav
        val informasjonsbrev = tilbakekreving.brevHistorikk.nåværende().entry.shouldBeInstanceOf<Informasjonsbrev>()
        informasjonsbrev.type shouldBe Informasjonsbrevtype.BORTFALT_KRAV
        informasjonsbrev.journalpostId.shouldBeNull()
        informasjonsbrev.sendtTid shouldBe 2.juni(2026)

        oppsamler.behovListe.filterIsInstance<InfobrevJournalføringBehov>().forSingle { behov ->
            behov.brevId shouldBe informasjonsbrev.id
            behov.behandlingId shouldBe tilbakekreving.nåværendeBehandlingId()
            behov.ytelse shouldBe Ytelse.Barnetrygd
            behov.fagsakId shouldBe tilbakekreving.eksternFagsak.eksternId()
            behov.journalførendeEnhet shouldBe "0425"
            behov.infobrevData.tittel shouldBe "Nav har avsluttet saken din om tilbakebetaling"
            behov.infobrevData.innhold shouldBe listOf(
                "Vi sendte deg et varsel 17. mai 2026 om at du hadde fått for mye utbetalt i barnetrygd. " +
                    "Vi har i ettertid avsluttet saken om tilbakebetaling fordi det ikke lenger er et feilutbetalt beløp å betale tilbake.",
            )
            behov.infobrevData.underavsnitt.forSingle { it.tittel shouldBe "Har du spørsmål?" }
            behov.infobrevData.enhetNavn shouldBe "Nav Solør"
            behov.infobrevData.sendtDato shouldBe 2.juni(2026)
            behov.infobrevData.tilbakekrevingId shouldBe tilbakekreving.id
        }
    }

    @Test
    fun `bortfalt krav uten sendt forhåndsvarsel`() {
        val oppsamler = BehovObservatørOppsamler()
        val tilbakekreving = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse())
        tilbakekreving.tilstand shouldBe TilBehandling

        tilbakekreving.håndter(
            bortfaltStatusmelding(),
            systemContext(behovObservatør = oppsamler, features = kravBortfaltPå()),
        )

        tilbakekreving.tilstand shouldBe KravgrunnlagBortfalt
        oppsamler.behovListe.filterIsInstance<InfobrevJournalføringBehov>().shouldBeEmpty()
    }

    private fun kravBortfaltPå(): FeatureToggles = defaultFeatures(featureOverrides = arrayOf(Toggle.KravBortfalt to true))

    private fun tilbakekrevingMedForhåndsvarsel(varsletDato: LocalDate): Tilbakekreving {
        val tilbakekreving = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse())
        tilbakekreving.sendOgDistribuerForhåndsvarsel(saksbehandlerContext(klokke = KlokkeStub(varsletDato)))
        tilbakekreving.tilstand shouldBe TilBehandling
        return tilbakekreving
    }
}
