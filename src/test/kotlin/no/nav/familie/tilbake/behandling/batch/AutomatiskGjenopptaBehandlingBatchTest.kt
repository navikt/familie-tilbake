package no.nav.familie.tilbake.behandling.batch

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.inspectors.forOne
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class AutomatiskGjenopptaBehandlingBatchTest(
    @Autowired private val kravgrunnlagRepository: KravgrunnlagRepository,
) : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var automatiskGjenopptaBehandlingBatch: AutomatiskGjenopptaBehandlingBatch

    @Autowired
    private lateinit var historikkService: HistorikkService

    @Test
    fun `skal gjenoppta behandling som venter på varsel og tidsfristen har utgått`() {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId = fagsak.id).copy(status = Behandlingsstatus.UTREDES))
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id))
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingId = behandling.id,
                behandlingssteg = Behandlingssteg.VARSEL,
                behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                tidsfrist = LocalDate.now().minusWeeks(4),
            ),
        )
        shouldNotThrow<RuntimeException> { automatiskGjenopptaBehandlingBatch.automatiskGjenopptaBehandling() }

        historikkService.hentHistorikkinnslag(behandling.id) forOne {
            it.tittel shouldBe "Behandling gjenopptatt"
        }
    }

    @Test
    fun `skal gjenoppta behandling som venter på avvent dokumentasjon`() {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId = fagsak.id).copy(status = Behandlingsstatus.UTREDES))
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id))
        val tidsfrist = LocalDate.now().minusWeeks(1)
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingId = behandling.id,
                behandlingssteg = Behandlingssteg.VILKÅRSVURDERING,
                behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                venteårsak = Venteårsak.AVVENTER_DOKUMENTASJON,
                tidsfrist = tidsfrist,
            ),
        )
        shouldNotThrow<RuntimeException> { automatiskGjenopptaBehandlingBatch.automatiskGjenopptaBehandling() }

        historikkService.hentHistorikkinnslag(behandling.id) forOne {
            it.tittel shouldBe "Behandling gjenopptatt"
        }
    }
}
