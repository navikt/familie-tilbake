package no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.tilbakekreving.ManuellAdresseInfo
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType.DØDSBO
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.ManuellBrevmottakerRequestDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class ManuellBrevmottakerServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var manuellBrevmottakerRepository: ManuellBrevmottakerRepository
    private val mockHistorikkService: HistorikkService = mockk(relaxed = true)
    private val mockBehandlingRepository: BehandlingRepository = mockk(relaxed = true)
    private val mockBehandlingskontrollService: BehandlingskontrollService = mockk(relaxed = true)

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    private lateinit var behandling: Behandling
    private lateinit var manuellBrevmottakerService: ManuellBrevmottakerService
    private val opprettetTidspunktSlot = mutableListOf<LocalDateTime>()

    private val manuellBrevmottakerRequestDto = ManuellBrevmottakerRequestDto(
        type = DØDSBO,
        navn = "John Doe",
        manuellAdresseInfo = ManuellAdresseInfo(
            adresselinje1 = "test adresse1",
            adresselinje2 = "test adresse2",
            postnummer = "0000",
            poststed = "Oslo",
            landkode = "NO"
        )
    )

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandling = behandlingRepository.insert(Testdata.behandling)

        manuellBrevmottakerService = ManuellBrevmottakerService(manuellBrevmottakerRepository, mockHistorikkService, mockBehandlingRepository, mockBehandlingskontrollService)

        every {
            mockHistorikkService.lagHistorikkinnslag(
                behandlingId = any(),
                historikkinnslagstype = any(),
                aktør = any(),
                opprettetTidspunkt = capture(opprettetTidspunktSlot)
            )
        } just runs
    }

    @AfterEach
    fun clearSlot() {
        opprettetTidspunktSlot.clear()
    }

    @Test
    fun `leggTilBrevmottaker skal legge til brevmottakere og oppdatere med oppdaterBrevmottaker`() {
        shouldNotThrow<RuntimeException> {
            manuellBrevmottakerService.leggTilBrevmottaker(behandling.id, manuellBrevmottakerRequestDto)
        }
        shouldNotThrow<RuntimeException> {
            manuellBrevmottakerService.leggTilBrevmottaker(
                behandling.id,
                manuellBrevmottakerRequestDto.copy(
                    navn = "Kari Nordmann",
                    manuellAdresseInfo = null,
                    personIdent = "12345678910"
                )
            )
        }

        var manuellBrevmottakere = manuellBrevmottakerService.hentBrevmottakere(behandling.id)

        manuellBrevmottakere.shouldHaveSize(2)
        val dbManuellBrevmottaker = manuellBrevmottakere.filter { it.navn.equals("John Doe") }.first()
        assertEqualsManuellBrevmottaker(dbManuellBrevmottaker, manuellBrevmottakerRequestDto)

        verify(exactly = 2) {
            mockHistorikkService.lagHistorikkinnslag(
                behandlingId = behandling.id,
                historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_LAGT_TIL,
                aktør = Aktør.SAKSBEHANDLER,
                opprettetTidspunkt = or(opprettetTidspunktSlot[0], opprettetTidspunktSlot[1])
            )
        }

        val oppdatertManuellBrevmottaker = manuellBrevmottakerRequestDto.copy(
            manuellAdresseInfo = ManuellAdresseInfo(
                adresselinje1 = "ny",
                postnummer = "1111",
                poststed = "stavanger",
                landkode = "NO"
            )
        )
        shouldNotThrow<RuntimeException> {
            manuellBrevmottakerService.oppdaterBrevmottaker(
                dbManuellBrevmottaker.id,
                oppdatertManuellBrevmottaker
            )
        }

        manuellBrevmottakere = manuellBrevmottakerService.hentBrevmottakere(behandling.id)
        manuellBrevmottakere.shouldHaveSize(2)
        val dbOppdatertManuellBrevmottaker = manuellBrevmottakere.filter { it.navn.equals("John Doe") }.first()
        assertEqualsManuellBrevmottaker(dbOppdatertManuellBrevmottaker, oppdatertManuellBrevmottaker)
    }

    @Test
    fun `fjernBrevmottaker fjerner brevmottaker`() {
        shouldNotThrow<RuntimeException> {
            manuellBrevmottakerService.leggTilBrevmottaker(behandling.id, manuellBrevmottakerRequestDto)
        }
        shouldNotThrow<RuntimeException> {
            manuellBrevmottakerService.leggTilBrevmottaker(
                behandling.id,
                manuellBrevmottakerRequestDto.copy(navn = "Kari Nordmann")
            )
        }

        val manuellBrevmottakere = manuellBrevmottakerService.hentBrevmottakere(behandling.id)

        manuellBrevmottakere.shouldHaveSize(2)
        val dbManuellBrevmottaker = manuellBrevmottakere.filter { it.navn.equals("John Doe") }.first()
        assertEqualsManuellBrevmottaker(dbManuellBrevmottaker, manuellBrevmottakerRequestDto)

        verify(exactly = 2) {
            mockHistorikkService.lagHistorikkinnslag(
                behandlingId = behandling.id,
                historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_LAGT_TIL,
                aktør = Aktør.SAKSBEHANDLER,
                opprettetTidspunkt = or(opprettetTidspunktSlot[0], opprettetTidspunktSlot[1])
            )
        }

        shouldNotThrow<RuntimeException> {
            manuellBrevmottakerService.fjernBrevmottaker(behandling.id, dbManuellBrevmottaker.id)
        }

        manuellBrevmottakerService.hentBrevmottakere(behandling.id).filter { it.navn.equals("John Doe") }
            .shouldBeEmpty()

        verify(exactly = 1) {
            mockHistorikkService.lagHistorikkinnslag(
                behandlingId = behandling.id,
                historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_FJERNET,
                aktør = Aktør.SAKSBEHANDLER,
                opprettetTidspunkt = opprettetTidspunktSlot[2]
            )
        }
    }

    private fun assertEqualsManuellBrevmottaker(a: ManuellBrevmottaker, b: ManuellBrevmottakerRequestDto) {
        a.id.shouldNotBeNull()
        a.orgNr shouldBe b.organisasjonsnummer
        a.ident shouldBe b.personIdent
        a.type shouldBe b.type
        a.vergetype shouldBe b.vergetype
        a.adresselinje1 shouldBe b.manuellAdresseInfo?.adresselinje1
        a.adresselinje2 shouldBe b.manuellAdresseInfo?.adresselinje2
        a.postnummer shouldBe b.manuellAdresseInfo?.postnummer
        a.poststed shouldBe b.manuellAdresseInfo?.poststed
        a.landkode shouldBe b.manuellAdresseInfo?.landkode
    }
}
