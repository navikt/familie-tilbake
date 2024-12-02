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
}
