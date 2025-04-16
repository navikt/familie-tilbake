package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.saksbehandler.Saksbehandling
import org.junit.jupiter.api.Test

class FatteVedtakStegTest {
    private val ansvarligSaksbehandler = Behandler.Saksbehandler("AnsvarligSaksbehandler")
    private val ansvarligBeslutter = Behandler.Saksbehandler("AnsvarligBeslutter")
    private val saksbehandling = object : Saksbehandling {
        override fun ansvarligSaksbehandler(): Behandler {
            return ansvarligSaksbehandler
        }

        override fun oppdaterAnsvarligSaksbehandler(behandler: Behandler) {
            error("Oppdatering av saksbehandler er ikke gyldig i test")
        }
    }

    @Test
    fun `delvis vurdering av fatte vedtak steg`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett(saksbehandling)
        fatteVedtakSteg.håndter(ansvarligBeslutter, Behandlingssteg.FORELDELSE, FatteVedtakSteg.Vurdering.Godkjent)

        fatteVedtakSteg.erFullstending() shouldBe false
    }

    @Test
    fun `fullstendig vurdering av fatte vedtak steg`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett(saksbehandling)
        fatteVedtakSteg.håndter(ansvarligBeslutter, Behandlingssteg.FAKTA, FatteVedtakSteg.Vurdering.Godkjent)
        fatteVedtakSteg.håndter(ansvarligBeslutter, Behandlingssteg.FORELDELSE, FatteVedtakSteg.Vurdering.Godkjent)
        fatteVedtakSteg.håndter(ansvarligBeslutter, Behandlingssteg.VILKÅRSVURDERING, FatteVedtakSteg.Vurdering.Godkjent)
        fatteVedtakSteg.håndter(ansvarligBeslutter, Behandlingssteg.FORESLÅ_VEDTAK, FatteVedtakSteg.Vurdering.Godkjent)

        fatteVedtakSteg.erFullstending() shouldBe true
    }

    @Test
    fun `irrelevant vurdering av fatte vedtak steg`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett(saksbehandling)

        shouldThrow<NoSuchElementException> {
            fatteVedtakSteg.håndter(ansvarligBeslutter, Behandlingssteg.BREVMOTTAKER, FatteVedtakSteg.Vurdering.Godkjent)
        }
    }

    @Test
    fun `ansvarlig saksbehandler skal ikke kunne fatte vedtak`() {
        val fatteVedtakSteg = FatteVedtakSteg.opprett(saksbehandling)

        shouldThrow<IllegalStateException> {
            fatteVedtakSteg.håndter(ansvarligSaksbehandler, Behandlingssteg.FAKTA, FatteVedtakSteg.Vurdering.Godkjent)
        }
    }
}
