package no.nav.familie.tilbake.service.dokumentbestilling.felles.task

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.historikkinnslag.LagHistorikkinnslagTask
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevtype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.Properties
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class LagreBrevsporingTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var lagreBrevsporingTask: LagreBrevsporingTask

    private val behandling = Testdata.behandling
    private val behandlingId = behandling.id

    private val dokumentId: String = "testverdi"
    private val journalpostId: String = "testverdi"

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `doTask skal lagre brevsporing for varselbrev`() {
        lagreBrevsporingTask.doTask(opprettTask(behandlingId, Brevtype.VARSEL))

        assertBrevsporing(Brevtype.VARSEL)
    }

    @Test
    fun `onCompletion skal lage historikk task for varselbrev`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandlingId, Brevtype.VARSEL))

        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT)
    }

    @Test
    fun `doTask skal lagre brevsporing for korrigert varselbrev`() {
        lagreBrevsporingTask.doTask(opprettTask(behandlingId, Brevtype.KORRIGERT_VARSEL))

        assertBrevsporing(Brevtype.KORRIGERT_VARSEL)
    }

    @Test
    fun `onCompletion skal lage historikk task for korrigert varselbrev`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandlingId, Brevtype.KORRIGERT_VARSEL))

        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.KORRIGERT_VARSELBREV_SENDT)
    }

    @Test
    fun `doTask skal lagre brevsporing for henleggelsesbrev`() {
        lagreBrevsporingTask.doTask(opprettTask(behandlingId, Brevtype.HENLEGGELSE))

        assertBrevsporing(Brevtype.HENLEGGELSE)
    }

    @Test
    fun `onCompletion skal lage historikk task for henleggelsesbrev`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandlingId, Brevtype.HENLEGGELSE))

        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT)
    }

    @Test
    fun `doTask skal lagre brevsporing for innhent dokumentasjon`() {
        lagreBrevsporingTask.doTask(opprettTask(behandlingId, Brevtype.INNHENT_DOKUMENTASJON))

        assertBrevsporing(Brevtype.INNHENT_DOKUMENTASJON)
    }

    @Test
    fun `onCompletion skal lage historikk task for innhent dokumentasjon`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandlingId, Brevtype.INNHENT_DOKUMENTASJON))

        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.INNHENT_DOKUMENTASJON_BREV_SENDT)
    }

    @Test
    fun `doTask skal lagre brevsporing for vedtaksbrev`() {
        lagreBrevsporingTask.doTask(opprettTask(behandlingId, Brevtype.VEDTAK))

        assertBrevsporing(Brevtype.VEDTAK)
    }

    @Test
    fun `onCompletion skal lage historikk task for vedtaksbrev`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandlingId, Brevtype.VEDTAK))

        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.VEDTAKSBREV_SENDT)
    }

    private fun opprettTask(behandlingId: UUID, brevtype: Brevtype): Task {
        return Task(type = LagreBrevsporingTask.TYPE,
                    payload = behandlingId.toString(),
                    properties = Properties().apply {
                        this["dokumentId"] = dokumentId
                        this["journalpostId"] = journalpostId
                        this["brevtype"] = brevtype.name
                        this["mottaker"] = Brevmottager.BRUKER.name
                    })
    }

    private fun assertBrevsporing(brevtype: Brevtype) {
        val brevsporing = brevsporingRepository.findFirstByBehandlingIdAndBrevtypeOrderBySporbarOpprettetTidDesc(behandlingId,
                                                                                                                 brevtype)
        assertNotNull(brevsporing)
        assertEquals(dokumentId, brevsporing.dokumentId)
        assertEquals(journalpostId, brevsporing.journalpostId)
    }

    private fun assertHistorikkTask(historikkinnslagstype: TilbakekrevingHistorikkinnslagstype) {
        assertTrue {
            taskRepository.findByStatus(Status.UBEHANDLET).any {
                LagHistorikkinnslagTask.TYPE == it.type &&
                historikkinnslagstype.name == it.metadata["historikkinnslagType"] &&
                Aktør.VEDTAKSLØSNING.name == it.metadata["aktor"] &&
                behandlingId.toString() == it.payload
            }
        }
    }
}
