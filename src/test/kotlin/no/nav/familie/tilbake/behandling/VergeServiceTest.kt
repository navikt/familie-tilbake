package no.nav.familie.tilbake.behandling

import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.VergeDto
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class VergeServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private lateinit var vergeService: VergeService

    private val historikkTaskService: HistorikkTaskService = mockk(relaxed = true)

    private val vergeDto = VergeDto(orgNr = "987654321",
                                    type = Vergetype.ADVOKAT,
                                    navn = "Stor Herlig Straff",
                                    kilde = "",
                                    begrunnelse = "Det var nødvendig")

    @BeforeEach
    fun setUp() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
        vergeService = VergeService(behandlingRepository, historikkTaskService)
    }

    @Test
    fun `opprettVerge skal opprette verge i basen`() {
        vergeService.opprettVerge(Testdata.behandling.id, vergeDto)

        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val verge = behandling.aktivVerge!!
        assertThat(verge.aktiv).isEqualTo(true)
        assertThat(verge.orgNr).isEqualTo("987654321")
        assertThat(verge.type).isEqualTo(Vergetype.ADVOKAT)
        assertThat(verge.navn).isEqualTo("Stor Herlig Straff")
        assertThat(verge.kilde).isEqualTo("")
        assertThat(verge.begrunnelse).isEqualTo("Det var nødvendig")
    }

    @Test
    fun `opprettVerge skal deaktivere eksisterende verger i basen`() {
        val behandlingFørOppdatering = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val gammelVerge = behandlingFørOppdatering.aktivVerge!!

        vergeService.opprettVerge(Testdata.behandling.id, vergeDto)

        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val deaktivertVerge = behandling.verger.first { !it.aktiv }
        assertThat(deaktivertVerge.id).isEqualTo(gammelVerge.id)
    }

    @Test
    fun `opprettVerge skal kalle historikkTaskService for å opprette historikkTask`() {
        vergeService.opprettVerge(Testdata.behandling.id, vergeDto)

        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)

        verify {
            historikkTaskService.lagHistorikkTask(behandling.id,
                                                  TilbakekrevingHistorikkinnslagstype.VERGE_OPPRETTET,
                                                  Aktør.SAKSBEHANDLER)
        }
    }

    @Test
    fun `fjernVerge skal deaktivere verge i basen hvis det finnes aktiv verge`() {
        val behandlingFørOppdatering = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val gammelVerge = behandlingFørOppdatering.aktivVerge!!

        vergeService.fjernVerge(Testdata.behandling.id)

        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val deaktivertVerge = behandling.verger.first()
        assertThat(deaktivertVerge.id).isEqualTo(gammelVerge.id)
        assertThat(deaktivertVerge.aktiv).isEqualTo(false)
    }

    @Test
    fun `fjernVerge skal kalle historikkTaskService for å opprette historikkTask hvis verge har blitt deaktivert`() {
        val behandlingFørOppdatering = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val gammelVerge = behandlingFørOppdatering.aktivVerge
        assertThat(gammelVerge).isNotNull

        vergeService.fjernVerge(Testdata.behandling.id)

        verify {
            historikkTaskService.lagHistorikkTask(behandlingFørOppdatering.id,
                                                  TilbakekrevingHistorikkinnslagstype.VERGE_FJERNET,
                                                  Aktør.SAKSBEHANDLER)
        }

    }

    @Test
    fun `fjernVerge skal ikke gjøre noe hvis det finnes aktiv verge`() {
        val behandlingUtenVerge = behandlingRepository.insert(Testdata.behandling.copy(id = UUID.randomUUID(),
                                                                                       eksternBrukId = UUID.randomUUID(),
                                                                                       resultater = setOf(),
                                                                                       varsler = setOf(),
                                                                                       fagsystemsbehandling = setOf(),
                                                                                       verger = setOf()))

        vergeService.fjernVerge(behandlingUtenVerge.id)

        val behandlingEtterUtførtKall = behandlingRepository.findByIdOrThrow(behandlingUtenVerge.id)
        assertThat(behandlingUtenVerge).isEqualTo(behandlingEtterUtførtKall)
        verify(exactly = 0) { historikkTaskService.lagHistorikkTask(any(), any(), any()) }

    }

}