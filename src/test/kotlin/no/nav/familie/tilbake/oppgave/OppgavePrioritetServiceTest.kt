package no.nav.familie.tilbake.oppgave

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.data.Testdata.lagFeilBeløp
import no.nav.familie.tilbake.data.Testdata.lagYtelBeløp
import no.nav.familie.tilbake.kontrakter.oppgave.Oppgave
import no.nav.familie.tilbake.kontrakter.oppgave.OppgavePrioritet
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode.Companion.til
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

internal class OppgavePrioritetServiceTest {
    val kravgrunnlagRepository = mockk<KravgrunnlagRepository>()
    val oppgavePrioritetService = OppgavePrioritetService(kravgrunnlagRepository)

    @Test
    fun `skal gi prioritet LAV for feilutbetaling på 9999`() {
        val behandlingId = UUID.randomUUID()
        val oppgave = Oppgave()

        every { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandlingId) } returns true

        every { kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId) } returns
            lagKravgrunnlagMedFeilutbetaling(
                9999,
            )

        assertThat(oppgavePrioritetService.utledOppgaveprioritet(behandlingId, oppgave)).isEqualTo(OppgavePrioritet.LAV)
    }

    @Test
    fun `skal gi prioritet NORM for feilutbetaling på 30 000`() {
        val behandlingId = UUID.randomUUID()
        val oppgave = Oppgave()

        every { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandlingId) } returns true

        every { kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId) } returns
            lagKravgrunnlagMedFeilutbetaling(
                30_000,
            )

        assertThat(oppgavePrioritetService.utledOppgaveprioritet(behandlingId, oppgave)).isEqualTo(OppgavePrioritet.NORM)
    }

    @Test
    fun `skal gi prioritet HØY for feilutbetaling på 75 000`() {
        val behandlingId = UUID.randomUUID()
        val oppgave = Oppgave()

        every { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandlingId) } returns true

        every { kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId) } returns
            lagKravgrunnlagMedFeilutbetaling(
                75_000,
            )

        assertThat(oppgavePrioritetService.utledOppgaveprioritet(behandlingId, oppgave)).isEqualTo(OppgavePrioritet.HOY)
    }

    @Test
    fun `skal gi oppgavens eksisterende prioritet dersom det ikke finnes kravgrunnlag`() {
        val behandlingId = UUID.randomUUID()
        val oppgave = Oppgave(prioritet = OppgavePrioritet.HOY)

        every { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandlingId) } returns false

        assertThat(oppgavePrioritetService.utledOppgaveprioritet(behandlingId, oppgave)).isEqualTo(OppgavePrioritet.HOY)
    }

    private fun lagKravgrunnlagMedFeilutbetaling(feilutbetaling: Int): Kravgrunnlag431 {
        val periode = Testdata.lagKravgrunnlagsperiode(januar(2020) til januar(2023)).copy(
            beløp = setOf(
                lagFeilBeløp(BigDecimal(feilutbetaling)),
                lagYtelBeløp(BigDecimal(feilutbetaling), BigDecimal(10)),
            ),
        )

        return Testdata.lagKravgrunnlag(Testdata.lagBehandling(Testdata.fagsak().id).id).copy(perioder = setOf(periode))
    }
}
