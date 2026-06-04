package no.nav.tilbakekreving.behandling

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.behandlerContext
import no.nav.tilbakekreving.behandling
import no.nav.tilbakekreving.beslutterContext
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.fatteVedtakVurdering
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.test.forsettelig
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.tilstand.Avsluttet
import no.nav.tilbakekreving.tilstand.TilBehandling
import org.junit.jupiter.api.Test

class BehandlingRolleTest {
    val periode = 1.januar(2021) til 31.januar(2021)

    @Test
    fun `veileder-rolle vises for bruker med veileder-tilgang`() {
        val behandling = behandling()
        val veileder = Behandler.Saksbehandler("veileder")

        val dto = behandling.tilFrontendDto(tilstand = TilBehandling, lesContext = behandlerContext(veileder), kanBeslutte = false, behandlerRolle = BehandlerRolle.VEILEDER)

        dto.innloggetRolle shouldBe BehandlerRolle.VEILEDER
    }

    @Test
    fun `saksbehandler-rolle vises for bruker med beslutter-tilgang utenfor fatte-vedtak-steget`() {
        val behandling = behandling()

        val dto = behandling.tilFrontendDto(tilstand = TilBehandling, beslutterContext(), kanBeslutte = true, behandlerRolle = BehandlerRolle.BESLUTTER)

        dto.innloggetRolle shouldBe BehandlerRolle.SAKSBEHANDLER
    }

    @Test
    fun `beslutter-rolle vises for bruker med beslutter-tilgang på fatte-vedtak-steget`() {
        val behandling = behandlingKlarTilBeslutning()
        val annenBeslutter = Behandler.Saksbehandler("annen-beslutter")

        val dto = behandling.tilFrontendDto(tilstand = TilBehandling, behandlerContext(annenBeslutter), kanBeslutte = true, behandlerRolle = BehandlerRolle.BESLUTTER)

        dto.innloggetRolle shouldBe BehandlerRolle.BESLUTTER
    }

    @Test
    fun `saksbehandler-rolle vises for beslutter som er ansvarlig saksbehandler på fatte-vedtak-steget`() {
        val behandling = behandlingKlarTilBeslutning()

        val dto = behandling.tilFrontendDto(tilstand = TilBehandling, saksbehandlerContext(), kanBeslutte = true, behandlerRolle = BehandlerRolle.BESLUTTER)

        dto.innloggetRolle shouldBe BehandlerRolle.SAKSBEHANDLER
    }

    @Test
    fun `beslutter-rolle vises for ansvarlig beslutter på avsluttet behandling`() {
        val behandling = behandlingKlarTilBeslutning()
        behandling.medSaksbehandling(beslutterContext()) { fatteVedtak(fatteVedtakVurdering()) }

        val dto = behandling.tilFrontendDto(tilstand = Avsluttet, beslutterContext(), kanBeslutte = true, behandlerRolle = BehandlerRolle.BESLUTTER)

        dto.innloggetRolle shouldBe BehandlerRolle.BESLUTTER
    }

    private fun behandlingKlarTilBeslutning(): Behandling {
        val behandling = behandling()
        behandling.medSaksbehandling(saksbehandlerContext()) {
            vurderFakta(faktastegVurdering())
            lagreForhåndsvarselUnntak(
                BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG,
                beskrivelse = "Trenger ikke forhåndsvarsel i test",
            )
            vurderForeldelse(periode, foreldelseVurdering())
            vurderVilkår(periode, forårsaketAvNav().burdeForstått(aktsomhet = forsettelig()))
            foreslåVedtak()
        }
        return behandling
    }
}
