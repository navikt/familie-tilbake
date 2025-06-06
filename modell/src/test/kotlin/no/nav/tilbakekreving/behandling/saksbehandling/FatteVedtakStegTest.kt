package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.junit.jupiter.api.Test

class FatteVedtakStegTest {
    private val ansvarligSaksbehandler = Behandler.Saksbehandler("AnsvarligSaksbehandler")
    private val ansvarligBeslutter = Behandler.Saksbehandler("AnsvarligBeslutter")

    @Test
    fun `delvis vurdering av fatte vedtak steg`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett()
        fatteVedtakSteg.håndter(ansvarligBeslutter, ansvarligSaksbehandler, Behandlingssteg.FORELDELSE, FatteVedtakSteg.Vurdering.Godkjent)

        fatteVedtakSteg.erFullstending() shouldBe false
    }

    @Test
    fun `fullstendig vurdering av fatte vedtak steg`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett()
        fatteVedtakSteg.håndter(ansvarligBeslutter, ansvarligSaksbehandler, Behandlingssteg.FAKTA, FatteVedtakSteg.Vurdering.Godkjent)
        fatteVedtakSteg.håndter(ansvarligBeslutter, ansvarligSaksbehandler, Behandlingssteg.FORELDELSE, FatteVedtakSteg.Vurdering.Godkjent)
        fatteVedtakSteg.håndter(ansvarligBeslutter, ansvarligSaksbehandler, Behandlingssteg.VILKÅRSVURDERING, FatteVedtakSteg.Vurdering.Godkjent)
        fatteVedtakSteg.håndter(ansvarligBeslutter, ansvarligSaksbehandler, Behandlingssteg.FORESLÅ_VEDTAK, FatteVedtakSteg.Vurdering.Godkjent)

        fatteVedtakSteg.erFullstending() shouldBe true
    }

    @Test
    fun `irrelevant vurdering av fatte vedtak steg`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett()

        shouldThrow<NoSuchElementException> {
            fatteVedtakSteg.håndter(ansvarligBeslutter, ansvarligSaksbehandler, Behandlingssteg.BREVMOTTAKER, FatteVedtakSteg.Vurdering.Godkjent)
        }
    }

    @Test
    fun `ansvarlig saksbehandler skal ikke kunne fatte vedtak`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett()

        shouldThrow<IllegalStateException> {
            fatteVedtakSteg.håndter(ansvarligSaksbehandler, ansvarligSaksbehandler, Behandlingssteg.FAKTA, FatteVedtakSteg.Vurdering.Godkjent)
        }
    }
}
