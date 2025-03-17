package no.nav.familie.tilbake.faktaomfeilutbetaling.domain

import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class FaktaFeilutbetalingsperiode(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val periode: Månedsperiode,
    val hendelsestype: Hendelsestype,
    val hendelsesundertype: Hendelsesundertype,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
