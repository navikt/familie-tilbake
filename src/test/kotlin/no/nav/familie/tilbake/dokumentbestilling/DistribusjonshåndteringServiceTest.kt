package no.nav.familie.tilbake.dokumentbestilling

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.config.FeatureToggleConfig
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmetadataUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager.BRUKER
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager.VERGE
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingService
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.henleggelse.HenleggelsesbrevService
import no.nav.familie.tilbake.dokumentbestilling.henleggelse.SendHenleggelsesbrevTask

import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository

import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevgunnlagService
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional

class DistribusjonshåndteringServiceTest  {

    private val behandlingRepository: BehandlingRepository = mockk()
    private val fagsakRepository: FagsakRepository = mockk()
    private val manuelleBrevmottakerRepository: ManuellBrevmottakerRepository = mockk(relaxed = true)
    private val pdfBrevService: PdfBrevService = mockk(relaxed = true)
    private val featureToggleService: FeatureToggleService = mockk(relaxed = true)
    private val eksterneDataForBrevService: EksterneDataForBrevService = mockk()
    private val vedtaksbrevgrunnlagService: VedtaksbrevgunnlagService = mockk()

    private val brevmetadataUtil = BrevmetadataUtil(
        behandlingRepository = behandlingRepository,
        fagsakRepository = fagsakRepository,
        manuelleBrevmottakerRepository = manuelleBrevmottakerRepository,
        eksterneDataForBrevService = eksterneDataForBrevService,
        organisasjonService = mockk(),
        featureToggleService = featureToggleService,
    )
    private val distribusjonshåndteringService = DistribusjonshåndteringService(
        brevmetadataUtil = brevmetadataUtil,
        fagsakRepository = fagsakRepository,
        manuelleBrevmottakerRepository = manuelleBrevmottakerRepository,
        pdfBrevService = pdfBrevService,
        vedtaksbrevgrunnlagService = vedtaksbrevgrunnlagService,
        featureToggleService = featureToggleService
    )
    private val brevsporingService: BrevsporingService = mockk()
    private val henleggelsesbrevService = HenleggelsesbrevService(
        behandlingRepository = behandlingRepository,
        brevsporingService = brevsporingService,
        fagsakRepository = fagsakRepository,
        eksterneDataForBrevService = eksterneDataForBrevService,
        pdfBrevService = pdfBrevService,
        organisasjonService = mockk(),
        distribusjonshåndteringService = distribusjonshåndteringService,
        brevmetadataUtil = brevmetadataUtil
    )
    private val sendHenleggelsesbrevTask = SendHenleggelsesbrevTask(
        henleggelsesbrevService = henleggelsesbrevService,
        behandlingRepository = behandlingRepository,
        fagsakRepository = fagsakRepository,
        featureToggleService = featureToggleService
    )

    private val behandling = Testdata.behandling
    private val fagsak = Testdata.fagsak
    private val personinfoBruker = Personinfo(fagsak.bruker.ident, LocalDate.now(), navn = "brukernavn")
    private val brukerAdresse = Adresseinfo(personinfoBruker.ident, personinfoBruker.navn)
    private val verge = behandling.aktivVerge!!
    private val vergeAdresse = Adresseinfo(verge.ident!!, verge.navn)

    @BeforeEach
    fun setUp() {
        every { behandlingRepository.findById(any()) } returns Optional.of(behandling)
        every { fagsakRepository.findById(any()) } returns Optional.of(fagsak)
        every { eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem) } returns
                personinfoBruker
        every {
            eksterneDataForBrevService.hentAdresse(personinfoBruker, BRUKER, behandling.aktivVerge, any())
        } returns brukerAdresse
        every {
            eksterneDataForBrevService.hentAdresse(personinfoBruker, VERGE, behandling.aktivVerge, any())
        } returns vergeAdresse
        every { eksterneDataForBrevService.hentSaksbehandlernavn(any()) } returns behandling.ansvarligSaksbehandler
        every { eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(any()) } returns behandling.ansvarligSaksbehandler
        every { brevsporingService.finnSisteVarsel(any()) } returns Testdata.brevsporing
    }

    @Test
    fun `skal sende brev med likt innhold og til samme mottakere når toggle er på, som sendbrev-tasken når toggle er av`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.KONSOLIDERT_HÅNDTERING_AV_BREVMOTTAKERE) } returns
                true andThen false

        val task = SendHenleggelsesbrevTask.opprettTask(behandling.id, fagsak.fagsystem, "fritekst")
        val brevdata = mutableListOf<Brevdata>()

        sendHenleggelsesbrevTask.doTask(task)
        sendHenleggelsesbrevTask.doTask(task)

        verify(exactly = 4) {
            pdfBrevService.sendBrev(
                behandling,
                fagsak,
                Brevtype.HENLEGGELSE,
                capture(brevdata),
            )
        }

        brevdata shouldHaveSize 4

        brevdata.first { it.mottager == VERGE }.brevtekst shouldBeEqual
                brevdata.last { it.mottager == VERGE }.brevtekst
        brevdata.first { it.mottager == BRUKER }.brevtekst shouldBeEqual
                brevdata.last { it.mottager == BRUKER }.brevtekst

        brevdata.first { it.mottager == VERGE }.metadata shouldBeEqual
                brevdata.last { it.mottager == VERGE }.metadata
        brevdata.first { it.mottager == BRUKER }.metadata shouldBeEqual
                brevdata.last { it.mottager == BRUKER }.metadata
    }

    @Test
    fun `skal kun sende til bruker`() {
        val behandlingUtenVerge = behandling.copy(verger = emptySet())

        every { behandlingRepository.findById(any()) } returns Optional.of(behandlingUtenVerge)
        every {
            eksterneDataForBrevService.hentAdresse(personinfoBruker, BRUKER, behandlingUtenVerge.aktivVerge, any())
        } returns brukerAdresse

        every { featureToggleService.isEnabled(FeatureToggleConfig.KONSOLIDERT_HÅNDTERING_AV_BREVMOTTAKERE) } returns
                true andThen false

        val task = SendHenleggelsesbrevTask.opprettTask(behandling.id, fagsak.fagsystem, "fritekst")
        val brevdata = mutableListOf<Brevdata>()

        sendHenleggelsesbrevTask.doTask(task)
        sendHenleggelsesbrevTask.doTask(task)

        verify {
            pdfBrevService.sendBrev(
                behandlingUtenVerge,
                fagsak,
                Brevtype.HENLEGGELSE,
                capture(brevdata),
            )
        }

        brevdata shouldHaveSize 2

        brevdata.first().brevtekst shouldBeEqual brevdata.last().brevtekst
        brevdata.first().metadata shouldBeEqual brevdata.last().metadata
    }
}