package no.nav.familie.tilbake.service

import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.service.beregning.LogiskPeriodeTjeneste
import no.nav.familie.tilbake.service.modell.LogiskPeriode
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.SortedMap
import java.util.UUID

@Service
class KravgrunnlagTjeneste(private val kravgrunnlagRepository: KravgrunnlagRepository) {

    fun utledLogiskPeriode(behandlingId: UUID): List<LogiskPeriode> {
        return LogiskPeriodeTjeneste.utledLogiskPeriode(finnFeilutbetalingPrPeriode(behandlingId))
    }

    private fun finnFeilutbetalingPrPeriode(behandlingId: UUID): SortedMap<Periode, BigDecimal> {
        val kravgrunnlag: Kravgrunnlag431 = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        return kravgrunnlag.perioder
                .associateBy({ it.periode },
                             { periode ->
                                 periode.beløp
                                         .filter { it.klassetype == Klassetype.FEIL }
                                         .sumOf { it.nyttBeløp }
                             })
                .filter { it.value.compareTo(BigDecimal.ZERO) != 0 }
                .toSortedMap(Periode.COMPARATOR)
    }

}