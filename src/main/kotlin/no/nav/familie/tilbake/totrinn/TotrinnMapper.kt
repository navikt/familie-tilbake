package no.nav.familie.tilbake.totrinn

import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.totrinn.domain.Totrinnsvurdering
import no.nav.tilbakekreving.api.v1.dto.Totrinnsstegsinfo
import no.nav.tilbakekreving.api.v1.dto.TotrinnsvurderingDto
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus

object TotrinnMapper {
    fun tilRespons(
        totrinnsvurderinger: List<Totrinnsvurdering>,
        behandlingsstegstilstand: List<Behandlingsstegstilstand>,
    ): TotrinnsvurderingDto {
        val totrinnsstegsinfo =
            if (totrinnsvurderinger.isEmpty()) {
                hentStegSomGjelderForTotrinn(behandlingsstegstilstand)
            } else {
                totrinnsvurderinger.map {
                    Totrinnsstegsinfo(
                        behandlingssteg = it.behandlingssteg,
                        godkjent = it.godkjent,
                        begrunnelse = it.begrunnelse,
                    )
                } +
                    hentStegSomGjelderForTotrinn(behandlingsstegstilstand) // Ny behandlingssteg kan være gyldig for totrinn
                        .filter { stegstilstand -> totrinnsvurderinger.none { it.behandlingssteg == stegstilstand.behandlingssteg } }
            }
        return TotrinnsvurderingDto(totrinnsstegsinfo = totrinnsstegsinfo.sortedBy { it.behandlingssteg.sekvens })
    }

    private fun hentStegSomGjelderForTotrinn(behandlingsstegstilstand: List<Behandlingsstegstilstand>) =
        behandlingsstegstilstand
            .filter {
                it.behandlingssteg.kanBesluttes &&
                    it.behandlingsstegsstatus != Behandlingsstegstatus.AUTOUTFØRT
            }.map { Totrinnsstegsinfo(behandlingssteg = it.behandlingssteg) }
}
