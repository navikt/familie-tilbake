package no.nav.familie.tilbake.service

import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.service.modell.BehandlingFeilutbetalingFakta
import no.nav.familie.tilbake.service.modell.HendelseTypeMedUndertypeDto
import no.nav.familie.tilbake.service.modell.LogiskPeriode
import no.nav.familie.tilbake.service.modell.LogiskPeriodeMedFaktaDto
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class FaktaFeilutbetalingTjeneste(private val faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository,
                                  private val kravgrunnlagTjeneste: KravgrunnlagTjeneste) {


    fun hentBehandlingFeilutbetalingFakta(behandling: Behandling): BehandlingFeilutbetalingFakta {
        val tilbakekrevingValg: String? = null  // TODO legge inn riktig objekt med verdier
        val logiskePerioder: List<LogiskPeriode> = kravgrunnlagTjeneste.utledLogiskPeriode(behandling.id)
        val fakta: FaktaFeilutbetaling? = faktaFeilutbetalingRepository.findByAktivIsTrueAndBehandlingId(behandling.id)
        val logiskePerioderMedFakta: List<LogiskPeriodeMedFaktaDto> = logiskePerioder.map { leggPåFakta(it, fakta) }
        return BehandlingFeilutbetalingFakta(perioder = logiskePerioderMedFakta,
                                             aktuellFeilUtbetaltBeløp = logiskePerioder.sumOf(LogiskPeriode::feilutbetaltBeløp),
                                             tidligereVarseltBeløp = behandling.aktivtVarsel?.varselbeløp,
                                             totalPeriode = omkringliggendePeriode(logiskePerioder),
                                             datoForRevurderingsvedtak = behandling.sisteResultat?.behandlingsvedtak?.vedtaksdato,
                                             behandlingsresultat = behandling.sisteResultat?.type,
                                             behandlingÅrsaker = behandling.årsaker.map { it.type },
                                             tilbakekrevingValg = tilbakekrevingValg,
                                             begrunnelse = fakta?.begrunnelse)
    }

    private fun omkringliggendePeriode(perioder: List<LogiskPeriode>): Periode {
        var totalPeriodeFom: LocalDate = LocalDate.MAX
        var totalPeriodeTom: LocalDate = LocalDate.MIN
        perioder.forEach {
            totalPeriodeFom = Periode.min(it.fom, totalPeriodeFom)
            totalPeriodeTom = Periode.max(it.tom, totalPeriodeTom)
        }
        return Periode(totalPeriodeFom, totalPeriodeTom)
    }

    private fun leggPåFakta(logiskPeriode: LogiskPeriode, fakta: FaktaFeilutbetaling?): LogiskPeriodeMedFaktaDto {
        val feilutbetalingPeriodeÅrsak =
                fakta?.perioder
                        ?.firstOrNull { logiskPeriode.periode == it.periode }
                        ?.let {
                            HendelseTypeMedUndertypeDto(it.hendelsestype,
                                                        it.hendelsesundertype)
                        }
        return LogiskPeriodeMedFaktaDto(logiskPeriode.periode,
                                        logiskPeriode.feilutbetaltBeløp,
                                        feilutbetalingPeriodeÅrsak)

    }
}