package no.nav.familie.tilbake.totrinn

import no.nav.familie.tilbake.api.dto.Totrinnsstegsinfo
import no.nav.familie.tilbake.api.dto.TotrinnsvurderingDto
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.totrinn.domain.Totrinnsvurdering

object TotrinnMapper {

    fun tilRespons(totrinnsvurderinger: List<Totrinnsvurdering>,
                   behandlingsstegstilstand: List<Behandlingsstegstilstand>): TotrinnsvurderingDto {
        val totrinnsstegsinfo = when {
            totrinnsvurderinger.isEmpty() -> {
                behandlingsstegstilstand.filter {
                    it.behandlingssteg.kanBesluttes &&
                    it.behandlingsstegsstatus != Behandlingsstegstatus.AUTOUTFØRT
                }
                        .map { Totrinnsstegsinfo(behandlingssteg = it.behandlingssteg) }
            }
            else -> {
                totrinnsvurderinger.map {
                    Totrinnsstegsinfo(behandlingssteg = it.behandlingssteg,
                                      godkjent = it.godkjent,
                                      begrunnelse = it.begrunnelse)
                }
            }
        }
        return TotrinnsvurderingDto(totrinnsstegsinfo = totrinnsstegsinfo)
    }


}
