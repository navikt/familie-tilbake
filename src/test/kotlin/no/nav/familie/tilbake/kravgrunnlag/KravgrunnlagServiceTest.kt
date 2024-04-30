package no.nav.familie.tilbake.kravgrunnlag

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEventPublisher
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KravgrunnlagServiceTest {
    private val kravgrunnlagRepository: KravgrunnlagRepository = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()
    private val mottattXmlService: ØkonomiXmlMottattService = mockk()
    private val stegService: StegService = mockk()
    private val behandlingskontrollService: BehandlingskontrollService = mockk()
    private val taskService: TaskService = mockk()
    private val tellerService: TellerService = mockk()
    private val oppgaveTaskService: OppgaveTaskService = mockk()
    private val historikkTaskService: HistorikkTaskService = mockk()
    private val hentFagsystemsbehandlingService: HentFagsystemsbehandlingService = mockk()
    private val endretKravgrunnlagEventPublisher: EndretKravgrunnlagEventPublisher = mockk()

    private val kravgrunnlagService =
        KravgrunnlagService(
            kravgrunnlagRepository = kravgrunnlagRepository,
            behandlingRepository = behandlingRepository,
            mottattXmlService = mottattXmlService,
            stegService = stegService,
            behandlingskontrollService = behandlingskontrollService,
            taskService = taskService,
            tellerService = tellerService,
            oppgaveTaskService = oppgaveTaskService,
            historikkTaskService = historikkTaskService,
            hentFagsystemsbehandlingService = hentFagsystemsbehandlingService,
            endretKravgrunnlagEventPublisher = endretKravgrunnlagEventPublisher,
        )

    @Test
    fun `Skal ikke oppdatere aktiv på nytt kravgrunnlag med eldre dato i kontrollfelt`() {
        val gammeltKravgrunnlag = Testdata.kravgrunnlag431.copy(kontrollfelt = "2024-04-29-18.50.15.236317")
        val nyttKravgrunnlag = Testdata.kravgrunnlag431.copy(kontrollfelt = "2024-04-29-18.50.15.236316")

        val nyttKravgrunnlagSlot = slot<Kravgrunnlag431>()
        val gammeltKravgrunnlagSlot = slot<Kravgrunnlag431>()

        assertThat(gammeltKravgrunnlag.aktiv).isTrue

        every { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(any()) } returns true
        every { kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(any()) } returns gammeltKravgrunnlag
        every { kravgrunnlagRepository.insert(capture(nyttKravgrunnlagSlot)) } returns mockk()
        every { kravgrunnlagRepository.update(capture(gammeltKravgrunnlagSlot)) } returns mockk()

        kravgrunnlagService.lagreKravgrunnlag(nyttKravgrunnlag, Ytelsestype.BARNETRYGD)

        assertThat(gammeltKravgrunnlagSlot.captured.aktiv).isTrue
        assertThat(nyttKravgrunnlagSlot.captured.aktiv).isFalse
    }

    @Test
    fun `Skal oppdatere aktiv på nytt kravgrunnlag med nyere dato i kontrollfelt`() {
        val gammeltKravgrunnlag = Testdata.kravgrunnlag431.copy(kontrollfelt = "2024-04-29-18.50.15.236316")
        val nyttKravgrunnlag = Testdata.kravgrunnlag431.copy(kontrollfelt = "2024-04-29-18.50.15.236317")

        val nyttKravgrunnlagSlot = slot<Kravgrunnlag431>()
        val gammeltKravgrunnlagSlot = slot<Kravgrunnlag431>()

        assertThat(gammeltKravgrunnlag.aktiv).isTrue

        every { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(any()) } returns true
        every { kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(any()) } returns gammeltKravgrunnlag
        every { kravgrunnlagRepository.update(capture(gammeltKravgrunnlagSlot)) } returns mockk()
        every { kravgrunnlagRepository.insert(capture(nyttKravgrunnlagSlot)) } returns mockk()

        kravgrunnlagService.lagreKravgrunnlag(nyttKravgrunnlag, Ytelsestype.OVERGANGSSTØNAD)

        assertThat(gammeltKravgrunnlagSlot.captured.aktiv).isFalse
        assertThat(nyttKravgrunnlagSlot.captured.aktiv).isTrue
    }

    @Test
    fun `Skal lagre nytt kravgrunnlag dersom det ikke finnes et kravgrunnlag fra før`() {
        val nyttKravgrunnlag = Testdata.kravgrunnlag431.copy(kontrollfelt = "2024-04-29-18.50.15.236317")

        val nyttKravgrunnlagSlot2 = slot<Kravgrunnlag431>()

        every { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(any()) } returns false
        every { kravgrunnlagRepository.insert(capture(nyttKravgrunnlagSlot2)) } returns mockk()

        kravgrunnlagService.lagreKravgrunnlag(nyttKravgrunnlag, Ytelsestype.OVERGANGSSTØNAD)

        assertThat(nyttKravgrunnlagSlot2.captured.aktiv).isTrue
    }
}
