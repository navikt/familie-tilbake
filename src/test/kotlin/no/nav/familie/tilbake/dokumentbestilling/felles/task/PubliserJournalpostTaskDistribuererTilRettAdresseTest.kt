package no.nav.familie.tilbake.dokumentbestilling.felles.task

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstidspunkt
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.dokdist.ManuellAdresse
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.config.FeatureToggleConfig.Companion.DSITRIBUER_TIL_MANUELLE_BREVMOTTAKERE
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.Properties
import java.util.UUID

class PubliserJournalpostTaskDistribuererTilRettAdresseTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var manuellBrevmottakerRepository: ManuellBrevmottakerRepository

    @Autowired
    private lateinit var manuellBrevmottakerService: ManuellBrevmottakerService

    private val featureToggleService: FeatureToggleService = mockk(relaxed = true)

    @Autowired
    private lateinit var integrasjonerClient: IntegrasjonerClient

    private lateinit var publiserJournalpostTask: PubliserJournalpostTask

    private lateinit var behandlingId: UUID
    private val manuellAdresse = slot<ManuellAdresse>()

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingId = behandlingRepository.insert(Testdata.behandling).id

        publiserJournalpostTask =
            PubliserJournalpostTask(integrasjonerClient, manuellBrevmottakerService, featureToggleService, taskService)

        every {
            featureToggleService.isEnabled(DSITRIBUER_TIL_MANUELLE_BREVMOTTAKERE)
        } returns true

        every {
            integrasjonerClient.distribuerJournalpost(
                journalpostId = any(),
                fagsystem = any(),
                distribusjonstype = any(),
                distribusjonstidspunkt = any(),
                manuellAdresse = capture(manuellAdresse)
            )
        } returns "hei"
    }

    @Test
    fun `skal også sende til manuell adresse`() {
        val expectedManuellBrevmottaker = manuellBrevmottakerRepository.insert(
            ManuellBrevmottaker(
                behandlingId = behandlingId,
                type = MottakerType.FULLMEKTIG,
                navn = "Navn Navnesen",
                adresselinje1 = "Gate gatesen 2",
                postnummer = "0000",
                poststed = "OSLO",
                landkode = "NO"
            )
        )

        val task = opprettTask("tilfeldigId", behandlingId)

        publiserJournalpostTask.doTask(task)

        val actualAdresse = manuellAdresse.captured

        assertEqualsManuellAddresseOgBrevmottaker(actualAdresse, expectedManuellBrevmottaker)
    }

    private fun assertEqualsManuellAddresseOgBrevmottaker(
        actualAdresse: ManuellAdresse,
        expectedManuellBrevmottaker: ManuellBrevmottaker
    ) {
        actualAdresse.adresselinje1 shouldBe expectedManuellBrevmottaker.adresselinje1
        actualAdresse.adresselinje2 shouldBe expectedManuellBrevmottaker.adresselinje2
        actualAdresse.postnummer shouldBe expectedManuellBrevmottaker.postnummer
        actualAdresse.poststed shouldBe expectedManuellBrevmottaker.poststed
        actualAdresse.land shouldBe expectedManuellBrevmottaker.landkode
    }

    private fun opprettTask(journalpostId: String, behandlingId: UUID): Task {
        return Task(
            type = PubliserJournalpostTask.TYPE,
            payload = behandlingId.toString(),
            properties = Properties().apply {
                this["journalpostId"] = journalpostId
                this["fagsystem"] = Fagsystem.BA.name
                this["distribusjonstype"] = Distribusjonstype.VIKTIG.name
                this["distribusjonstidspunkt"] = Distribusjonstidspunkt.KJERNETID.name
            }
        )
    }
}
