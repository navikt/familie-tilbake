package no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak

import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import java.math.BigDecimal
import java.time.LocalDate

data class VedtakPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val hendelsestype: String,
    val hendelsesundertype: String? = null,
    val vilkårsresultat: UtvidetVilkårsresultat,
    val feilutbetaltBeløp: BigDecimal,
    val bruttoTilbakekrevingsbeløp: BigDecimal,
    val rentebeløp: BigDecimal,
    val harBruktSjetteLedd: Boolean = false,
    val aktsomhet: Aktsomhet? = null,
    val særligeGrunner: SærligeGrunner? = null,
)
