package no.nav.tilbakekreving.entities

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning.ForskjellType
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class ForskjellEntityTest {
    @Test
    fun `mapper JustertBeløp til og fra entity`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val forskjell = KravgrunnlagSammenligning.Forskjell.JustertBeløp(periode, BigDecimal.valueOf(1234))

        val entity = forskjell.tilEntity(UUID.randomUUID(), null, null)

        entity.type shouldBe ForskjellType.JustertBeløp
        val tilbake = entity.fraEntity()
        tilbake shouldBe forskjell
    }

    @Test
    fun `mapper NyPeriode til og fra entity`() {
        val periode = 1.februar(2021) til 28.februar(2021)
        val forskjell = KravgrunnlagSammenligning.Forskjell.NyPeriode(periode)

        val entity = forskjell.tilEntity(null, UUID.randomUUID(), null)

        entity.type shouldBe ForskjellType.NyPeriode
        val tilbake = entity.fraEntity()
        tilbake shouldBe forskjell
    }
}
