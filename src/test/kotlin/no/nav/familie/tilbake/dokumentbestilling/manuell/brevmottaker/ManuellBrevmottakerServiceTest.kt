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
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.ManuellBrevmottakerDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.MottakerType
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class ManuellBrevmottakerServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var manuellBrevmottakerRepository: ManuellBrevmottakerRepository
    private val mockHistorikkService: HistorikkService = mockk(relaxed = true)

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    private lateinit var behandling: Behandling
    private lateinit var manuellBrevmottakerService: ManuellBrevmottakerService
    private val opprettetTidspunktSlot = slot<LocalDateTime>()

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandling = behandlingRepository.insert(Testdata.behandling)

        manuellBrevmottakerService = ManuellBrevmottakerService(manuellBrevmottakerRepository, mockHistorikkService)

        every {
            mockHistorikkService.lagHistorikkinnslag(
                behandlingId = any(),
                historikkinnslagstype = any(),
                aktør = any(),
                opprettetTidspunkt = capture(opprettetTidspunktSlot)
            )
        } just runs
    }

    @Test
    fun `leggTilBrevmottaker skal legge til FULLMEKTIG brevmottakere og hente det med hentBrevmottakere`() {
        val manuellBrevmottakerDto = ManuellBrevmottakerDto(
            type = MottakerType.FULLMEKTIG,
            navn = "John Doe",
            adresselinje1 = "test adresse1",
            adresselinje2 = "test adresse2",
            postnummer = "0000",
            poststed = "Oslo",
            landkode = "NO"
        )
        shouldNotThrow<RuntimeException> {
            manuellBrevmottakerService.leggTilBrevmottaker(behandling.id, manuellBrevmottakerDto)
        }

        val manuellBrevMottakere = manuellBrevmottakerService.hentBrevmottakere(behandling.id)
        manuellBrevMottakere.shouldHaveSize(1)
        manuellBrevMottakere.first().adresselinje1 shouldBe manuellBrevmottakerDto.adresselinje1
        manuellBrevMottakere.first().adresselinje2 shouldBe manuellBrevmottakerDto.adresselinje2
        manuellBrevMottakere.first().postnummer shouldBe manuellBrevmottakerDto.postnummer
        manuellBrevMottakere.first().poststed shouldBe manuellBrevmottakerDto.poststed
        manuellBrevMottakere.first().landkode shouldBe manuellBrevmottakerDto.landkode

        verify(exactly = 1) {
            mockHistorikkService.lagHistorikkinnslag(
                behandlingId = behandling.id,
                historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_LAGT_TIL,
                aktør = Aktør.SAKSBEHANDLER,
                opprettetTidspunkt = opprettetTidspunktSlot.captured
            )
        }
    }

    @Test
    fun `leggTilBrevmottaker skal legge til DØDSBO brevmottakere, oppdatere med oppdaterBrevmottaker og fjerne med fjernBrevmottaker`() {
        val manuellBrevmottakerDto = ManuellBrevmottakerDto(
            type = MottakerType.DØDSBO,
            navn = "John Doe",
            adresselinje1 = "test adresse1",
            adresselinje2 = "test adresse2",
            postnummer = "0000",
            poststed = "Oslo",
            landkode = "NO"
        )

        shouldNotThrow<RuntimeException> {
            manuellBrevmottakerService.leggTilBrevmottaker(behandling.id, manuellBrevmottakerDto)
        }

        var manuellBrevmottakere = manuellBrevmottakerService.hentBrevmottakere(behandling.id)
        manuellBrevmottakere.shouldHaveSize(1)

        var manuellBrevmottaker = manuellBrevmottakere.first()
        manuellBrevmottaker.id.shouldNotBeNull()
        manuellBrevmottaker.adresselinje1 shouldBe manuellBrevmottakerDto.adresselinje1
        manuellBrevmottaker.adresselinje2 shouldBe manuellBrevmottakerDto.adresselinje2
        manuellBrevmottaker.postnummer shouldBe manuellBrevmottakerDto.postnummer
        manuellBrevmottaker.poststed shouldBe manuellBrevmottakerDto.poststed
        manuellBrevmottaker.landkode shouldBe manuellBrevmottakerDto.landkode

        verify(exactly = 1) {
            mockHistorikkService.lagHistorikkinnslag(
                behandlingId = behandling.id,
                historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_LAGT_TIL,
                aktør = Aktør.SAKSBEHANDLER,
                opprettetTidspunkt = opprettetTidspunktSlot.captured
            )
        }

        shouldNotThrow<RuntimeException> {
            manuellBrevmottakerService.oppdaterBrevmottaker(
                behandling.id,
                manuellBrevmottaker.id!!,
                manuellBrevmottakerDto.copy(adresselinje1 = "ny", postnummer = "1111", poststed = "stavanger")
            )
        }

        manuellBrevmottakere = manuellBrevmottakerService.hentBrevmottakere(behandling.id)
        manuellBrevmottakere.shouldHaveSize(1)
        manuellBrevmottaker = manuellBrevmottakere.first()

        manuellBrevmottaker.id.shouldNotBeNull()
        manuellBrevmottaker.adresselinje1 shouldBe "ny"
        manuellBrevmottaker.postnummer shouldBe "1111"
        manuellBrevmottaker.poststed shouldBe "stavanger"

        shouldNotThrow<RuntimeException> {
            manuellBrevmottakerService.fjernBrevmottaker(behandling.id, manuellBrevmottaker.id!!)
        }

        manuellBrevmottakerService.hentBrevmottakere(behandling.id).shouldBeEmpty()

        verify(exactly = 1) {
            mockHistorikkService.lagHistorikkinnslag(
                behandlingId = behandling.id,
                historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_FJERNET,
                aktør = Aktør.SAKSBEHANDLER,
                opprettetTidspunkt = opprettetTidspunktSlot.captured
            )
        }
    }
}
