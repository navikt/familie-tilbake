package no.nav.tilbakekreving

import io.kotest.inspectors.forOne

fun <T> Collection<T>.shouldGiveOne(assertion: (T) -> Unit): T {
    forOne(assertion)
    return single { runCatching { assertion(it) }.isSuccess }
}
