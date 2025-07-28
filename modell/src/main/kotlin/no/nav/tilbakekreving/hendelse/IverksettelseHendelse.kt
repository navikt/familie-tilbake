package no.nav.tilbakekreving.hendelse

import java.math.BigInteger
import java.util.UUID

data class IverksettelseHendelse(
    val iverksattVedtakId: UUID,
    val vedtakId: BigInteger,
)
