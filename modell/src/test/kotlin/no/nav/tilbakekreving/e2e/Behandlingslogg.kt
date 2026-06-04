package no.nav.tilbakekreving.e2e
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.behandlingslogg.EkstraInfo
import no.nav.tilbakekreving.brukerinfoHendelse
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
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.skalUnnlates
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class Behandlingslogg {
    @Test
    fun `lagring av behandlingslogg i historikken`() {
        val behandlingslogg = Behandlingslogg(mutableListOf())
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse,
            sideeffektContext = systemContext(behandlingslogg = behandlingslogg),
        )

        tilbakekreving.håndter(
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(3.januar(2021) til 3.januar(2021)),
                    kravgrunnlagPeriode(1.februar(2021) til 1.februar(2021)),
                ),
            ),
            systemContext(behandlingslogg = behandlingslogg),
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
            systemContext(behandlingslogg = behandlingslogg),
        )
        tilbakekreving.håndter(brukerinfoHendelse(), systemContext(behandlingslogg = behandlingslogg))
        tilbakekreving.sendVarselbrev(tilbakekreving.nåværendeBehandlingId(), "tekst fra saksbehandler", saksbehandlerContext(behandlingslogg = behandlingslogg))
        tilbakekreving.håndter(
            hendelse = VarselbrevJournalføringHendelse(
                varselbrevId = tilbakekreving.brevHistorikk.sisteVarselbrev()!!.id,
                journalpostId = "123",
                dokumentInfoId = "321",
            ),
            sideeffektContext = systemContext(behandlingslogg = behandlingslogg),
        )

        tilbakekreving.håndter(
            hendelse = VarselbrevDistribueringHendelse(
                brevId = tilbakekreving.brevHistorikk.sisteVarselbrev()!!.id,
                journalpostId = "123",
                dokumentInfoId = "321",
            ),
            sideeffektContext = systemContext(behandlingslogg = behandlingslogg),
        )
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(behandlingslogg = behandlingslogg)) {
            lagreFristUtsettelse(LocalDate.of(2027, 1, 1), "Begrunnelse")
            lagreUttalelse(UttalelseVurdering.NEI_ETTER_FORHÅNDSVARSEL, null, "ingen uttalelse")
            vurderFakta(faktastegVurdering(1.januar(2021) til 31.januar(2021)))
            vurderFakta(faktastegVurdering(1.februar(2021) til 28.februar(2021)))
            vurderForeldelse(1.januar(2021) til 31.januar(2021), Foreldelsesteg.Vurdering.IkkeForeldet(""))
            vurderForeldelse(1.februar(2021) til 28.februar(2021), Foreldelsesteg.Vurdering.IkkeForeldet(""))
            vurderVilkår(1.januar(2021) til 31.januar(2021), forårsaketAvBruker().uaktsomt(unnlates = skalUnnlates()))
        }

        behandlingslogg.tilFrontend().map { it.tittel } shouldContainAll listOf(
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
        val forhåndsvarsel = behandlingslogg.tilFrontend().filter { it.tittel == "Forhåndsvarsel sendt" }.shouldHaveSize(1)

        val forhåndsvarselEkstraInfo = forhåndsvarsel[0].ekstraInfo as Map<*, *>
        forhåndsvarselEkstraInfo[EkstraInfo.JOURNALPOST_ID] shouldBe "123"
        forhåndsvarselEkstraInfo[EkstraInfo.DOKUMENTINFO_ID] shouldBe "321"

        val utsettUttalelse = behandlingslogg.tilFrontend().filter { it.tittel.contains("Ny frist for uttalelse") }.shouldHaveSize(1)
        utsettUttalelse[0].tittel shouldBe "Ny frist for uttalelse: 2027-01-01"
        utsettUttalelse[0].tekst shouldBe "Begrunnelse"
    }
}
