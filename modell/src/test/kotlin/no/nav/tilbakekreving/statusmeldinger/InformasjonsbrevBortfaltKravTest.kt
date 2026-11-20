package no.nav.tilbakekreving.statusmeldinger

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forSingle
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.FeatureToggles
import no.nav.tilbakekreving.KlokkeStub
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.behandling.TilbakekrevingRevurderingsårsak
import no.nav.tilbakekreving.behandling.VedtakOmgjortAv
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.InfobrevDistribusjonBehov
import no.nav.tilbakekreving.behov.InfobrevJournalføringBehov
import no.nav.tilbakekreving.bortfaltStatusmelding
import no.nav.tilbakekreving.brev.Informasjonsbrev
import no.nav.tilbakekreving.brev.Informasjonsbrevtype
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.hendelse.InfobrevDistribusjonHendelse
import no.nav.tilbakekreving.hendelse.InfobrevJournalføringHendelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekreving
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.sendOgDistribuerForhåndsvarsel
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.juni
import no.nav.tilbakekreving.test.mai
import no.nav.tilbakekreving.tilbakekrevingTilBehandling
import no.nav.tilbakekreving.tilstand.AvsluttetUtenVedtak
import no.nav.tilbakekreving.tilstand.AvventerBrukerinfo
import no.nav.tilbakekreving.tilstand.DistribuerInfobrevForBortfaltKrav
import no.nav.tilbakekreving.tilstand.JournalførInfobrevForBortfaltKrav
import no.nav.tilbakekreving.tilstand.KravgrunnlagBortfalt
import no.nav.tilbakekreving.tilstand.TilBehandling
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
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
    fun `bortfalt krav uten forhåndsvarsel og uten at bruker er informert avslutter saken`() {
        val oppsamler = BehovObservatørOppsamler()
        val tilbakekreving = tilbakekrevingTilBehandling(
            opprettTilbakekrevingHendelse(),
            fagsysteminfo = fagsysteminfoHendelse(brukerInformertOmFeilutbetaling = false),
        )
        tilbakekreving.tilstand shouldBe TilBehandling

        tilbakekreving.håndter(
            bortfaltStatusmelding(),
            systemContext(behovObservatør = oppsamler, features = kravBortfaltPå()),
        )

        tilbakekreving.tilstand shouldBe AvsluttetUtenVedtak
        oppsamler.behovListe.filterIsInstance<InfobrevJournalføringBehov>().shouldBeEmpty()
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = [true])
    fun `bortfalt krav uten forhåndsvarsel når bruker er informert eller det er ukjent om bruker er informert`(
        brukerInformertOmFeilutbetaling: Boolean?,
    ) {
        val oppsamler = BehovObservatørOppsamler()
        val tilbakekreving = tilbakekrevingTilBehandling(
            opprettTilbakekrevingHendelse(),
            fagsysteminfo = fagsysteminfoHendelse(brukerInformertOmFeilutbetaling = brukerInformertOmFeilutbetaling),
        )
        tilbakekreving.tilstand shouldBe TilBehandling

        tilbakekreving.håndter(
            bortfaltStatusmelding(),
            systemContext(behovObservatør = oppsamler, features = kravBortfaltPå()),
        )

        tilbakekreving.tilstand shouldBe KravgrunnlagBortfalt
        oppsamler.behovListe.filterIsInstance<InfobrevJournalføringBehov>().shouldBeEmpty()
    }

    @Test
    fun `bortfalt krav før brukerinfo avgjøres etter regelen når brukerinfo kommer`() {
        val tilbakekreving = opprettTilbakekreving(opprettTilbakekrevingHendelse()).apply {
            håndter(kravgrunnlag(), systemContext())
            håndter(fagsysteminfoHendelse(brukerInformertOmFeilutbetaling = false), systemContext())
        }
        tilbakekreving.tilstand shouldBe AvventerBrukerinfo

        tilbakekreving.håndter(bortfaltStatusmelding(), systemContext(features = kravBortfaltPå()))
        tilbakekreving.tilstand shouldBe AvventerBrukerinfo

        tilbakekreving.håndter(brukerinfoHendelse(), systemContext(features = kravBortfaltPå()))

        tilbakekreving.tilstand shouldBe AvsluttetUtenVedtak
    }

    @Test
    fun `journalføring og distribusjon av informasjonsbrev om bortfalt krav`() {
        val oppsamler = BehovObservatørOppsamler()
        val tilbakekreving = tilbakekrevingMedForhåndsvarsel(varsletDato = 17.mai(2026))
        tilbakekreving.håndter(bortfaltStatusmelding(), systemContext(features = kravBortfaltPå()))
        val brevId = tilbakekreving.brevHistorikk.nåværende().entry.id

        tilbakekreving.håndter(
            InfobrevJournalføringHendelse(infobrevId = brevId, journalpostId = "infobrev-journalpost", dokumentInfoId = "infobrev-dokument"),
            systemContext(behovObservatør = oppsamler),
        )

        tilbakekreving.tilstand shouldBe DistribuerInfobrevForBortfaltKrav
        oppsamler.behovListe.filterIsInstance<InfobrevDistribusjonBehov>().forSingle { behov ->
            behov.brevId shouldBe brevId
            behov.journalpostId shouldBe "infobrev-journalpost"
            behov.dokumentInfoId shouldBe "infobrev-dokument"
        }

        tilbakekreving.håndter(
            InfobrevDistribusjonHendelse(infobrevId = brevId, journalpostId = "infobrev-journalpost", dokumentInfoId = "infobrev-dokument"),
            systemContext(),
        )

        tilbakekreving.tilstand shouldBe AvsluttetUtenVedtak
    }

    @Test
    fun `sak avsluttet uten vedtak kan ikke revurderes`() {
        val tilbakekreving = tilbakekrevingMedForhåndsvarsel(varsletDato = 17.mai(2026))
        tilbakekreving.håndter(bortfaltStatusmelding(), systemContext(features = kravBortfaltPå()))
        val brevId = tilbakekreving.brevHistorikk.nåværende().entry.id
        tilbakekreving.håndter(InfobrevJournalføringHendelse(brevId, "infobrev-journalpost", "infobrev-dokument"), systemContext())
        tilbakekreving.håndter(InfobrevDistribusjonHendelse(brevId, "infobrev-journalpost", "infobrev-dokument"), systemContext())
        tilbakekreving.tilstand shouldBe AvsluttetUtenVedtak

        shouldThrow<ModellFeil.UgyldigOperasjonException> {
            tilbakekreving.opprettRevurdering(
                TilbakekrevingRevurderingsårsak.EtterKlage(
                    vedtakOmgjortAv = VedtakOmgjortAv.Klageinstansen,
                    beskrivelse = "Begrunnelse fra klageinstansen",
                ),
                saksbehandlerContext(features = defaultFeatures(featureOverrides = arrayOf(Toggle.Revurdering to true))),
            )
        }.message shouldBe "Kan ikke hente kravgrunnlag for revurdering i AVSLUTTET_UTEN_VEDTAK"
    }

    private fun kravBortfaltPå(): FeatureToggles = defaultFeatures(featureOverrides = arrayOf(Toggle.KravBortfalt to true))

    private fun tilbakekrevingMedForhåndsvarsel(varsletDato: LocalDate): Tilbakekreving {
        val tilbakekreving = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse())
        tilbakekreving.sendOgDistribuerForhåndsvarsel(saksbehandlerContext(klokke = KlokkeStub(varsletDato)))
        tilbakekreving.tilstand shouldBe TilBehandling
        return tilbakekreving
    }
}
