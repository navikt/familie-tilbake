package no.nav.tilbakekreving.breeeev.standardtekster

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.breeeev.standardtekster.HjemmelForTilbakekreving.Companion.formatter
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class HjemmelForTilbakekrevingTest {
    @ParameterizedTest
    @MethodSource("hjemler")
    fun `lager riktig tekst for hjemler`(hjemler: List<HjemmelForTilbakekreving>, forventetTekst: String) {
        hjemler.formatter() shouldBe forventetTekst
    }

    companion object {
        @JvmStatic
        fun hjemler(): Set<Arguments> {
            return setOf(
                Arguments.of(
                    listOf(HjemmelForTilbakekreving.FOLKETRYGDLOVEN_22_15),
                    "folketrygdloven § 22-15",
                ),
                Arguments.of(
                    listOf(
                        HjemmelForTilbakekreving.BARNETRYGDLOVEN_13,
                        HjemmelForTilbakekreving.FOLKETRYGDLOVEN_22_15,
                    ),
                    "barnetrygdloven § 13 samt folketrygdloven § 22-15",
                ),
                Arguments.of(
                    listOf(
                        HjemmelForTilbakekreving.FOLKETRYGDLOVEN_22_15,
                        HjemmelForTilbakekreving.FOLKETRYGDLOVEN_22_17A,
                    ),
                    "folketrygdloven §§ 22-15 og 22-17a",
                ),
                Arguments.of(
                    listOf(
                        HjemmelForTilbakekreving.BARNETRYGDLOVEN_13,
                        HjemmelForTilbakekreving.FOLKETRYGDLOVEN_22_15,
                        HjemmelForTilbakekreving.FORELDELSESLOVEN_2,
                        HjemmelForTilbakekreving.FORELDELSESLOVEN_3,
                    ),
                    "barnetrygdloven § 13, folketrygdloven § 22-15 samt foreldelsesloven §§ 2 og 3",
                ),
                Arguments.of(
                    listOf(
                        HjemmelForTilbakekreving.ARBEIDSMARKEDSLOVEN_22,
                        HjemmelForTilbakekreving.FOLKETRYGDLOVEN_22_15,
                        HjemmelForTilbakekreving.FOLKETRYGDLOVEN_22_17A,
                        HjemmelForTilbakekreving.FORELDELSESLOVEN_2,
                        HjemmelForTilbakekreving.FORELDELSESLOVEN_3,
                        HjemmelForTilbakekreving.FORELDELSESLOVEN_10,
                    ),
                    "arbeidsmarkedsloven § 22, folketrygdloven §§ 22-15 og 22-17a samt foreldelsesloven §§ 2, 3 og 10",
                ),
            )
        }
    }
}
