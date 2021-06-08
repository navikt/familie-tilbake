package no.nav.familie.tilbake.dokumentbestilling.handlebars

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class KroneFormattererMedTusenskilleTest {

    @Test
    fun `medTusenskille skal gi riktig tusenskille`() {
        assertThat(KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(1), ' ')).isEqualTo("1")
        assertThat(KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(12), ' ')).isEqualTo("12")
        assertThat(KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(123), ' ')).isEqualTo("123")
        assertThat(KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(1234), ' ')).isEqualTo("1 234")
        assertThat(KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(12345), ' ')).isEqualTo("12 345")
        assertThat(KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(123456), ' '))
                .isEqualTo("123 456")
        assertThat(KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(1234567), ' '))
                .isEqualTo("1 234 567")
        assertThat(KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(12345678), ' '))
                .isEqualTo("12 345 678")
        assertThat(KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(123456789), ' '))
                .isEqualTo("123 456 789")
        assertThat(KroneFormattererMedTusenskille.medTusenskille(BigDecimal.valueOf(1234567), '\u00A0'))
                .isEqualTo("1\u00A0234\u00A0567")
    }
}
