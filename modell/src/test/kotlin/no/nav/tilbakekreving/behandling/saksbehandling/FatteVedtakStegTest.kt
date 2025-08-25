package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.junit.jupiter.api.Test
import java.util.UUID

class FatteVedtakStegTest {
    private val ansvarligSaksbehandler = Behandler.Saksbehandler("AnsvarligSaksbehandler")
    private val ansvarligBeslutter = Behandler.Saksbehandler("AnsvarligBeslutter")

    @Test
    fun `delvis vurdering av fatte vedtak steg`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett()
        fatteVedtakSteg.håndter(
            ansvarligBeslutter,
            ansvarligSaksbehandler,
            Behandlingssteg.FORELDELSE,
            FatteVedtakSteg.Vurdering.Godkjent,
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        fatteVedtakSteg.behandlingsstatus shouldBe Behandlingsstatus.FATTER_VEDTAK
        fatteVedtakSteg.erFullstendig() shouldBe false
    }

    @Test
    fun `fullstendig vurdering av fatte vedtak steg`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett()
        fatteVedtakSteg.håndter(
            ansvarligBeslutter,
            ansvarligSaksbehandler,
            Behandlingssteg.FAKTA,
            FatteVedtakSteg.Vurdering.Godkjent,
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )
        fatteVedtakSteg.håndter(
            ansvarligBeslutter,
            ansvarligSaksbehandler,
            Behandlingssteg.FORELDELSE,
            FatteVedtakSteg.Vurdering.Godkjent,
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )
        fatteVedtakSteg.håndter(
            ansvarligBeslutter,
            ansvarligSaksbehandler,
            Behandlingssteg.VILKÅRSVURDERING,
            FatteVedtakSteg.Vurdering.Godkjent,
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )
        fatteVedtakSteg.håndter(
            ansvarligBeslutter,
            ansvarligSaksbehandler,
            Behandlingssteg.FORESLÅ_VEDTAK,
            FatteVedtakSteg.Vurdering.Godkjent,
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        fatteVedtakSteg.behandlingsstatus shouldBe Behandlingsstatus.IVERKSETTER_VEDTAK
        fatteVedtakSteg.erFullstendig() shouldBe true
    }

    @Test
    fun `irrelevant vurdering av fatte vedtak steg`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett()

        shouldThrow<NoSuchElementException> {
            fatteVedtakSteg.håndter(
                ansvarligBeslutter,
                ansvarligSaksbehandler,
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
                ansvarligSaksbehandler,
                ansvarligSaksbehandler,
                Behandlingssteg.FAKTA,
                FatteVedtakSteg.Vurdering.Godkjent,
                Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
            )
        }
    }
}
