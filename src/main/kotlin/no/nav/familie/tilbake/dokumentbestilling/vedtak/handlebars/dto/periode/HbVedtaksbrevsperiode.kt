package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode

import no.nav.familie.kontrakter.felles.Datoperiode

data class HbVedtaksbrevsperiode(
    val periode: Datoperiode,
    val kravgrunnlag: HbKravgrunnlag,
    val fakta: HbFakta,
    val vurderinger: HbVurderinger,
    val resultat: HbResultat
)
