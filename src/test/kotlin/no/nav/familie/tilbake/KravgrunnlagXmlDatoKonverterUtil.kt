package no.nav.familie.tilbake

import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import java.time.LocalDate

fun String.konverterDatoIXMLTilIkkeForeldet(): String {
    var muterbarXml = this

    val detaljertKravgrunnlagDto = KravgrunnlagUtil.unmarshalKravgrunnlag(this)
    val perioder = detaljertKravgrunnlagDto.tilbakekrevingsPeriode.flatMap { listOf(it.periode.fom, it.periode.tom) }

    perioder.forEach { dato ->
        muterbarXml = muterbarXml.replace(dato.toString(), dato.lagDatoIkkeForeldet().toString())
    }

    return muterbarXml
}

fun LocalDate.lagDatoIkkeForeldet(): LocalDate {
    return withYear(LocalDate.now().year.minus(1))
}
