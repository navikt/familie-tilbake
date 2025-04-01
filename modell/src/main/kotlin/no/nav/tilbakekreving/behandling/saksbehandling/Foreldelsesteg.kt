package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelsesperiodeDto
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.time.LocalDate

class Foreldelsesteg : Saksbehandlingsteg<VurdertForeldelseDto> {
    override val type: Behandlingssteg = Behandlingssteg.FORELDELSE

    override fun erFullstending(): Boolean = true

    override fun tilFrontendDto(): VurdertForeldelseDto {
        return VurdertForeldelseDto(
            foreldetPerioder =
                listOf(
                    VurdertForeldelsesperiodeDto(
                        periode = Datoperiode(LocalDate.now().minusYears(3), LocalDate.now().minusYears(3).plusMonths(2)),
                        feilutbetaltBeløp = BigDecimal("0.0"),
                        begrunnelse = "Automatisk behandlet. Utbetalingen har skjedd innen 30 måneder fra i dag.",
                        foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_FORELDET,
                        foreldelsesfrist = null,
                        oppdagelsesdato = null,
                    ),
                ),
        )
    }
}
