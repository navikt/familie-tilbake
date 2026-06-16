
package no.nav.tilbakekreving.entities

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.Enhet
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.IverksettelseBehov
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.lesContext
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.FellesTestdata.ANSVARLIG_SAKSBEHANDLER
import no.nav.tilbakekreving.tilstand.TilBehandling
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class BehandlingEntityTest {
    @Test
    fun `bruker riktig innslag i historikken når man gjenopptar behandling`() {
        val behandlingId = UUID.randomUUID()
        val fagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(mutableListOf())
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf())
        val brevHistorikk = BrevHistorikk(mutableListOf())

        val revurderingInnslag = fagsakBehandlingHistorikk.lagre(
            EksternFagsakRevurdering.Revurdering(
                id = UUID.randomUUID(),
                eksternId = UUID.randomUUID().toString(),
                revurderingsårsak = EksternFagsakRevurdering.Revurderingsårsak.NYE_OPPLYSNINGER,
                årsakTilFeilutbetaling = "abc",
                vedtaksdato = LocalDate.now(),
                utvidedePerioder = emptyList(),
                url = "http://localhost:8080",
            ),
        )
        // Lagre et nytt innslag så vi er sikker på at det riktige plukkes opp, ikke det nyeste
        fagsakBehandlingHistorikk.lagre(EksternFagsakRevurdering.Ukjent(UUID.randomUUID(), UUID.randomUUID().toString(), null))

        val kravgrunnlag = kravgrunnlagHistorikk.lagre(kravgrunnlag())
        kravgrunnlagHistorikk.lagre(kravgrunnlag())
        val behandlingFørLagring = Behandling.nyBehandling(
            id = behandlingId,
            type = Behandlingstype.TILBAKEKREVING,
            enhet = Enhet("0425", "NAV Solør"),
            ansvarligSaksbehandler = ANSVARLIG_SAKSBEHANDLER,
            eksternFagsakRevurdering = revurderingInnslag,
            kravgrunnlag = kravgrunnlag,
            brevHistorikk = brevHistorikk,
            klokke = SystemKlokke,
        )

        val behandlingEtterLagring = behandlingFørLagring
            .tilEntity("not_needed")
            .fraEntity(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)
        behandlingEtterLagring.tilFrontendDto(TilBehandling, systemContext(), true, BehandlerRolle.BESLUTTER) shouldBe behandlingFørLagring.tilFrontendDto(TilBehandling, lesContext(), true, BehandlerRolle.BESLUTTER)

        val observatør = BehovObservatørOppsamler()
        behandlingEtterLagring.trengerIverksettelse(systemContext(behovObservatør = observatør), Ytelse.Tilleggsstønad, Aktør.Person("20046912345"))

        val behov = observatør.behovListe.single().shouldBeInstanceOf<IverksettelseBehov>()
        behov.kravgrunnlagId shouldBe kravgrunnlag.entry.kravgrunnlagId
    }
}
