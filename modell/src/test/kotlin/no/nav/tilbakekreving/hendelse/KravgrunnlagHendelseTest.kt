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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.Boolean

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
        kravgrunnlag1.kanBrukesUtenNyVurdering(kravgrunnlag2) shouldBe true
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
        kravgrunnlag1.kanBrukesUtenNyVurdering(kravgrunnlag2) shouldBe false
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
        kravgrunnlag1.kanBrukesUtenNyVurdering(kravgrunnlag2) shouldBe false
    }

    @Test
    fun `identiske kravgrunnlag`() {
        val a = standardKravgrunnlag()
        val b = standardKravgrunnlag()

        a.harNokOverlapp(b) shouldBe true
        a.kanBrukesUtenNyVurdering(b) shouldBe true
        a shouldBe b
        a.hashCode() shouldBe b.hashCode()
    }

    @Test
    fun `kravgrunnlag med ulik id, opprettet og korrigering`() {
        val a = standardKravgrunnlag()
        val b = standardKravgrunnlag(
            id = UUID.randomUUID(),
            opprettet = LocalDateTime.of(2022, 5, 5, 8, 0),
            korrigering = true,
        )

        a.harNokOverlapp(b) shouldBe true
        a.kanBrukesUtenNyVurdering(b) shouldBe true
        a shouldBe b
        a.hashCode() shouldBe b.hashCode()
    }

    @Test
    fun `kravgrunnlag med ulik vedtakId`() {
        val a = standardKravgrunnlag()
        val b = standardKravgrunnlag(vedtakId = BigInteger("124"))

        a.harNokOverlapp(b) shouldBe true
        a.kanBrukesUtenNyVurdering(b) shouldBe true
        a shouldNotBe b
        a.hashCode() shouldNotBe b.hashCode()
    }

    @Test
    fun `kravgrunnlag med ulik kravstatuskode`() {
        val a = standardKravgrunnlag(kravstatuskode = KravgrunnlagHendelse.Kravstatuskode.NY)
        val b = standardKravgrunnlag()

        a.harNokOverlapp(b) shouldBe true
        a.kanBrukesUtenNyVurdering(b) shouldBe true
        a shouldNotBe b
        a.hashCode() shouldNotBe b.hashCode()
    }

    @Test
    fun `kravgrunnlag med ulik fagsystemVedtaksdato`() {
        val a = standardKravgrunnlag()
        val b = standardKravgrunnlag(fagsystemVedtaksdato = LocalDate.of(2021, 3, 1))

        a.harNokOverlapp(b) shouldBe true
        a.kanBrukesUtenNyVurdering(b) shouldBe true
        a shouldNotBe b
        a.hashCode() shouldNotBe b.hashCode()
    }

    @Test
    fun `kravgrunnlag med ulik vedtakGjelder`() {
        val annen = Aktør.Person("20046912346")
        val a = standardKravgrunnlag()
        val b = standardKravgrunnlag(vedtakGjelder = annen)

        a.harNokOverlapp(b) shouldBe true
        a.kanBrukesUtenNyVurdering(b) shouldBe true
        a shouldNotBe b
        a.hashCode() shouldNotBe b.hashCode()
    }

    @Test
    fun `kravgrunnlag med ulik utbetalesTil`() {
        val a = standardKravgrunnlag()
        val b = standardKravgrunnlag(utbetalesTil = Aktør.Person("20046912346"))

        a.harNokOverlapp(b) shouldBe true
        a.kanBrukesUtenNyVurdering(b) shouldBe true
        a shouldNotBe b
        a.hashCode() shouldNotBe b.hashCode()
    }

    @Test
    fun `kravgrunnlag med ulik skalBeregneRenter`() {
        val a = standardKravgrunnlag()
        val b = standardKravgrunnlag(skalBeregneRenter = true)

        a.harNokOverlapp(b) shouldBe true
        a.kanBrukesUtenNyVurdering(b) shouldBe false
        a shouldNotBe b
        a.hashCode() shouldNotBe b.hashCode()
    }

    @Test
    fun `kravgrunnlag med ulik ansvarligEnhet`() {
        val a = standardKravgrunnlag()
        val b = standardKravgrunnlag(ansvarligEnhet = "4488")

        a.harNokOverlapp(b) shouldBe true
        a.kanBrukesUtenNyVurdering(b) shouldBe true
        a shouldNotBe b
        a.hashCode() shouldNotBe b.hashCode()
    }

    @Test
    fun `kravgrunnlag med ulik kontrollfelt`() {
        val a = standardKravgrunnlag()
        val b = standardKravgrunnlag(kontrollfelt = "annet")

        a.harNokOverlapp(b) shouldBe true
        a.kanBrukesUtenNyVurdering(b) shouldBe true
        a shouldNotBe b
        a.hashCode() shouldNotBe b.hashCode()
    }

    @Test
    fun `kravgrunnlag med ulik kravgrunnlagId`() {
        val a = standardKravgrunnlag()
        val b = standardKravgrunnlag(kravgrunnlagId = "annen")

        a.harNokOverlapp(b) shouldBe true
        a.kanBrukesUtenNyVurdering(b) shouldBe true
        a shouldNotBe b
        a.hashCode() shouldNotBe b.hashCode()
    }

    @Test
    fun `kravgrunnlag med ulik referanse`() {
        val a = standardKravgrunnlag()
        val b = standardKravgrunnlag(referanse = "annen")

        a.harNokOverlapp(b) shouldBe true
        a.kanBrukesUtenNyVurdering(b) shouldBe true
        a shouldNotBe b
        a.hashCode() shouldNotBe b.hashCode()
    }

    companion object {
        private val perioder = listOf(kravgrunnlagPeriode())
        private val vedtakId = BigInteger("123456")
        private val bruker = Aktør.Person("20046912345")

        private fun standardKravgrunnlag(
            vedtakId: BigInteger = this.vedtakId,
            kravstatuskode: KravgrunnlagHendelse.Kravstatuskode = KravgrunnlagHendelse.Kravstatuskode.ENDRET,
            fagsystemVedtaksdato: LocalDate = 1.februar(2021),
            vedtakGjelder: Aktør = bruker,
            utbetalesTil: Aktør = bruker,
            skalBeregneRenter: Boolean = false,
            ansvarligEnhet: String = "0425",
            kontrollfelt: String = "kontrollfelt",
            kravgrunnlagId: String = "kravgrunnlagId",
            referanse: String = "referanse",
            id: UUID = UUID.randomUUID(),
            opprettet: LocalDateTime = 1.februar(2021).atTime(12, 0),
            korrigering: Boolean = false,
        ) = KravgrunnlagHendelse(
            vedtakGjelder = vedtakGjelder,
            utbetalesTil = utbetalesTil,
            skalBeregneRenter = skalBeregneRenter,
            perioder = perioder,
            vedtakId = vedtakId,
            kravstatuskode = kravstatuskode,
            kontrollfelt = kontrollfelt,
            referanse = referanse,
            kravgrunnlagId = kravgrunnlagId,
            opprettet = opprettet,
            id = id,
            fagsystemVedtaksdato = fagsystemVedtaksdato,
            ansvarligEnhet = ansvarligEnhet,
            korrigering = korrigering,
        )
    }
}
