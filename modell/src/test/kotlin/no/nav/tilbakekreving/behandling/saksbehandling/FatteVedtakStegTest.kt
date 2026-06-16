package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.test.FellesTestdata.ANSVARLIG_BESLUTTER
import no.nav.tilbakekreving.test.FellesTestdata.ANSVARLIG_SAKSBEHANDLER
import org.junit.jupiter.api.Test
import java.util.UUID

class FatteVedtakStegTest {
    @Test
    fun `delvis vurdering av fatte vedtak steg`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett()
        fatteVedtakSteg.håndter(
            ANSVARLIG_BESLUTTER,
            ANSVARLIG_SAKSBEHANDLER,
            Behandlingssteg.FORELDELSE,
            FatteVedtakSteg.Vurdering.Godkjent,
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        fatteVedtakSteg.behandlingsstatus shouldBe BehandlingsstatusModell.FATTER_VEDTAK
        fatteVedtakSteg.erFullstendig(SystemKlokke) shouldBe false
    }

    @Test
    fun `fullstendig vurdering av fatte vedtak steg`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett()
        fatteVedtakSteg.håndter(
            ANSVARLIG_BESLUTTER,
            ANSVARLIG_SAKSBEHANDLER,
            Behandlingssteg.FAKTA,
            FatteVedtakSteg.Vurdering.Godkjent,
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )
        fatteVedtakSteg.håndter(
            ANSVARLIG_BESLUTTER,
            ANSVARLIG_SAKSBEHANDLER,
            Behandlingssteg.FORHÅNDSVARSEL,
            FatteVedtakSteg.Vurdering.Godkjent,
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )
        fatteVedtakSteg.håndter(
            ANSVARLIG_BESLUTTER,
            ANSVARLIG_SAKSBEHANDLER,
            Behandlingssteg.FORELDELSE,
            FatteVedtakSteg.Vurdering.Godkjent,
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )
        fatteVedtakSteg.håndter(
            ANSVARLIG_BESLUTTER,
            ANSVARLIG_SAKSBEHANDLER,
            Behandlingssteg.VILKÅRSVURDERING,
            FatteVedtakSteg.Vurdering.Godkjent,
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )
        fatteVedtakSteg.håndter(
            ANSVARLIG_BESLUTTER,
            ANSVARLIG_SAKSBEHANDLER,
            Behandlingssteg.FORESLÅ_VEDTAK,
            FatteVedtakSteg.Vurdering.Godkjent,
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        fatteVedtakSteg.erFullstendig(SystemKlokke) shouldBe true
    }

    @Test
    fun `irrelevant vurdering av fatte vedtak steg`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett()

        shouldThrow<NoSuchElementException> {
            fatteVedtakSteg.håndter(
                ANSVARLIG_BESLUTTER,
                ANSVARLIG_SAKSBEHANDLER,
                Behandlingssteg.BREVMOTTAKER,
                FatteVedtakSteg.Vurdering.Godkjent,
                Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
            )
        }
    }

    @Test
    fun `ansvarlig saksbehandler skal ikke kunne fatte vedtak`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett()

        shouldThrow<ModellFeil.IngenTilgangException> {
            fatteVedtakSteg.håndter(
                ANSVARLIG_SAKSBEHANDLER,
                ANSVARLIG_SAKSBEHANDLER,
                Behandlingssteg.FAKTA,
                FatteVedtakSteg.Vurdering.Godkjent,
                Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
            )
        }
    }

    @Test
    fun `underkjenn alle steg i vedtaket`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett()

        fatteVedtakSteg.håndter(
            ANSVARLIG_BESLUTTER,
            ANSVARLIG_SAKSBEHANDLER,
            Behandlingssteg.FAKTA,
            FatteVedtakSteg.Vurdering.Underkjent("Ikke bra"),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )
        fatteVedtakSteg.håndter(
            ANSVARLIG_BESLUTTER,
            ANSVARLIG_SAKSBEHANDLER,
            Behandlingssteg.FORHÅNDSVARSEL,
            FatteVedtakSteg.Vurdering.Underkjent("Ikke bra"),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )
        fatteVedtakSteg.håndter(
            ANSVARLIG_BESLUTTER,
            ANSVARLIG_SAKSBEHANDLER,
            Behandlingssteg.FORELDELSE,
            FatteVedtakSteg.Vurdering.Underkjent("Ikke bra"),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )
        fatteVedtakSteg.håndter(
            ANSVARLIG_BESLUTTER,
            ANSVARLIG_SAKSBEHANDLER,
            Behandlingssteg.VILKÅRSVURDERING,
            FatteVedtakSteg.Vurdering.Underkjent("Ikke bra"),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )
        fatteVedtakSteg.håndter(
            ANSVARLIG_BESLUTTER,
            ANSVARLIG_SAKSBEHANDLER,
            Behandlingssteg.FORESLÅ_VEDTAK,
            FatteVedtakSteg.Vurdering.Underkjent("Ikke bra"),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        fatteVedtakSteg.erFullstendig(SystemKlokke) shouldBe true
        fatteVedtakSteg.erVedtakUnderkjent() shouldBe true
    }

    @Test
    fun `underkjenn noen av steg i vedtaket`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett()

        fatteVedtakSteg.håndter(
            ANSVARLIG_BESLUTTER,
            ANSVARLIG_SAKSBEHANDLER,
            Behandlingssteg.FAKTA,
            FatteVedtakSteg.Vurdering.Godkjent,
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )
        fatteVedtakSteg.håndter(
            ANSVARLIG_BESLUTTER,
            ANSVARLIG_SAKSBEHANDLER,
            Behandlingssteg.FORHÅNDSVARSEL,
            FatteVedtakSteg.Vurdering.Godkjent,
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )
        fatteVedtakSteg.håndter(
            ANSVARLIG_BESLUTTER,
            ANSVARLIG_SAKSBEHANDLER,
            Behandlingssteg.FORELDELSE,
            FatteVedtakSteg.Vurdering.Godkjent,
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )
        fatteVedtakSteg.håndter(
            ANSVARLIG_BESLUTTER,
            ANSVARLIG_SAKSBEHANDLER,
            Behandlingssteg.VILKÅRSVURDERING,
            FatteVedtakSteg.Vurdering.Underkjent("Ikke bra"),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )
        fatteVedtakSteg.håndter(
            ANSVARLIG_BESLUTTER,
            ANSVARLIG_SAKSBEHANDLER,
            Behandlingssteg.FORESLÅ_VEDTAK,
            FatteVedtakSteg.Vurdering.Underkjent("Ikke bra"),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        fatteVedtakSteg.erFullstendig(SystemKlokke) shouldBe true
        fatteVedtakSteg.erVedtakUnderkjent() shouldBe true
    }
}
