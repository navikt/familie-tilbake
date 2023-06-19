package no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.FeatureToggleConfig
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.DistribusjonshåndteringService
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmetadataUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.organisasjon.OrganisasjonService
import no.nav.familie.tilbake.pdfgen.validering.PdfaValidator
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
    private val featureToggleService: FeatureToggleService = mockk(relaxed = true)
    private val brevmetadataUtil = BrevmetadataUtil(
        behandlingRepository = behandlingRepository,
        fagsakRepository = fagsakRepository,
        manuelleBrevmottakerRepository = mockk(relaxed = true),
        eksterneDataForBrevService = mockEksterneDataForBrevService,
        organisasjonService = organisasjonService,
        featureToggleService = featureToggleService
    )

    @BeforeEach
    fun setup() {
        spyPdfBrevService = spyk(pdfBrevService)
        innhentDokumentasjonBrevService = InnhentDokumentasjonbrevService(
            fagsakRepository,
            behandlingRepository,
            mockEksterneDataForBrevService,
            spyPdfBrevService,
            organisasjonService,
            distribusjonshåndteringService,
            brevmetadataUtil
        )
        every { fagsakRepository.findByIdOrThrow(Testdata.fagsak.id) } returns Testdata.fagsak
        every { behandlingRepository.findByIdOrThrow(Testdata.behandling.id) } returns Testdata.behandling
        val personinfo = Personinfo("DUMMY_FØDSELSNUMMER", LocalDate.now(), "Fiona")
        val ident = Testdata.fagsak.bruker.ident
        every { mockEksterneDataForBrevService.hentPerson(ident, Fagsystem.BA) } returns personinfo
        every { mockEksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(any()) } returns "Siri Saksbehandler"
        every { mockEksterneDataForBrevService.hentAdresse(any(), any(), any<Verge>(), any()) }
            .returns(Adresseinfo("DUMMY_FØDSELSNUMMER", "Bob"))
    }

    @Test
    fun `hentForhåndsvisningInnhentDokumentasjonBrev returnere pdf for innhent dokumentasjonbrev`() {
        val data = innhentDokumentasjonBrevService.hentForhåndsvisningInnhentDokumentasjonBrev(
            Testdata.behandling.id,
            flereOpplysninger
        )

        PdfaValidator.validatePdf(data)
    }

    @Test
    fun `brevmetadataUtil skal lage lik metadata som InnhentDokumentasjonbrevService selv`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.KONSOLIDERT_HÅNDTERING_AV_BREVMOTTAKERE) } returns
            true andThen false

        val brevdata = mutableListOf<Brevdata>()

        innhentDokumentasjonBrevService.hentForhåndsvisningInnhentDokumentasjonBrev(
            Testdata.behandling.id,
            flereOpplysninger
        )
        innhentDokumentasjonBrevService.hentForhåndsvisningInnhentDokumentasjonBrev(
            Testdata.behandling.id,
            flereOpplysninger
        )

        verify(exactly = 2) {
            spyPdfBrevService.genererForhåndsvisning(
                capture(brevdata)
            )
        }

        brevdata shouldHaveSize 2
        brevdata.first().metadata.copy(annenMottakersNavn = null) shouldBeEqualToComparingFields
            brevdata.last().metadata // gammel flyt setter ikke annenMottakersNavn i metadata. Utledes lokalt for hvert brev
        brevdata.first().brevtekst shouldBeEqual brevdata.last().brevtekst
    }
}
