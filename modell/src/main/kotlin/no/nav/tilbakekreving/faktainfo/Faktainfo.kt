package no.nav.tilbakekreving.faktainfo

import no.nav.tilbakekreving.api.v2.Opprettelsevalg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import java.util.UUID

data class Faktainfo(
    val id: UUID = UUID.randomUUID(),
    val revurderingsårsak: String,
    val revurderingsresultat: String,
    val opprettelsevalg: Opprettelsevalg,
    val konsekvensForYtelser: Set<String> = emptySet(),
    val aktiv: Boolean = true,
    // Hva er forskjellen mellom begrunnelse og revurderingsårsak?
    val begrunnelse: String?,
    val perioder: Set<FaktaFeilutbetalingsperiode> = setOf(),
    val vurderingAvBrukersUttalelse: HarBrukerUttaltSeg? = null,
    val versjon: Long = 0,
)

data class FaktaFeilutbetalingsperiode(
    val periode: Månedsperiode,
    val hendelsestype: Hendelsestype,
    val hendelsesundertype: Hendelsesundertype,
)

data class HarBrukerUttaltSeg(
    val harBrukerUttaltSeg: HarBrukerUttaltSeg,
    val beskrivelse: String?,
)
