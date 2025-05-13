package no.nav.tilbakekreving.kravgrunnlag

import io.kotest.inspectors.forAll
import io.kotest.inspectors.forOne
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.februar
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import org.junit.jupiter.api.Test

class MånedsperiodeValidatorTest {
    @Test
    fun `månedsvalidator, flere ulike måneder`() {
        assertFeil(
            periode = 1.januar() til 28.februar(),
            forventetFeilmeldinger = listOf(
                "Perioden ${1.januar()} til ${28.februar()} er ikke innenfor samme kalendermåned",
            ),
        )
    }

    @Test
    fun `starter ikke første dag i måned`() {
        assertFeil(
            periode = 2.januar() til 31.januar(),
            forventetFeilmeldinger = listOf("Perioden ${2.januar()} til ${31.januar()} starter ikke første dag i måned"),
        )
    }

    @Test
    fun `slutter ikke siste dag i måned`() {
        assertFeil(
            periode = 1.januar() til 30.januar(),
            forventetFeilmeldinger = listOf("Perioden ${1.januar()} til ${30.januar()} slutter ikke siste dag i måned"),
        )
    }

    fun assertFeil(
        periode: Datoperiode,
        forventetFeilmeldinger: List<String>,
    ) {
        forventetFeilmeldinger.forAll { forventetFeilmelding ->
            PeriodeValidator.MånedsperiodeValidator.valider(periode).forOne {
                it.melding shouldBe forventetFeilmelding
            }
        }
    }
}
