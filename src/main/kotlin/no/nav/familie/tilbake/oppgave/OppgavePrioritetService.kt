package no.nav.familie.tilbake.oppgave

import no.nav.familie.tilbake.kontrakter.oppgave.Oppgave
import no.nav.familie.tilbake.kontrakter.oppgave.OppgavePrioritet
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class OppgavePrioritetService(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
) {
    fun utledOppgaveprioritet(
        behandlingId: UUID,
        oppgave: Oppgave? = null,
    ): OppgavePrioritet {
        val finnesKravgrunnlag = kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandlingId)

        return if (finnesKravgrunnlag) {
            val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)

            val feilutbetaltBeløp = kravgrunnlag.sumFeilutbetaling()

            when {
                feilutbetaltBeløp < BigDecimal(10_000) -> OppgavePrioritet.LAV
                feilutbetaltBeløp > BigDecimal(70_000) -> OppgavePrioritet.HOY
                else -> OppgavePrioritet.NORM
            }
        } else {
            oppgave?.prioritet ?: OppgavePrioritet.NORM
        }
    }
}
