package no.nav.tilbakekreving.test.vilkårsvurdering

import java.math.BigDecimal

sealed interface BeløpIBeholdBuilder {
    data object Ingenting : BeløpIBeholdBuilder

    data class Deler(val beløp: BigDecimal) : BeløpIBeholdBuilder

    data object Hele : BeløpIBeholdBuilder
}
