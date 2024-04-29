package no.nav.familie.tilbake.kravgrunnlag

import io.mockk.mockk
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEventPublisher
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
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

    private val kravgrunnlagService = KravgrunnlagService(
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
        endretKravgrunnlagEventPublisher = endretKravgrunnlagEventPublisher
    )

    @Test
    fun `Skal ikke oppdatere aktiv på nytt kravgrunnlag med eldre dato i kontrollfelt`() {

    }


    @Test
    fun `Skal oppdatere aktiv på nytt kravgrunnlag med nyere dato i kontrollfelt`() {

    }
}