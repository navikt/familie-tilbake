package no.nav.familie.tilbake.common

import no.nav.tilbakekreving.kontrakter.periode.Periode
import java.time.temporal.Temporal

fun <T> List<Periode<T>>.erSammenhengende(): Boolean where T : Comparable<T>, T : Temporal =
    this.sorted().foldIndexed(true) { index, acc, periode ->
        if (index == 0) {
            acc
        } else {
            val forrigePeriode = this[index - 1]
            when {
                forrigePeriode.påfølgesAv(periode) -> acc
                else -> false
            }
        }
    }

fun <T> List<Periode<T>>.harOverlappende(): Boolean where T : Comparable<T>, T : Temporal =
    this
        .sorted()
        .zipWithNext { a, b ->
            a.overlapper(b)
        }.any { it }
