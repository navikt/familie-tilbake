package no.nav.familie.tilbake.dokumentbestilling.varsel.manuelt

import io.kotest.matchers.collections.shouldHaveSingleElement
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.task.PubliserJournalpostTask
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.tilbakekreving.kontrakter.brev.Dokumentmalstype
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode.Companion.til
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmottager
import no.nav.tilbakekreving.pdf.validering.PdfaValidator
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ManueltVarselbrevServiceTest : OppslagSpringRunnerTest() {
    private val korrigertVarseltekst = "Sender korrigert varselbrev"
    private val varseltekst = "Sender manuelt varselbrev"

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var manueltVarselbrevService: ManueltVarselbrevService

    @Autowired
    private lateinit var taskService: TaskService

    private lateinit var behandling: Behandling
    private lateinit var fagsak: Fagsak

    @BeforeEach
    fun setup() {
        fagsak = fagsakRepository.insert(Testdata.fagsak())
        behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId = fagsak.id))
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id))
        faktaFeilutbetalingRepository.insert(
            Testdata.lagFaktaFeilutbetaling(behandling.id, setOf(januar(2025) til januar(2025))),
        )
    }

    @Test
    fun `sendManueltVarselBrev skal sende manuelt varselbrev`() {
        manueltVarselbrevService.sendManueltVarselBrev(behandling, varseltekst, Brevmottager.BRUKER)

        assertPublisertBrevTask(brevtype = Brevtype.VARSEL, mottager = Brevmottager.BRUKER, varsletBeløp = 10000L)
    }

    @Test
    fun `sendKorrigertVarselBrev skal sende korrigert varselbrev`() {
        manueltVarselbrevService.sendKorrigertVarselBrev(behandling, korrigertVarseltekst, Brevmottager.BRUKER)

        assertPublisertBrevTask(brevtype = Brevtype.KORRIGERT_VARSEL, mottager = Brevmottager.BRUKER, varsletBeløp = 10000L)
    }

    @Test
    fun `sendKorrigertVarselBrev skal sende korrigert varselbrev med verge`() {
        manueltVarselbrevService.sendKorrigertVarselBrev(behandling, korrigertVarseltekst, Brevmottager.VERGE)

        assertPublisertBrevTask(brevtype = Brevtype.KORRIGERT_VARSEL, mottager = Brevmottager.VERGE, varsletBeløp = 10000L)
    }

    @Test
    fun `hentForhåndsvisningManueltVarselbrev skal forhåndsvise manuelt varselbrev`() {
        val data =
            manueltVarselbrevService.hentForhåndsvisningManueltVarselbrev(
                behandling.id,
                Dokumentmalstype.VARSEL,
                varseltekst,
            )

        PdfaValidator.validatePdf(data)
    }

    @Test
    fun `hentForhåndsvisningManueltVarselbrev skal forhåndsvise korrigert varselbrev`() {
        val data =
            manueltVarselbrevService.hentForhåndsvisningManueltVarselbrev(
                behandling.id,
                Dokumentmalstype.KORRIGERT_VARSEL,
                varseltekst,
            )

        PdfaValidator.validatePdf(data)
    }

    private fun assertPublisertBrevTask(
        brevtype: Brevtype,
        mottager: Brevmottager,
        varsletBeløp: Long,
    ) {
        taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET)).shouldHaveSingleElement {
            it.type == PubliserJournalpostTask.TYPE &&
                it.payload.contains(behandling.id.toString()) &&
                it.metadata.getProperty("brevtype") == brevtype.name &&
                it.metadata.getProperty("mottager") == mottager.name &&
                it.metadata.getProperty("varselbeløp") == varsletBeløp.toString()
        }
    }
}
