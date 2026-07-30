package no.nav.tilbakekreving.entities

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.bruker
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.UUID

class KravgrunnlagHendelseEntityTest {
    @Test
    fun `feil rekkefølge på perioder i db blir gjenopprettet i riktig rekkefølge`() {
        val periode1 = DatoperiodeEntity(1.januar(2021), 10.januar(2021))
        val periode2 = DatoperiodeEntity(15.januar(2021), 20.januar(2021))
        val entity = KravgrunnlagHendelseEntity(
            id = UUID.randomUUID(),
            tilbakekrevingId = UUID.randomUUID().toString(),
            vedtakId = BigInteger.ONE,
            kravstatuskode = KravgrunnlagHendelse.Kravstatuskode.NY,
            fagsystemVedtaksdato = null,
            vedtakGjelder = AktørEntity(AktørType.Person, bruker().ident),
            utbetalesTil = AktørEntity(AktørType.Person, bruker().ident),
            skalBeregneRenter = false,
            ansvarligEnhet = "0425",
            kontrollfelt = UUID.randomUUID().toString(),
            kravgrunnlagId = UUID.randomUUID().toString(),
            referanse = UUID.randomUUID().toString(),
            perioder = listOf(
                KravgrunnlagPeriodeEntity(
                    id = UUID.randomUUID(),
                    kravgrunnlagId = UUID.randomUUID(),
                    periode = periode2,
                    månedligSkattebeløp = BigDecimal.ZERO,
                    beløp = emptyList(),
                ),
                KravgrunnlagPeriodeEntity(
                    id = UUID.randomUUID(),
                    kravgrunnlagId = UUID.randomUUID(),
                    periode = periode1,
                    månedligSkattebeløp = BigDecimal.ZERO,
                    beløp = emptyList(),
                ),
            ),
            opprettet = LocalDateTime.now(),
        )

        val gjenopprettetEntity = entity.fraEntity().tilEntity("")

        gjenopprettetEntity.perioder[0].periode shouldBe periode1
        gjenopprettetEntity.perioder[1].periode shouldBe periode2
    }
}
