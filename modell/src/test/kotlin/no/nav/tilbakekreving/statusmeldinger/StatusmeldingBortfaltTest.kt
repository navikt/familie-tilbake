package no.nav.tilbakekreving.statusmeldinger

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.bortfaltStatusmelding
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.e2e.kravgrunnlagMedBeløp
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.fatteVedtak
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.hendelse.VarselbrevJournalføringHendelse
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekreving
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.sperrStatusmelding
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.tilbakekrevingTilBehandling
import no.nav.tilbakekreving.tilstand.AvventerBrukerinfo
import no.nav.tilbakekreving.tilstand.AvventerFagsysteminfo
import no.nav.tilbakekreving.tilstand.AvventerKravgrunnlag
import no.nav.tilbakekreving.tilstand.DistribuerVarselbrev
import no.nav.tilbakekreving.tilstand.IverksettVedtak
import no.nav.tilbakekreving.tilstand.KravgrunnlagBortfalt
import no.nav.tilbakekreving.tilstand.SendVarselbrev
import no.nav.tilbakekreving.tilstand.Sperret
import no.nav.tilbakekreving.tilstand.TilBehandling
import no.nav.tilbakekreving.vurderAlleSteg
import org.junit.jupiter.api.Test

class StatusmeldingBortfaltTest {
    @Test
    fun `validering av scope feiler ikke for behandling i TilBehandling`() {
        val tilbakekreving = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse())

        tilbakekreving.tilstand shouldBe TilBehandling
        shouldNotThrowAny {
            tilbakekreving.validerInnenforScope(defaultFeatures())
        }
    }

    @Test
    fun `bortfalt kravgrunnlag når behandlingen er i TilBehandling`() {
        val tilbakekreving = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse())
        tilbakekreving.tilstand shouldBe TilBehandling

        tilbakekreving.håndter(bortfaltStatusmelding(), systemContext())

        tilbakekreving.tilstand shouldBe KravgrunnlagBortfalt
        shouldThrow<ModellFeil.UtenforScopeException> {
            tilbakekreving.validerInnenforScope(defaultFeatures())
        }.utenforScope shouldBe UtenforScope.KravgrunnlagBortfalt
    }

    @Test
    fun `bortfalt kravgrunnlag når behandlingen er i IverksettVedtak`() {
        val periode = 1.januar(2025) til 31.januar(2025)
        val saksbehandlerContext = saksbehandlerContext()
        val tilbakekreving = tilbakekrevingTilBehandling(kravgrunnlag = kravgrunnlagMedBeløp(periode to 2000.kroner))
        tilbakekreving.vurderAlleSteg(saksbehandlerContext, periode)
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            foreslåVedtak()
        }
        tilbakekreving.fatteVedtak()
        tilbakekreving.tilstand shouldBe IverksettVedtak

        tilbakekreving.håndter(bortfaltStatusmelding(), systemContext())

        tilbakekreving.tilstand shouldBe KravgrunnlagBortfalt
        shouldThrow<ModellFeil.UtenforScopeException> {
            tilbakekreving.validerInnenforScope(defaultFeatures())
        }.utenforScope shouldBe UtenforScope.KravgrunnlagBortfalt
    }

    @Test
    fun `bortfalt kravgrunnlag når behandlingen er sperret`() {
        val tilbakekreving = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse())
        tilbakekreving.håndter(sperrStatusmelding(), systemContext())
        tilbakekreving.tilstand shouldBe Sperret

        tilbakekreving.håndter(bortfaltStatusmelding(), systemContext())

        tilbakekreving.tilstand shouldBe KravgrunnlagBortfalt
        shouldThrow<ModellFeil.UtenforScopeException> {
            tilbakekreving.validerInnenforScope(defaultFeatures())
        }.utenforScope shouldBe UtenforScope.KravgrunnlagBortfalt
    }

    @Test
    fun `bortfalt kravgrunnlag når behandlingen er i AvventerKravgrunnlag`() {
        val context = saksbehandlerContext()
        val tilbakekreving = opprettTilbakekreving(opprettTilbakekrevingHendelse(), context)
        tilbakekreving.tilstand shouldBe AvventerKravgrunnlag

        tilbakekreving.håndter(bortfaltStatusmelding(), systemContext())

        tilbakekreving.tilstand shouldBe KravgrunnlagBortfalt
        shouldThrow<ModellFeil.UtenforScopeException> {
            tilbakekreving.validerInnenforScope(defaultFeatures())
        }.utenforScope shouldBe UtenforScope.KravgrunnlagBortfalt
    }

    @Test
    fun `bortfalt kravgrunnlag når behandlingen er i AvventerFagsysteminfo`() {
        val context = saksbehandlerContext()
        val tilbakekreving = opprettTilbakekreving(opprettTilbakekrevingHendelse(), context).apply {
            håndter(kravgrunnlag(), context)
        }
        tilbakekreving.tilstand shouldBe AvventerFagsysteminfo

        tilbakekreving.håndter(bortfaltStatusmelding(), systemContext())

        tilbakekreving.tilstand shouldBe KravgrunnlagBortfalt
        shouldThrow<ModellFeil.UtenforScopeException> {
            tilbakekreving.validerInnenforScope(defaultFeatures())
        }.utenforScope shouldBe UtenforScope.KravgrunnlagBortfalt
    }

    @Test
    fun `bortfalt kravgrunnlag når behandlingen er i AvventerBrukerinfo`() {
        val context = saksbehandlerContext()
        val tilbakekreving = opprettTilbakekreving(opprettTilbakekrevingHendelse(), context).apply {
            håndter(kravgrunnlag(), context)
            håndter(fagsysteminfoHendelse(), context)
        }
        tilbakekreving.tilstand shouldBe AvventerBrukerinfo

        tilbakekreving.håndter(bortfaltStatusmelding(), systemContext())

        tilbakekreving.tilstand shouldBe KravgrunnlagBortfalt
        shouldThrow<ModellFeil.UtenforScopeException> {
            tilbakekreving.validerInnenforScope(defaultFeatures())
        }.utenforScope shouldBe UtenforScope.KravgrunnlagBortfalt
    }

    @Test
    fun `bortfalt kravgrunnlag feiler når behandlingen er i SEND_VARSELBREV`() {
        val tilbakekreving = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse())
        tilbakekreving.tilstand shouldBe TilBehandling

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            vurderFakta(faktastegVurdering(perioder = listOf(1.januar(2021) til 31.januar(2021))))
        }
        tilbakekreving.sendVarselbrev(tilbakekreving.nåværendeBehandlingId(), "Tekst fra saksbehandler", saksbehandlerContext())
        tilbakekreving.tilstand shouldBe SendVarselbrev

        shouldThrow<ModellFeil.UgyldigOperasjonException> {
            tilbakekreving.håndter(bortfaltStatusmelding(), systemContext())
        }.message shouldBe "Forventet ikke AvsluttetStatusmeldingHendelse i SEND_VARSELBREV"
    }

    @Test
    fun `bortfalt kravgrunnlag feiler når behandlingen er i DISTRIUBER_VARSELBREV`() {
        val tilbakekreving = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse())
        tilbakekreving.tilstand shouldBe TilBehandling

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            vurderFakta(faktastegVurdering(perioder = listOf(1.januar(2021) til 31.januar(2021))))
        }
        tilbakekreving.sendVarselbrev(tilbakekreving.nåværendeBehandlingId(), "Tekst fra saksbehandler", saksbehandlerContext())
        tilbakekreving.håndter(VarselbrevJournalføringHendelse(tilbakekreving.brevHistorikk.sisteVarselbrev()!!.id, "123456789", "1234"), systemContext())

        tilbakekreving.tilstand shouldBe DistribuerVarselbrev

        shouldThrow<ModellFeil.UgyldigOperasjonException> {
            tilbakekreving.håndter(bortfaltStatusmelding(), systemContext())
        }.message shouldBe "Forventet ikke AvsluttetStatusmeldingHendelse i DISTRIUBER_VARSELBREV"
    }
}
