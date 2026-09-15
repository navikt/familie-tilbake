package no.nav.familie.tilbake.dokumentbestilling.henleggelse

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.string.shouldContain
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.task.PubliserJournalpostTask
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmottager
import no.nav.tilbakekreving.pdf.validering.PdfaValidator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class HenleggelsesbrevServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var henleggelsesbrevService: HenleggelsesbrevService

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var taskService: TaskService

    private lateinit var behandling: Behandling
    private lateinit var fagsak: Fagsak

    @BeforeEach
    fun setup() {
        fagsak = fagsakRepository.insert(Testdata.fagsak())
        behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId = fagsak.id))
    }

    @Test
    fun `sendHenleggelsebrev skal sende henleggelsesbrev`() {
        brevsporingRepository.insert(Testdata.lagBrevsporing(behandling.id))

        henleggelsesbrevService.sendHenleggelsebrev(behandling.id, null, Brevmottager.BRUKER)

        taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET)).shouldHaveSingleElement {
            it.type == PubliserJournalpostTask.TYPE &&
                it.payload.contains(behandling.id.toString()) &&
                it.metadata.getProperty("brevtype") == Brevtype.HENLEGGELSE.name
        }
    }

    @Test
    fun `hentForhåndsvisningHenleggelsesbrev skal returnere pdf for henleggelsebrev`() {
        brevsporingRepository.insert(Testdata.lagBrevsporing(behandling.id))

        val bytes = henleggelsesbrevService.hentForhåndsvisningHenleggelsesbrev(behandling.id, null)

        PdfaValidator.validatePdf(bytes)
    }

    @Test
    fun `hentForhåndsvisningHenleggelsesbrev skal returnere pdf for henleggelsebrev for tilbakekreving revurdering`() {
        behandlingRepository.update(behandling.copy(type = Behandlingstype.REVURDERING_TILBAKEKREVING))

        val bytes =
            henleggelsesbrevService.hentForhåndsvisningHenleggelsesbrev(
                behandling.id,
                REVURDERING_HENLEGGELSESBREV_FRITEKST,
            )

        PdfaValidator.validatePdf(bytes)
    }

    @Test
    fun `sendHenleggelsebrev skal ikke sende henleggelsesbrev hvis varselbrev ikke sendt`() {
        val e =
            shouldThrow<IllegalStateException> {
                henleggelsesbrevService.sendHenleggelsebrev(
                    behandling.id,
                    null,
                    Brevmottager.BRUKER,
                )
            }

        e.message shouldContain "varsel ikke er sendt"
    }

    @Test
    fun `sendHenleggelsebrev skal ikke sende henleggelsesbrev for tilbakekreving revurdering uten fritekst`() {
        behandlingRepository.update(behandling.copy(type = Behandlingstype.REVURDERING_TILBAKEKREVING))

        val e =
            shouldThrow<IllegalStateException> {
                henleggelsesbrevService.sendHenleggelsebrev(
                    behandling.id,
                    null,
                    Brevmottager.BRUKER,
                )
            }

        e.message shouldContain "henleggelsesbrev uten fritekst"
    }

    companion object {
        private const val REVURDERING_HENLEGGELSESBREV_FRITEKST = "Revurderingen ble henlagt"
    }
}
