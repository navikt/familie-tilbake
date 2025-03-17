package no.nav.tilbakekreving.api.v1.dto

import java.math.BigDecimal
import java.time.LocalDate
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode

data class VurdertForeldelseDto(
    val foreldetPerioder: List<VurdertForeldelsesperiodeDto>,
)

data class VurdertForeldelsesperiodeDto(
    val periode: Datoperiode,
    val feilutbetaltBeløp: BigDecimal,
    val begrunnelse: String? = null,
    val foreldelsesvurderingstype: Foreldelsesvurderingstype? = null,
    val foreldelsesfrist: LocalDate? = null,
    val oppdagelsesdato: LocalDate? = null,
)
