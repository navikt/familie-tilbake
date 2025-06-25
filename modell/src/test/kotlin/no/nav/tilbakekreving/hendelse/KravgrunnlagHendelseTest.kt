package no.nav.tilbakekreving.hendelse

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.feil.UtenforScopeException
import no.nav.tilbakekreving.kravgrunnlag
import org.junit.jupiter.api.Test

class KravgrunnlagHendelseTest {
    @Test
    fun `kravgrunnlag hvor mottaker er ulik bruker er utenfor scope`() {
        val exception = shouldThrow<UtenforScopeException> {
            kravgrunnlag(
                vedtakGjelder = KravgrunnlagHendelse.Aktør.Person("04056912345"),
                utbetalesTil = KravgrunnlagHendelse.Aktør.Person("20046912345"),
            )
        }

        exception.utenforScope shouldBe UtenforScope.KravgrunnlagBrukerIkkeLikMottaker
    }

    @Test
    fun `kan ikke håndtere kravgrunnlag som ikke gjelder person`() {
        val exception = shouldThrow<UtenforScopeException> {
            kravgrunnlag(
                vedtakGjelder = KravgrunnlagHendelse.Aktør.Organisasjon("889640782"),
            )
        }

        exception.utenforScope shouldBe UtenforScope.KravgrunnlagIkkePerson
    }
}
