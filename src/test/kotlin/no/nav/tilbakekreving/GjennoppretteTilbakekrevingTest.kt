package no.nav.tilbakekreving

import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.tilstand.AvventerBrukerinfo
import no.nav.tilbakekreving.tilstand.AvventerFagsysteminfo
import no.nav.tilbakekreving.tilstand.AvventerKravgrunnlag
import no.nav.tilbakekreving.tilstand.SendVarselbrev
import no.nav.tilbakekreving.tilstand.TilBehandling
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class GjennoppretteTilbakekrevingTest : OppslagSpringRunnerTest() {
    @Autowired
    lateinit var tilbakekrevingRepository: TilbakekrevingRepository

    private fun <T> håndterHendlese(
        hendelse: T,
        tilbakekreving: Tilbakekreving,
    ) {
        when (hendelse) {
            is KravgrunnlagHendelse -> tilbakekreving.håndter(hendelse as KravgrunnlagHendelse)
            is FagsysteminfoHendelse -> tilbakekreving.håndter(hendelse as FagsysteminfoHendelse)
            is BrukerinfoHendelse -> tilbakekreving.håndter(hendelse as BrukerinfoHendelse)
            is VarselbrevSendtHendelse -> tilbakekreving.håndter(hendelse as VarselbrevSendtHendelse)
        }

        tilbakekrevingRepository.lagreTilstand(tilbakekreving.tilEntity())
    }

    @Test
    fun `gjennopprette tilbakekreving etter at hendelsene er håndtert`() {
        val opprettEvent = opprettTilbakekrevingHendelse(opprettelsesvalg = Opprettelsesvalg.OPPRETT_BEHANDLING_MED_VARSEL)
        val tilbakekreving = Tilbakekreving.opprett(BehovObservatørOppsamler(), opprettEvent)

        tilbakekrevingRepository.lagreTilstand(tilbakekreving.tilEntity())

        tilbakekreving.hentTilstand() shouldBe AvventerKravgrunnlag.tilbakekrevingTilstand
        var gjennopprettetTilbakekreving = tilbakekrevingRepository.hentTilbakekrevingMedTilbakekrevingId(tilbakekreving.id)?.fraEntity(Observatør())
        gjennopprettetTilbakekreving!!.hentTilstand() shouldBe tilbakekreving.hentTilstand()

        håndterHendlese(kravgrunnlag(), tilbakekreving)
        gjennopprettetTilbakekreving = tilbakekrevingRepository.hentTilbakekrevingMedTilbakekrevingId(tilbakekreving.id)?.fraEntity(Observatør())
        tilbakekreving.hentTilstand() shouldBe AvventerFagsysteminfo.tilbakekrevingTilstand
        gjennopprettetTilbakekreving!!.hentTilstand() shouldBe tilbakekreving.hentTilstand()

        håndterHendlese(fagsysteminfoHendelse(), tilbakekreving)
        gjennopprettetTilbakekreving = tilbakekrevingRepository.hentTilbakekrevingMedTilbakekrevingId(tilbakekreving.id)?.fraEntity(Observatør())
        tilbakekreving.hentTilstand() shouldBe AvventerBrukerinfo.tilbakekrevingTilstand
        gjennopprettetTilbakekreving!!.hentTilstand() shouldBe tilbakekreving.hentTilstand()

        håndterHendlese(brukerinfoHendelse(), tilbakekreving)
        gjennopprettetTilbakekreving = tilbakekrevingRepository.hentTilbakekrevingMedTilbakekrevingId(tilbakekreving.id)?.fraEntity(Observatør())
        tilbakekreving.hentTilstand() shouldBe SendVarselbrev.tilbakekrevingTilstand
        gjennopprettetTilbakekreving!!.hentTilstand() shouldBe tilbakekreving.hentTilstand()

        håndterHendlese(VarselbrevSendtHendelse(varselbrev()), tilbakekreving)
        gjennopprettetTilbakekreving = tilbakekrevingRepository.hentTilbakekrevingMedTilbakekrevingId(tilbakekreving.id)?.fraEntity(Observatør())
        tilbakekreving.hentTilstand() shouldBe TilBehandling.tilbakekrevingTilstand
        gjennopprettetTilbakekreving!!.hentTilstand() shouldBe tilbakekreving.hentTilstand()
    }
}
