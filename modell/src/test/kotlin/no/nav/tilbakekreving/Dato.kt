package no.nav.tilbakekreving

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.time.LocalDate

val Int.januar: LocalDate get() = LocalDate.of(2018, 1, this)
val Int.februar: LocalDate get() = LocalDate.of(2018, 2, this)
val Int.mars: LocalDate get() = LocalDate.of(2018, 3, this)
val Int.april: LocalDate get() = LocalDate.of(2018, 4, this)
val Int.mai: LocalDate get() = LocalDate.of(2018, 5, this)
val Int.juni: LocalDate get() = LocalDate.of(2018, 6, this)
val Int.juli: LocalDate get() = LocalDate.of(2018, 7, this)
val Int.august: LocalDate get() = LocalDate.of(2018, 8, this)
val Int.september: LocalDate get() = LocalDate.of(2018, 9, this)
val Int.oktober: LocalDate get() = LocalDate.of(2018, 10, this)
val Int.november: LocalDate get() = LocalDate.of(2018, 11, this)
val Int.desember: LocalDate get() = LocalDate.of(2018, 12, this)

infix fun LocalDate.til(tom: LocalDate) = Datoperiode(this, tom)
