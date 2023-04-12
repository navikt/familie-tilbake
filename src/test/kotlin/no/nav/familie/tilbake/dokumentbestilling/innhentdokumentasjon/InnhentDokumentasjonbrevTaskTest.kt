package no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.FeatureToggleConfig
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.person.PersonService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class InnhentDokumentasjonbrevTaskTest() {

    private val behandlingskontrollService: BehandlingskontrollService = mockk()
    private val oppgaveTaskService: OppgaveTaskService = mockk()
    private val personService: PersonService = mockk()
    private val integrasjonerClient: IntegrasjonerClient = mockk()
    private val manuellBrevmottakerService: ManuellBrevmottakerService = mockk()
    private val fagsakRepository: FagsakRepository = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()
    private val pdfPrevService: PdfBrevService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()

    private val eksterneDataForBrevService =
        EksterneDataForBrevService(personService, integrasjonerClient, featureToggleService, manuellBrevmottakerService)

    private val innhentDokumentasjonBrevService = InnhentDokumentasjonbrevService(
        fagsakRepository,
        behandlingRepository,
        eksterneDataForBrevService,
        pdfPrevService,
        mockk()
    )

    private val innhentDokumentasjonbrevTask: InnhentDokumentasjonbrevTask = InnhentDokumentasjonbrevTask(
        behandlingRepository,
        innhentDokumentasjonBrevService,
        behandlingskontrollService,
        oppgaveTaskService,
        fagsakRepository
    )

    private val ident = "DUMMY_BRUKERIDENT"
    private val brukerNavn = "Brukernavn"
    private val vergeIdent = "DUMMY_VERGEIDENT"
    private val vergeNavn = "Vergenavn"
    private val personinfo = Personinfo(ident, LocalDate.now(), brukerNavn)
    private val vergePersonInfo = Personinfo(vergeIdent, LocalDate.now(), vergeNavn)
    val orgNr = "DUMMY_ORGNR"
    val advokatfirma = "Advokatsen AS"

    private val manuellVerge = ManuellBrevmottaker(
        behandlingId = Testdata.behandling.id,
        type = MottakerType.VERGE,
        vergetype = Vergetype.VERGE_FOR_BARN,
        navn = "Manuelt Vergenavn",
        ident = "DUMMY_MANUELLVERGEIDENT"
    )

    private val brevdata = mutableListOf<Brevdata>()

    @BeforeAll
    fun setup() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.DSITRIBUER_TIL_MANUELLE_BREVMOTTAKERE) } returns true

        every { fagsakRepository.findByIdOrThrow(Testdata.fagsak.id) } returns Testdata.fagsak.copy(
            bruker = Testdata.fagsak.bruker.copy(
                ident = ident
            )
        )
        every { behandlingRepository.findByIdOrThrow(Testdata.behandling.id) } returns Testdata.behandling.copy(
            verger = setOf(Testdata.behandling.verger.first().copy(ident = vergeIdent, navn = vergeNavn))
        )
        every { oppgaveTaskService.oppdaterOppgaveTask(any(), any(), any(), any()) } returns Unit
        every { pdfPrevService.sendBrev(any(), any(), any(), capture(brevdata), any(), any()) } returns Unit

        every { personService.hentPersoninfo(ident, any()) } returns personinfo
        every { personService.hentPersoninfo(vergeIdent, any()) } returns vergePersonInfo
        every { personService.hentPersoninfo(manuellVerge.ident!!, any()) } returns Personinfo(
            manuellVerge.ident!!,
            LocalDate.now(),
            manuellVerge.navn
        )

        every { integrasjonerClient.hentOrganisasjon(orgNr) } returns Organisasjon(orgNr, advokatfirma)

        every { integrasjonerClient.hentSaksbehandler(any()) } returns Saksbehandler(
            azureId = UUID.randomUUID(),
            navIdent = "01987654321",
            fornavn = "Siri",
            etternavn = "Saksbehandler"
        )
    }

    @BeforeEach
    fun clearSlot() {
        brevdata.clear()
    }

    @Test
    fun `task kaller pdfBrevService to ganger uten manuelle mottakere`() {
        val fagsystem = fagsakRepository.findByIdOrThrow(Testdata.behandling.fagsakId).fagsystem
        val task = InnhentDokumentasjonbrevTask.opprettTask(Testdata.behandling.id, fagsystem, "fritekst")

        every { manuellBrevmottakerService.hentBrevmottakere(any()) } returns emptyList()

        innhentDokumentasjonbrevTask.doTask(task)

        brevdata shouldHaveSize 2

        val brukerBrev = brevdata.find { it.mottager == Brevmottager.BRUKER }
        assertBrevIsCorrect(brukerBrev = brukerBrev, mottagerNavn = brukerNavn, annenMottagerNavn = vergeNavn)

        val vergeBrev = brevdata.find { it.mottager == Brevmottager.VERGE }
        assertBrevIsCorrect(brukerBrev = vergeBrev, mottagerNavn = vergeNavn, annenMottagerNavn = brukerNavn)
    }

    @Test
    fun `manuell verge blir satt som verge`() {
        val fagsystem = fagsakRepository.findByIdOrThrow(Testdata.behandling.fagsakId).fagsystem

        val task = InnhentDokumentasjonbrevTask.opprettTask(Testdata.behandling.id, fagsystem, "fritekst")

        val manueltBrukernavn = "ManueltBrukernavn"
        every { manuellBrevmottakerService.hentBrevmottakere(any()) } returns listOf(
            manuellVerge,
            manuellVerge.copy(id = UUID.randomUUID(), type = MottakerType.FULLMEKTIG, navn = manueltBrukernavn)
        )

        innhentDokumentasjonbrevTask.doTask(task)

        brevdata shouldHaveSize 2

        val brukerBrev = brevdata.filter { it.mottager == Brevmottager.BRUKER }.singleOrNull()
        assertBrevIsCorrect(brukerBrev, manueltBrukernavn, manuellVerge.navn)

        val vergeBrev = brevdata.filter { it.mottager == Brevmottager.VERGE }.singleOrNull()
        assertBrevIsCorrect(vergeBrev, manuellVerge.navn, manueltBrukernavn)
    }

    @Test
    fun `manuell verge uten ident blir satt som verge`() {
        val fagsystem = fagsakRepository.findByIdOrThrow(Testdata.behandling.fagsakId).fagsystem
        val task = InnhentDokumentasjonbrevTask.opprettTask(Testdata.behandling.id, fagsystem, "fangettekst")

        every { manuellBrevmottakerService.hentBrevmottakere(any()) } returns listOf(
            manuellVerge.copy(
                ident = null,
                adresselinje1 = "Mockgata 1",
                postnummer = "0000",
                poststed = "OSLO",
                landkode = "NO"
            )
        )

        innhentDokumentasjonbrevTask.doTask(task)

        brevdata shouldHaveSize 2

        val brukerBrev = brevdata.find { it.mottager == Brevmottager.BRUKER }
        assertBrevIsCorrect(brukerBrev, brukerNavn, manuellVerge.navn)

        val vergeBrev = brevdata.find { it.mottager == Brevmottager.VERGE }
        assertBrevIsCorrect(vergeBrev, manuellVerge.navn, brukerNavn)
    }

    @Test
    fun `manuell advokatmottaker blir satt som mottaker`() {
        val fagsystem = fagsakRepository.findByIdOrThrow(Testdata.behandling.fagsakId).fagsystem
        val task = InnhentDokumentasjonbrevTask.opprettTask(Testdata.behandling.id, fagsystem, "fangettekst")

        every { manuellBrevmottakerService.hentBrevmottakere(any()) } returns listOf(
            manuellVerge.copy(vergetype = Vergetype.ADVOKAT, orgNr = orgNr)
        )

        innhentDokumentasjonbrevTask.doTask(task)

        brevdata shouldHaveSize 2

        val brukerBrev = brevdata.find { it.mottager == Brevmottager.BRUKER }
        assertBrevIsCorrect(brukerBrev, brukerNavn, "$advokatfirma v/ ${manuellVerge.navn}")

        val vergeBrev = brevdata.find { it.mottager == Brevmottager.VERGE }
        assertBrevIsCorrect(vergeBrev, "$advokatfirma v/ ${manuellVerge.navn}", brukerNavn)
    }

    private fun assertBrevIsCorrect(
        brukerBrev: Brevdata?,
        mottagerNavn: String,
        annenMottagerNavn: String? = null
    ) {
        brukerBrev shouldNotBe null
        val mottagerAdresse = brukerBrev!!.metadata.mottageradresse
        mottagerAdresse.mottagernavn shouldBe mottagerNavn
        mottagerAdresse.annenMottagersNavn shouldBe annenMottagerNavn
        brukerBrev.brevtekst.lineSequence().last() shouldBe "Brev med likt innhold er sendt til $annenMottagerNavn"
    }
}
