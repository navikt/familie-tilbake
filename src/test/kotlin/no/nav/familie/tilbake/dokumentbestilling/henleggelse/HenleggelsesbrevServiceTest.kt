package no.nav.familie.tilbake.dokumentbestilling.henleggelse

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.DistribusjonshåndteringService
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmetadataUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingService
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.organisasjon.OrganisasjonService
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Adresseinfo
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmottager
import no.nav.tilbakekreving.pdf.validering.PdfaValidator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class HenleggelsesbrevServiceTest : OppslagSpringRunnerTest() {
    override val tømDBEtterHverTest = false
    private val eksterneDataForBrevService: EksterneDataForBrevService = mockk()

    private lateinit var henleggelsesbrevService: HenleggelsesbrevService
    private lateinit var behandling: Behandling
    private lateinit var fagsak: Fagsak

    @Autowired
    lateinit var pdfBrevService: PdfBrevService
    lateinit var spyPdfBrevService: PdfBrevService
    private val fagsakRepository: FagsakRepository = mockk()
    private val brevsporingService: BrevsporingService = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()
    private val organisasjonService: OrganisasjonService = mockk()
    private val distribusjonshåndteringService: DistribusjonshåndteringService = mockk()
    private val featureToggleService: FeatureToggleService = mockk(relaxed = true)

    private val brevmetadataUtil =
        BrevmetadataUtil(
            behandlingRepository = behandlingRepository,
            fagsakRepository = fagsakRepository,
            manuelleBrevmottakerRepository = mockk(relaxed = true),
            eksterneDataForBrevService = eksterneDataForBrevService,
            organisasjonService = organisasjonService,
        )

    @BeforeEach
    fun setup() {
        spyPdfBrevService = spyk(pdfBrevService)
        henleggelsesbrevService =
            HenleggelsesbrevService(
                behandlingRepository,
                brevsporingService,
                fagsakRepository,
                eksterneDataForBrevService,
                spyPdfBrevService,
                organisasjonService,
                distribusjonshåndteringService,
                brevmetadataUtil,
            )
        fagsak = Testdata.fagsak()
        behandling = Testdata.lagBehandling(fagsakId = fagsak.id)
        every { fagsakRepository.findByIdOrThrow(fagsak.id) } returns fagsak
        every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling
        val personinfo = Personinfo("DUMMY_FNR_1", LocalDate.now(), "Fiona")
        val ident = fagsak.bruker.ident
        every { eksterneDataForBrevService.hentPerson(ident, Fagsystem.BA, any()) } returns personinfo
        every { eksterneDataForBrevService.hentAdresse(any(), any(), any<Verge>(), any(), any()) }
            .returns(Adresseinfo("DUMMY_FNR_2", "Bob"))
        every { eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(any(), any()) } returns "Siri Saksbehandler"
        every {
            brevsporingService.finnSisteVarsel(behandling.id)
        } returns (Testdata.lagBrevsporing(behandling.id))
    }

    @Test
    fun `sendHenleggelsebrev skal sende henleggelsesbrev`() {
        henleggelsesbrevService.sendHenleggelsebrev(behandling.id, null, Brevmottager.BRUKER)

        verify {
            spyPdfBrevService.sendBrev(
                behandling,
                fagsak,
                Brevtype.HENLEGGELSE,
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `hentForhåndsvisningHenleggelsesbrev skal returnere pdf for henleggelsebrev`() {
        val bytes = henleggelsesbrevService.hentForhåndsvisningHenleggelsesbrev(behandling.id, null)

        PdfaValidator.validatePdf(bytes)
    }

    @Test
    fun `hentForhåndsvisningHenleggelsesbrev skal returnere pdf for henleggelsebrev for tilbakekreving revurdering`() {
        every { behandlingRepository.findByIdOrThrow(behandling.id) }
            .returns(behandling.copy(type = Behandlingstype.REVURDERING_TILBAKEKREVING))

        val bytes =
            henleggelsesbrevService.hentForhåndsvisningHenleggelsesbrev(
                behandling.id,
                REVURDERING_HENLEGGELSESBREV_FRITEKST,
            )

        PdfaValidator.validatePdf(bytes)
    }

    @Test
    fun `sendHenleggelsebrev skal ikke sende henleggelsesbrev hvis varselbrev ikke sendt`() {
        every {
            brevsporingService.finnSisteVarsel(behandling.id)
        } returns (null)

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
        every { behandlingRepository.findByIdOrThrow(behandling.id) }
            .returns(behandling.copy(type = Behandlingstype.REVURDERING_TILBAKEKREVING))

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
