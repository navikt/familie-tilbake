package no.nav.tilbakekreving.breeeev.standardtekster.forhåndsvarsel

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.fagsystem.Ytelse
import org.junit.jupiter.api.Test

class ForhåndsvarselTeksterTest {

    @Test
    fun `spørsmål bruker riktig url for ytelsen`() {
        Bunntekst.SPØRSMÅL.avsnitt(Ytelse.Barnetrygd) shouldBe arrayOf(
            "Du finner mer informasjon på nav.no/barnetrygd. På nav.no/kontakt kan du chatte eller skrive til oss. Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00.",
        )

        Bunntekst.SPØRSMÅL.avsnitt(Ytelse.Tilleggsstønad) shouldBe arrayOf(
            "Du finner mer informasjon på nav.no/tilleggsstonader. På nav.no/kontakt kan du chatte eller skrive til oss. Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00.",
        )
    }
}
