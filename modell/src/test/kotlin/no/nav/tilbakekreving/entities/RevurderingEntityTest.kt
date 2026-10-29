package no.nav.tilbakekreving.entities

import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.Enhet
import no.nav.tilbakekreving.behandling.TilbakekrevingRevurderingsårsak
import no.nav.tilbakekreving.behandling.TilbakekrevingRevurderingsårsakType
import no.nav.tilbakekreving.behandling.VedtakOmgjortAv
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.frontend.models.EtterKlageDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RevurderingsinstansDto
import no.nav.tilbakekreving.kontrakter.frontend.models.TilbakekrevingRevurderingDto
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.lesContext
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.FellesTestdata.ANSVARLIG_SAKSBEHANDLER
import no.nav.tilbakekreving.tilstand.TilBehandling
import org.junit.jupiter.api.Test

class RevurderingEntityTest {
    @Test
    fun `Revurdering av tilbakekrevingen blir lagret`(){
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
                opprettet = LocalDateTime.now(),
            ),
        ) as HistorikkReferanse<UUID, EksternFagsakRevurdering>
        // Lagre et nytt innslag så vi er sikker på at det riktige plukkes opp, ikke det nyeste
        fagsakBehandlingHistorikk.lagre(EksternFagsakRevurdering.Ukjent(UUID.randomUUID(), UUID.randomUUID().toString(), null, opprettet = LocalDateTime.now()))

        val kravgrunnlag = kravgrunnlagHistorikk.lagre(kravgrunnlag())
        kravgrunnlagHistorikk.lagre(kravgrunnlag())
        val opprinneligBehandling = Behandling.nyBehandling(
            id = behandlingId,
            type = Behandlingstype.TILBAKEKREVING,
            enhet = Enhet("0425", "NAV Solør"),
            ansvarligSaksbehandler = ANSVARLIG_SAKSBEHANDLER,
            eksternFagsakRevurdering = revurderingInnslag,
            kravgrunnlag = kravgrunnlag,
            brevHistorikk = brevHistorikk,
            klokke = SystemKlokke,
        )

        opprinneligBehandling.tilFrontendDto(TilBehandling, systemContext(), true, BehandlerRolle.BESLUTTER).tilbakekrevingRevurdering shouldBe null

        val revurderingFørLagring = opprinneligBehandling.klonBehandlingForRevurdering(
            revurderingsårsak = TilbakekrevingRevurderingsårsak.EtterKlage(
                årsakType = TilbakekrevingRevurderingsårsakType.ETTER_KLAGE,
                vedtakOmgjortAv = VedtakOmgjortAv.Klageinstansen,
                beskrivelse = "Dette er en test",
            ),
            klokke = SystemKlokke
        )

        val behandlingEtterLagring = revurderingFørLagring
            .tilEntity("not_needed")
            .fraEntity(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)
        behandlingEtterLagring.tilFrontendDto(TilBehandling, systemContext(), true, BehandlerRolle.BESLUTTER).tilbakekrevingRevurdering shouldBe TilbakekrevingRevurderingDto(
            EtterKlageDto(
                instans = RevurderingsinstansDto.Klageinstansen,
                begrunnelse = "Dette er en test"
            )
        )
    }
}
