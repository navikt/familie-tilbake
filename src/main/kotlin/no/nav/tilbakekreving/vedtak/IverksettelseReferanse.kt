package no.nav.tilbakekreving.vedtak

import java.util.UUID

data class IverksettelseReferanse(
    val id: UUID,
    val behandlingId: UUID,
    val nyModell: Boolean,
)
