package no.nav.familie.tilbake.dokumentbestilling.vedtak.domain

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Vedtaksbrevsoppsummering(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val oppsummeringFritekst: String?,
    @Column( "skal_sammenslaa_perioder")
    val skalSammenslåPerioder: Boolean,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
