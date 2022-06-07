package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode

import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.Handlebarsperiode

data class HbVedtaksbrevsperiode(
    val periode: Handlebarsperiode,
    val kravgrunnlag: HbKravgrunnlag,
    val fakta: HbFakta,
    val vurderinger: HbVurderinger,
    val resultat: HbResultat
)
