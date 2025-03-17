package no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.DistribusjonshåndteringService
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmetadataUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.organisasjon.OrganisasjonService
import no.nav.familie.tilbake.pdfgen.validering.PdfaValidator
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class InnhentDokumentasjonbrevServiceTest : OppslagSpringRunnerTest() {
    private val flereOpplysninger = "Vi trenger flere opplysninger"
    private val mockEksterneDataForBrevService: EksterneDataForBrevService = mockk()

    @Autowired
    lateinit var pdfBrevService: PdfBrevService
    lateinit var spyPdfBrevService: PdfBrevService

    @Autowired
    lateinit var distribusjonshåndteringService: DistribusjonshåndteringService
    private val fagsakRepository: FagsakRepository = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()
    private lateinit var innhentDokumentasjonBrevService: InnhentDokumentasjonbrevService
    private val organisasjonService: OrganisasjonService = mockk()
    private val brevmetadataUtil =
        BrevmetadataUtil(
            behandlingRepository = behandlingRepository,
            fagsakRepository = fagsakRepository,
            manuelleBrevmottakerRepository = mockk(relaxed = true),
            eksterneDataForBrevService = mockEksterneDataForBrevService,
            organisasjonService = organisasjonService,
        )

    lateinit var behandling: Behandling

    @BeforeEach
    fun setup() {
        spyPdfBrevService = spyk(pdfBrevService)
        innhentDokumentasjonBrevService =
            InnhentDokumentasjonbrevService(
                fagsakRepository,
                behandlingRepository,
                mockEksterneDataForBrevService,
                spyPdfBrevService,
                organisasjonService,
                distribusjonshåndteringService,
                brevmetadataUtil,
            )
        behandling = Testdata.lagBehandling()
        every { fagsakRepository.findByIdOrThrow(Testdata.fagsak.id) } returns Testdata.fagsak
        every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling
        val personinfo = Personinfo("DUMMY_FØDSELSNUMMER", LocalDate.now(), "Fiona")
        val ident = Testdata.fagsak.bruker.ident
        every { mockEksterneDataForBrevService.hentPerson(ident, Fagsystem.BA, any()) } returns personinfo
        every { mockEksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(any(), any()) } returns "Siri Saksbehandler"
        every { mockEksterneDataForBrevService.hentAdresse(any(), any(), any<Verge>(), any(), any()) }
            .returns(Adresseinfo("DUMMY_FØDSELSNUMMER", "Bob"))
    }

    @Test
    fun `hentForhåndsvisningInnhentDokumentasjonBrev returnere pdf for innhent dokumentasjonbrev`() {
        val data =
            innhentDokumentasjonBrevService.hentForhåndsvisningInnhentDokumentasjonBrev(
                behandling.id,
                flereOpplysninger,
            )

        PdfaValidator.validatePdf(data)
    }
}
