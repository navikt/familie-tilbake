package no.nav.tilbakekreving.hendelse

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.kravgrunnlag
import org.junit.jupiter.api.Test

class KravgrunnlagHendelseTest {
    @Test
    fun `kravgrunnlag hvor mottaker er ulik bruker er utenfor scope`() {
        val exception = shouldThrow<ModellFeil.UtenforScopeException> {
            kravgrunnlag(
                vedtakGjelder = Aktør.Person("04056912345"),
                utbetalesTil = Aktør.Person("20046912345"),
            )
        }

        exception.utenforScope shouldBe UtenforScope.KravgrunnlagBrukerIkkeLikMottaker
    }

    @Test
    fun `kan ikke håndtere kravgrunnlag som ikke gjelder person`() {
        val exception = shouldThrow<ModellFeil.UtenforScopeException> {
            kravgrunnlag(
                vedtakGjelder = Aktør.Organisasjon("889640782"),
            )
        }

        exception.utenforScope shouldBe UtenforScope.KravgrunnlagIkkePerson
    }
}
