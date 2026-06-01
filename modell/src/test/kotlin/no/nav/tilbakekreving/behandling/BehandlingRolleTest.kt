package no.nav.tilbakekreving.behandling

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ANSVARLIG_BESLUTTER
import no.nav.tilbakekreving.ANSVARLIG_SAKSBEHANDLER
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.behandling
import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.behandlingslogg.LoggInnslag
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.fatteVedtakVurdering
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.test.forsettelig
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.tilstand.Avsluttet
import no.nav.tilbakekreving.tilstand.TilBehandling
import org.junit.jupiter.api.Test

class BehandlingRolleTest {
    val behandlingslogg = Behandlingslogg(mutableListOf<LoggInnslag>())
    val periode = 1.januar(2021) til 31.januar(2021)

    @Test
    fun `veileder-rolle vises for bruker med veileder-tilgang`() {
        val behandling = behandling()
        val veileder = Behandler.Saksbehandler("veileder")

        val dto = behandling.tilFrontendDto(
            tilstand = TilBehandling,
            behandler = veileder,
            kanBeslutte = false,
            behandlerRolle = BehandlerRolle.VEILEDER,
        )

        dto.innloggetSaksbehandlerRolle shouldBe BehandlerRolle.VEILEDER
    }

    @Test
    fun `saksbehandler-rolle vises for bruker med beslutter-tilgang utenfor fatte-vedtak-steget`() {
        val behandling = behandling()

        val dto = behandling.tilFrontendDto(tilstand = TilBehandling, behandler = ANSVARLIG_BESLUTTER, kanBeslutte = true, behandlerRolle = BehandlerRolle.BESLUTTER)

        dto.innloggetSaksbehandlerRolle shouldBe BehandlerRolle.SAKSBEHANDLER
    }

    @Test
    fun `beslutter-rolle vises for bruker med beslutter-tilgang på fatte-vedtak-steget`() {
        val behandling = behandlingKlarTilBeslutning()
        val annenBeslutter = Behandler.Saksbehandler("annen-beslutter")

        val dto = behandling.tilFrontendDto(tilstand = TilBehandling, behandler = annenBeslutter, kanBeslutte = true, behandlerRolle = BehandlerRolle.BESLUTTER)

        dto.innloggetSaksbehandlerRolle shouldBe BehandlerRolle.BESLUTTER
    }

    @Test
    fun `saksbehandler-rolle vises for beslutter som er ansvarlig saksbehandler på fatte-vedtak-steget`() {
        val behandling = behandlingKlarTilBeslutning()

        val dto = behandling.tilFrontendDto(tilstand = TilBehandling, behandler = ANSVARLIG_SAKSBEHANDLER, kanBeslutte = true, behandlerRolle = BehandlerRolle.BESLUTTER)

        dto.innloggetSaksbehandlerRolle shouldBe BehandlerRolle.SAKSBEHANDLER
    }

    @Test
    fun `beslutter-rolle vises for ansvarlig beslutter på avsluttet behandling`() {
        val behandling = behandlingKlarTilBeslutning()
        behandling.håndter(ANSVARLIG_BESLUTTER, fatteVedtakVurdering(), behandlingslogg)

        val dto = behandling.tilFrontendDto(tilstand = Avsluttet, behandler = ANSVARLIG_BESLUTTER, kanBeslutte = true, behandlerRolle = BehandlerRolle.BESLUTTER)

        dto.innloggetSaksbehandlerRolle shouldBe BehandlerRolle.BESLUTTER
    }

    private fun behandlingKlarTilBeslutning(): Behandling {
        val behandling = behandling()
        behandling.håndter(ANSVARLIG_SAKSBEHANDLER, faktastegVurdering(), behandlingslogg)
        behandling.lagreForhåndsvarselUnntak(
            BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG,
            beskrivelse = "Trenger ikke forhåndsvarsel i test",
            behandler = ANSVARLIG_SAKSBEHANDLER,
            behandlingslogg = behandlingslogg,
        )
        behandling.håndter(ANSVARLIG_SAKSBEHANDLER, periode, foreldelseVurdering(), behandlingslogg)
        behandling.håndter(ANSVARLIG_SAKSBEHANDLER, periode, forårsaketAvNav().burdeForstått(aktsomhet = forsettelig()), behandlingslogg)
        behandling.håndterForeslåVedtak(ANSVARLIG_SAKSBEHANDLER, behandlingslogg)
        return behandling
    }
}
