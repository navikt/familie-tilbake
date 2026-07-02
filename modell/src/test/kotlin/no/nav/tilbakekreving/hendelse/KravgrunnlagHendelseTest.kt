package no.nav.tilbakekreving.hendelse

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feilutbetalteBeløp
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.ytelsesbeløp
import org.junit.jupiter.api.Test
import java.math.BigInteger

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

    @Test
    fun `to like kravgrunnlag`() {
        val kravgrunnlag1 = kravgrunnlag(
            vedtakId = BigInteger("123"),
            kontrollfelt = "abc",
            kravgrunnlagId = "def",
        )
        val kravgrunnlag2 = kravgrunnlag(
            vedtakId = BigInteger("123"),
            kontrollfelt = "abc",
            kravgrunnlagId = "def",
        )
        kravgrunnlag1 shouldBe kravgrunnlag2
    }

    @Test
    fun `ulikt beløp`() {
        val ytelsesbeløp1 = ytelsesbeløp(tilbakekrevesBeløp = 2000.kroner)
        val kravgrunnlag1 = kravgrunnlag(
            vedtakId = BigInteger("123"),
            referanse = "abc",
            kontrollfelt = "def",
            kravgrunnlagId = "ghi",
            perioder = listOf(
                kravgrunnlagPeriode(ytelsesbeløp = ytelsesbeløp1 + feilutbetalteBeløp(ytelsesbeløp1)),
            ),
        )
        val ytelsesbeløp2 = ytelsesbeløp(tilbakekrevesBeløp = 3000.kroner)
        val kravgrunnlag2 = kravgrunnlag(
            vedtakId = BigInteger("123"),
            referanse = "abc",
            kontrollfelt = "def",
            kravgrunnlagId = "ghi",
            perioder = listOf(
                kravgrunnlagPeriode(ytelsesbeløp = ytelsesbeløp2 + feilutbetalteBeløp(ytelsesbeløp2)),
            ),
        )
        kravgrunnlag1 shouldNotBe kravgrunnlag2
    }

    @Test
    fun `flere perioder i nytt kravgrunnlag`() {
        val kravgrunnlag1 = kravgrunnlag(
            vedtakId = BigInteger("123"),
            referanse = "abc",
            kontrollfelt = "def",
            kravgrunnlagId = "ghi",
            perioder = listOf(
                kravgrunnlagPeriode(periode = 1.januar(2021) til 31.januar(2021)),
            ),
        )
        val kravgrunnlag2 = kravgrunnlag(
            vedtakId = BigInteger("123"),
            referanse = "abc",
            kontrollfelt = "def",
            kravgrunnlagId = "ghi",
            perioder = listOf(
                kravgrunnlagPeriode(periode = 1.januar(2021) til 31.januar(2021)),
                kravgrunnlagPeriode(periode = 1.februar(2021) til 28.februar(2021)),
            ),
        )
        kravgrunnlag1 shouldNotBe kravgrunnlag2
    }
}
