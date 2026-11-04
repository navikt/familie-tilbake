package no.nav.tilbakekreving.breeeev.standardtekster.revurdering

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.breeeev.standardtekster.Bunntekst
import org.junit.jupiter.api.Test

class RevurderingTeksterTest {
    @Test
    fun `standardtekster`() {
        Bunntekst.REVURDERING_STANDARD_BUNNTEKSTER shouldBe setOf(
            Bunntekst.HVORDAN_BETALE_TILBAKE,
            Bunntekst.RETT_TIL_Å_KLAGE,
            Bunntekst.RETT_TIL_INNSYN,
            Bunntekst.PERSONVÆRN_ERKLÆRING,
            Bunntekst.RETT_TIL_HJELP,
            Bunntekst.SPØRSMÅL,
        )
    }
}
