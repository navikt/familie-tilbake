package no.nav.familie.tilbake.service.dokumentbestilling.henleggelse

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.service.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.service.pdfgen.validering.PdfaValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class HenleggelsesbrevServiceTest : OppslagSpringRunnerTest() {

    private val eksterneDataForBrevService: EksterneDataForBrevService = mockk()

    private lateinit var henleggelsesbrevService: HenleggelsesbrevService
    private var behandlingId = Testdata.behandling.id

    @Autowired
    lateinit var pdfBrevService: PdfBrevService
    lateinit var spyPdfBrevService: PdfBrevService
    private val fagsakRepository: FagsakRepository = mockk()
    private val brevSporingRepository: BrevsporingRepository = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()

    @BeforeEach
    fun setup() {
        spyPdfBrevService = spyk(pdfBrevService)
        henleggelsesbrevService = HenleggelsesbrevService(behandlingRepository,
                                                          brevSporingRepository,
                                                          fagsakRepository,
                                                          eksterneDataForBrevService,
                                                          spyPdfBrevService)
        every { fagsakRepository.findByIdOrThrow(Testdata.fagsak.id) } returns Testdata.fagsak
        every { behandlingRepository.findByIdOrThrow(Testdata.behandling.id) } returns Testdata.behandling
        val personinfo = Personinfo("DUMMY_FØDSELSNUMMER", LocalDate.now(), "Fiona")
        val ident = Testdata.fagsak.bruker.ident
        every { eksterneDataForBrevService.hentPerson(ident, Fagsystem.BA) } returns personinfo
        every { eksterneDataForBrevService.hentAdresse(any(), any(), any<Verge>(), any()) }
                .returns(Adresseinfo("DUMMY_FØDSELSNUMMER", "Bob"))
        every { eksterneDataForBrevService.hentSaksbehandlernavn(any()) } returns "Siri Saksbehandler"
        every {
            brevSporingRepository.findFirstByBehandlingIdAndBrevtypeOrderBySporbarOpprettetTidDesc(behandlingId,
                                                                                                   Brevtype.VARSEL)
        } returns (Testdata.brevsporing)

    }

    @Test
    fun `sendHenleggelsebrev skal sende henleggelsesbrev`() {
        henleggelsesbrevService.sendHenleggelsebrev(behandlingId, null, Brevmottager.BRUKER)

        verify {
            spyPdfBrevService.sendBrev(Testdata.behandling,
                                       Testdata.fagsak,
                                       Brevtype.HENLEGGELSE,
                                       any(),
                                       any(),
                                       any())
        }
    }

    @Test
    fun `hentForhåndsvisningHenleggelsesbrev skal returnere pdf for henleggelsebrev`() {
        val bytes = henleggelsesbrevService.hentForhåndsvisningHenleggelsesbrev(behandlingId, null)

        PdfaValidator.validatePdf(bytes)
    }

    @Test
    fun `hentForhåndsvisningHenleggelsesbrev skal returnere pdf for henleggelsebrev for tilbakekreving revurdering`() {
        every { behandlingRepository.findByIdOrThrow(Testdata.behandling.id) }
                .returns(Testdata.behandling.copy(type = Behandlingstype.REVURDERING_TILBAKEKREVING))

        val bytes = henleggelsesbrevService.hentForhåndsvisningHenleggelsesbrev(behandlingId,
                                                                                REVURDERING_HENLEGGELSESBREV_FRITEKST)

        PdfaValidator.validatePdf(bytes)
    }

    @Test
    fun `sendHenleggelsebrev skal ikke sende henleggelsesbrev hvis varselbrev ikke sendt`() {
        every {
            brevSporingRepository.findFirstByBehandlingIdAndBrevtypeOrderBySporbarOpprettetTidDesc(behandlingId,
                                                                                                   Brevtype.VARSEL)
        } returns (null)

        val e = Assertions.assertThrows(IllegalStateException::class.java) {
            henleggelsesbrevService.sendHenleggelsebrev(behandlingId,
                                                        null,
                                                        Brevmottager.BRUKER)
        }

        assertThat(e.message).contains("varsel ikke er sendt")
    }

    @Test
    fun `sendHenleggelsebrev skal ikke sende henleggelsesbrev for tilbakekreving revurdering uten fritekst`() {
        every { behandlingRepository.findByIdOrThrow(Testdata.behandling.id) }
                .returns(Testdata.behandling.copy(type = Behandlingstype.REVURDERING_TILBAKEKREVING))

        val e = Assertions.assertThrows(IllegalStateException::class.java) {
            henleggelsesbrevService.sendHenleggelsebrev(behandlingId,
                                                        null,
                                                        Brevmottager.BRUKER)
        }

        assertThat(e.message).contains("henleggelsesbrev uten fritekst")
    }

    companion object {

        private const val REVURDERING_HENLEGGELSESBREV_FRITEKST = "Revurderingen ble henlagt"
    }
}
