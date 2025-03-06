package no.nav.familie.tilbake.faktaomfeilutbetaling

import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.HarBrukerUttaltSeg
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.VurderingAvBrukersUttalelse
import no.nav.tilbakekreving.kontrakter.Månedsperiode
import java.time.YearMonth
import java.util.UUID

object FaktaFeilutbetalingDomainUtil {
    fun lagFaktaFeilutbetaling(
        behandlingId: UUID = UUID.randomUUID(),
        perioder: Set<FaktaFeilutbetalingsperiode> = setOf(lagFaktaFeilutbetalingsperiode()),
    ) = FaktaFeilutbetaling(
        behandlingId = behandlingId,
        aktiv = true,
        begrunnelse = "begrunnelse",
        perioder = perioder,
        vurderingAvBrukersUttalelse =
            VurderingAvBrukersUttalelse(
                id = UUID.randomUUID(),
                harBrukerUttaltSeg = HarBrukerUttaltSeg.JA,
                beskrivelse = "beskrivelse",
                aktiv = true,
            ),
    )

    fun lagFaktaFeilutbetalingsperiode(
        periode: Månedsperiode = Månedsperiode(YearMonth.now(), YearMonth.now().plusMonths(1)),
        hendelsestype: Hendelsestype = Hendelsestype.ANNET,
        hendelsesundertype: Hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
    ) = FaktaFeilutbetalingsperiode(
        periode = periode,
        hendelsestype = hendelsestype,
        hendelsesundertype = hendelsesundertype,
    )
}
