package no.nav.familie.tilbake.faktaomfeilutbetaling.domain

import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class VurderingAvBrukersUttalelse(
    @Id
    val id: UUID = UUID.randomUUID(),
    val harBrukerUttaltSeg: HarBrukerUttaltSeg,
    val beskrivelse: String?,
    val aktiv: Boolean = true,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
