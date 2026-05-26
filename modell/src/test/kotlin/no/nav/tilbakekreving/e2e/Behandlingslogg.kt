package no.nav.tilbakekreving.e2e

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ANSVARLIG_SAKSBEHANDLER
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandlingslogg.EkstraInfo
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevDistribueringHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevJournalføringHendelse
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.skalUnnlates
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class Behandlingslogg {
    @Test
    fun `lagring av behandlingslogg i historikken`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val behandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")

        val tilbakekreving = Tilbakekreving.opprett(
            UUID.randomUUID().toString(),
            BehovObservatørOppsamler(),
            opprettTilbakekrevingHendelse,
            BigQueryServiceStub(),
            EndringObservatørOppsamler(),
            features = defaultFeatures(),
        )

        tilbakekreving.håndter(
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(3.januar(2021) til 3.januar(2021)),
                    kravgrunnlagPeriode(1.februar(2021) til 1.februar(2021)),
                ),
            ),
        )
        tilbakekreving.håndter(
            fagsysteminfoHendelse(
                utvidPerioder = listOf(
                    FagsysteminfoHendelse.UtvidetPeriode(
                        kravgrunnlagPeriode = 3.januar(2021) til 3.januar(2021),
                        vedtaksperiode = 1.januar(2021) til 31.januar(2021),
                    ),
                    FagsysteminfoHendelse.UtvidetPeriode(
                        kravgrunnlagPeriode = 1.februar(2021) til 1.februar(2021),
                        vedtaksperiode = 1.februar(2021) til 28.februar(2021),
                    ),
                ),
            ),
        )
        tilbakekreving.håndter(brukerinfoHendelse())
        tilbakekreving.sendVarselbrev(tilbakekreving.nåværendeBehandlingId(), "tekst fra saksbehandler")
        tilbakekreving.håndter(
            hendelse = VarselbrevJournalføringHendelse(
                varselbrevId = tilbakekreving.brevHistorikk.sisteVarselbrev()!!.id,
                journalpostId = "123",
                dokumentInfoId = "321",
            ),
        )

        tilbakekreving.håndter(
            hendelse = VarselbrevDistribueringHendelse(
                brevId = tilbakekreving.brevHistorikk.sisteVarselbrev()!!.id,
                journalpostId = "123",
                dokumentInfoId = "321",
            ),
        )
        tilbakekreving.lagreFristUtsettelse(tilbakekreving.nåværendeBehandlingId(), LocalDate.of(2027, 1, 1), "Begrunnelse", ANSVARLIG_SAKSBEHANDLER)
        tilbakekreving.lagreUttalelse(tilbakekreving.nåværendeBehandlingId(), UttalelseVurdering.NEI_ETTER_FORHÅNDSVARSEL, null, "ingen uttalelse", ANSVARLIG_SAKSBEHANDLER)

        tilbakekreving.håndter(tilbakekreving.nåværendeBehandlingId(), behandler, faktastegVurdering(1.januar(2021) til 31.januar(2021)))
        tilbakekreving.håndter(tilbakekreving.nåværendeBehandlingId(), behandler, faktastegVurdering(1.februar(2021) til 28.februar(2021)))
        tilbakekreving.håndter(tilbakekreving.nåværendeBehandlingId(), behandler, 1.januar(2021) til 31.januar(2021), Foreldelsesteg.Vurdering.IkkeForeldet(""))
        tilbakekreving.håndter(tilbakekreving.nåværendeBehandlingId(), behandler, 1.februar(2021) til 28.februar(2021), Foreldelsesteg.Vurdering.IkkeForeldet(""))
        tilbakekreving.håndter(
            tilbakekreving.nåværendeBehandlingId(),
            behandler,
            1.januar(2021) til 31.januar(2021),
            forårsaketAvBruker().uaktsomt(unnlates = skalUnnlates()),
        )

        tilbakekreving.hentBehandlingslogg().map { it.tittel } shouldContainAll listOf(
            "Kravgrunnlag mottatt",
            "Tilbakekreving opprettet",
            "Behandling opprettet",
            "Fagsysteminfo oppdatert",
            "Brukerinfo oppdatert",
            "Fakta vurdert",
            "Forhåndsvarsel sendt",
            "Varselbrev journalført",
            "Foreldelse vurdert",
            "Vilkår vurdert",
        )
        val forhåndsvarsel = tilbakekreving.hentBehandlingslogg().filter { it.tittel == "Forhåndsvarsel sendt" }.shouldHaveSize(1)

        val forhåndsvarselEkstraInfo = forhåndsvarsel[0].ekstraInfo as Map<*, *>
        forhåndsvarselEkstraInfo[EkstraInfo.JOURNALPOST_ID] shouldBe "123"
        forhåndsvarselEkstraInfo[EkstraInfo.DOKUMENTINFO_ID] shouldBe "321"

        val utsettUttalelse = tilbakekreving.hentBehandlingslogg().filter { it.tittel.contains("Ny frist for uttalelse") }.shouldHaveSize(1)
        utsettUttalelse[0].tittel shouldBe "Ny frist for uttalelse: 2027-01-01"
        utsettUttalelse[0].tekst shouldBe "Begrunnelse"
    }
}
