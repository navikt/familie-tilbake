package no.nav.tilbakekreving.entities

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.BehandlingObservatørOppsamler
import no.nav.tilbakekreving.behandling.Enhet
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.IverksettelseBehov
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.fagsystem.Ytelsestype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class BehandlingEntityTest {
    @Test
    fun `bruker riktig innslag i historikken når man gjenopptar behandling`() {
        val behandlingId = UUID.randomUUID()
        val fagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(mutableListOf())
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf())
        val brevHistorikk = BrevHistorikk(mutableListOf())

        val behandlingInnslag = fagsakBehandlingHistorikk.lagre(
            EksternFagsakBehandling.Behandling(
                internId = UUID.randomUUID(),
                eksternId = UUID.randomUUID().toString(),
                revurderingsresultat = "abc",
                revurderingsårsak = "abc",
                begrunnelseForTilbakekreving = "abc",
                revurderingsvedtaksdato = LocalDate.now(),
            ),
        )
        // Lagre et nytt innslag så vi er sikker på at det riktige plukkes opp, ikke det nyeste
        fagsakBehandlingHistorikk.lagre(EksternFagsakBehandling.Ukjent(UUID.randomUUID(), null))

        val kravgrunnlag = kravgrunnlagHistorikk.lagre(kravgrunnlag())
        kravgrunnlagHistorikk.lagre(kravgrunnlag())
        val behandler = Behandler.Saksbehandler("A123456")
        val behandlingFørLagring = Behandling.nyBehandling(
            internId = behandlingId,
            eksternId = behandlingId,
            behandlingstype = Behandlingstype.TILBAKEKREVING,
            opprettet = LocalDateTime.now(),
            sistEndret = LocalDateTime.now(),
            enhet = Enhet("0425", "NAV Solør"),
            årsak = Behandlingsårsakstype.REVURDERING_KLAGE_KA,
            ansvarligSaksbehandler = behandler,
            eksternFagsakBehandling = behandlingInnslag,
            kravgrunnlag = kravgrunnlag,
            brevHistorikk = brevHistorikk,
            behandlingObservatør = BehandlingObservatørOppsamler(),
        )

        val behandlingEtterLagring = behandlingFørLagring.tilEntity().fraEntity(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)
        behandlingEtterLagring.tilFrontendDto(behandler, kanBeslutte = true, erNyModell = false) shouldBe behandlingFørLagring.tilFrontendDto(behandler, kanBeslutte = true, erNyModell = false)

        val observatør = BehovObservatørOppsamler()
        behandlingEtterLagring.trengerIverksettelse(observatør, Ytelsestype.TILLEGGSSTØNAD, Aktør.Person("20046912345"))

        val behov = observatør.behovListe.single().shouldBeInstanceOf<IverksettelseBehov>()
        behov.kravgrunnlagId shouldBe kravgrunnlag.entry.kravgrunnlagId
    }
}
