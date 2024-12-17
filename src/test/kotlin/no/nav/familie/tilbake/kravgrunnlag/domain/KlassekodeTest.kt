package no.nav.familie.tilbake.kravgrunnlag.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KlassekodeTest {
    @Nested
    inner class FraKode {
        @Test
        fun `skal mappe fra kode med spesialtegn til Klassekode med overstyrtKlassekode`() {
            // Act
            val klassekode = Klassekode.fraKode("BAUTV-OP", Klassetype.YTEL)

            // Assert
            assertThat(klassekode).isEqualTo(Klassekode.BAUTV_OP)
        }
    }

    @Nested
    inner class TilKlassekodeNavn {
        @Test
        fun `skal bruke overstyrtKlassekode dersom den finnes ellers enum navn`() {
            // Act
            val klassekodeNavn = Klassekode.BAUTV_OP.tilKlassekodeNavn()

            // Assert
            assertThat(klassekodeNavn).isEqualTo("BAUTV-OP")
        }

        @Test
        fun `skal bruke enum navn dersom overstyrtKlassekode ikke er satt`() {
            // Act
            val klassekodeNavn = Klassekode.BATR.tilKlassekodeNavn()

            // Assert
            assertThat(klassekodeNavn).isEqualTo("BATR")
        }
    }
}
