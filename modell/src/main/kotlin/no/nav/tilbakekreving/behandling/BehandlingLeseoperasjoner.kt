package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatDto
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingDto
import no.nav.tilbakekreving.api.v1.dto.TotrinnsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import java.time.LocalDateTime
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatDto as FrontendBeregningsresultatDto

interface BehandlingLeseoperasjoner {
    val foreldelsestegDto: FrontendDto<VurdertForeldelseDto>

    val vilkårsvurderingsstegDto: FrontendDto<VurdertVilkårsvurderingDto>

    val fatteVedtakStegDto: FrontendDto<TotrinnsvurderingDto>

    fun faktastegFrontendDto(
        opprettelsesvalg: Opprettelsesvalg,
        tilbakekrevingOpprettet: LocalDateTime,
    ): FaktaFeilutbetalingDto

    fun beregnForFrontend(): BeregningsresultatDto

    fun hentVedtaksresultatForFrontend(): FrontendBeregningsresultatDto
}
