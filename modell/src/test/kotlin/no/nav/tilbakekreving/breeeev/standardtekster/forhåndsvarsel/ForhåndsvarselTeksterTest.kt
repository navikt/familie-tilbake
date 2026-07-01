package no.nav.tilbakekreving.breeeev.standardtekster.forhåndsvarsel

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.breeeev.standardtekster.forhåndsvarsel.ForhåndsvarselTekster.DETTE_LEGGES_VEKT_PÅ
import no.nav.tilbakekreving.breeeev.standardtekster.forhåndsvarsel.ForhåndsvarselTekster.FORELØPIG_VURDERING
import no.nav.tilbakekreving.breeeev.standardtekster.forhåndsvarsel.ForhåndsvarselTekster.HVORDAN_UTTALE_SEG
import no.nav.tilbakekreving.breeeev.standardtekster.forhåndsvarsel.ForhåndsvarselTekster.ÅRSAK_TIL_FEILUTBETALING
import no.nav.tilbakekreving.fagsystem.Ytelse
import org.junit.jupiter.api.Test

class ForhåndsvarselTeksterTest {
    @Test
    fun `standardtekster`() {
        ForhåndsvarselTekster.STANDARD_BUNNTEKSTER shouldBe arrayOf(
            ÅRSAK_TIL_FEILUTBETALING,
            DETTE_LEGGES_VEKT_PÅ,
            FORELØPIG_VURDERING,
            HVORDAN_UTTALE_SEG,
            ForhåndsvarselTekster.RETT_TIL_INNSYN,
            ForhåndsvarselTekster.PERSONVÆRN_ERKLÆRING,
            ForhåndsvarselTekster.SPØRSMÅL,
        )
    }

    @Test
    fun `får riktig tekst i den avsnitten som er basert på ytelse`() {
        ForhåndsvarselTekster.SPØRSMÅL.avsnitt(Ytelse.Barnetrygd) shouldBe arrayOf(
            "Du finner mer informasjon på nav.no/barnetrygd. På nav.no/kontakt kan du chatte eller skrive til oss. Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00–15.00.",
        )

        ForhåndsvarselTekster.SPØRSMÅL.avsnitt(Ytelse.Tilleggsstønad) shouldBe arrayOf(
            "Du finner mer informasjon på nav.no/tilleggsstonader. På nav.no/kontakt kan du chatte eller skrive til oss. Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00–15.00.",
        )
    }
}
