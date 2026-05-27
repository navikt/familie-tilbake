package no.nav.tilbakekreving.tilstand
import io.kotest.inspectors.forOne
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.systemContext
import org.junit.jupiter.api.Test
import java.util.UUID

class AvventerKravgrunnlagHendelseTest {
    @Test
    fun `tilbakekreving i AvventerKravgrunnlag går videre med Kravgrunnlag`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingEvent,
            sideeffektContext = systemContext(behovObservatør = oppsamler),
        )

        val kravgrunnlag = kravgrunnlag()
        tilbakekreving.håndter(kravgrunnlag, systemContext(behovObservatør = oppsamler))

        tilbakekreving.tilstand shouldBe AvventerFagsysteminfo
        oppsamler.behovListe.forOne {
            it shouldBeEqual FagsysteminfoBehov(
                eksternFagsakId = opprettTilbakekrevingEvent.eksternFagsak.eksternId,
                eksternBehandlingId = kravgrunnlag.referanse,
                ytelse = Ytelse.Barnetrygd,
                vedtakGjelderId = kravgrunnlag.vedtakGjelder.ident,
            )
        }
    }
}
