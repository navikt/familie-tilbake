package no.nav.familie.tilbake.behandling

import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Applikasjon
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.VergeDto
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class VergeServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    private lateinit var vergeService: VergeService

    private val historikkTaskService: HistorikkTaskService = mockk(relaxed = true)

    private val vergeDto = VergeDto(orgNr = "987654321",
                                    type = Vergetype.ADVOKAT,
                                    navn = "Stor Herlig Straff",
                                    begrunnelse = "Det var nødvendig")

    @BeforeEach
    fun setUp() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
        vergeService = VergeService(behandlingRepository, historikkTaskService, behandlingskontrollService)
    }

    @Test
    fun `lagreVerge skal lagre verge i basen`() {
        vergeService.lagreVerge(Testdata.behandling.id, vergeDto)

        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val verge = behandling.aktivVerge!!
        assertThat(verge.aktiv).isEqualTo(true)
        assertThat(verge.orgNr).isEqualTo("987654321")
        assertThat(verge.type).isEqualTo(Vergetype.ADVOKAT)
        assertThat(verge.navn).isEqualTo("Stor Herlig Straff")
        assertThat(verge.kilde).isEqualTo(Applikasjon.FAMILIE_TILBAKE.name)
        assertThat(verge.begrunnelse).isEqualTo("Det var nødvendig")
    }

    @Test
    fun `lagreVerge skal deaktivere eksisterende verger i basen`() {
        val behandlingFørOppdatering = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val gammelVerge = behandlingFørOppdatering.aktivVerge!!

        vergeService.lagreVerge(Testdata.behandling.id, vergeDto)

        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val deaktivertVerge = behandling.verger.first { !it.aktiv }
        assertThat(deaktivertVerge.id).isEqualTo(gammelVerge.id)
    }

    @Test
    fun `lagreVerge skal kalle historikkTaskService for å opprette historikkTask`() {
        vergeService.lagreVerge(Testdata.behandling.id, vergeDto)

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

        lagBehandlingsstegstilstand(behandlingFørOppdatering.id, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandlingFørOppdatering.id, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandlingFørOppdatering.id, Behandlingssteg.VERGE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandlingFørOppdatering.id, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)

        vergeService.fjernVerge(Testdata.behandling.id)

        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val deaktivertVerge = behandling.verger.first()
        assertThat(deaktivertVerge.id).isEqualTo(gammelVerge.id)
        assertThat(deaktivertVerge.aktiv).isEqualTo(false)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VERGE, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `fjernVerge skal tilbakeføre verge steg når behandling er på vilkårsvurdering steg og verge fjernet`() {
        val behandlingFørOppdatering = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val gammelVerge = behandlingFørOppdatering.aktivVerge
        assertThat(gammelVerge).isNotNull

        lagBehandlingsstegstilstand(behandlingFørOppdatering.id, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandlingFørOppdatering.id, Behandlingssteg.VERGE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandlingFørOppdatering.id, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandlingFørOppdatering.id, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        vergeService.fjernVerge(Testdata.behandling.id)

        assertFalse { behandlingRepository.findByIdOrThrow(behandlingFørOppdatering.id).harVerge }
        verify {
            historikkTaskService.lagHistorikkTask(behandlingFørOppdatering.id,
                                                  TilbakekrevingHistorikkinnslagstype.VERGE_FJERNET,
                                                  Aktør.SAKSBEHANDLER)
        }
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingFørOppdatering.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VERGE, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `fjernVerge skal tilbakeføre verge steg og fortsette til fakta når behandling er på verge steg og verge fjernet`() {
        val behandlingFørOppdatering = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val behandlingUtenVerge = behandlingRepository.update(behandlingFørOppdatering.copy(verger = emptySet()))

        lagBehandlingsstegstilstand(behandlingUtenVerge.id, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandlingUtenVerge.id, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandlingUtenVerge.id, Behandlingssteg.VERGE, Behandlingsstegstatus.KLAR)

        vergeService.fjernVerge(behandlingUtenVerge.id)

        assertFalse { behandlingUtenVerge.harVerge }
        verify(exactly = 0) { historikkTaskService.lagHistorikkTask(any(), any(), any()) }

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingUtenVerge.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VERGE, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `opprettVergeSteg skal opprette verge steg når behandling er på vilkårsvurdering steg`() {
        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        vergeService.opprettVergeSteg(behandling.id)
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VERGE, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `opprettVergeSteg skal ikke opprette verge steg når behandling er avsluttet`() {
        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = assertFailsWith<RuntimeException> { vergeService.opprettVergeSteg(behandling.id) }
        assertEquals("Behandling med id=${behandling.id} er allerede ferdig behandlet.", exception.message)
    }

    @Test
    fun `opprettVergeSteg skal ikke opprette verge steg når behandling er på vent`() {
        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
        behandlingskontrollService.settBehandlingPåVent(behandling.id,
                                                        Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                                                        LocalDate.now().plusWeeks(4))

        val exception = assertFailsWith<RuntimeException> { vergeService.opprettVergeSteg(behandling.id) }
        assertEquals("Behandling med id=${behandling.id} er på vent.", exception.message)
    }

    @Test
    fun `hentVerge skal returnere lagret verge data`() {
        val aktivVerge = Testdata.behandling.aktivVerge
        assertNotNull(aktivVerge)

        val respons = vergeService.hentVerge(Testdata.behandling.id)

        assertNotNull(respons)
        assertEquals(aktivVerge.begrunnelse, respons.begrunnelse)
        assertEquals(aktivVerge.type, respons.type)
        assertEquals(aktivVerge.ident, respons.ident)
        assertEquals(aktivVerge.navn, respons.navn)
        assertEquals(aktivVerge.orgNr, respons.orgNr)
    }

    private fun lagBehandlingsstegstilstand(behandlingId: UUID,
                                            behandlingssteg: Behandlingssteg,
                                            behandlingsstegstatus: Behandlingsstegstatus) {
        behandlingsstegstilstandRepository.insert(Behandlingsstegstilstand(behandlingId = behandlingId,
                                                                           behandlingssteg = behandlingssteg,
                                                                           behandlingsstegsstatus = behandlingsstegstatus))
    }

    private fun assertBehandlingssteg(behandlingsstegstilstand: List<Behandlingsstegstilstand>,
                                      behandlingssteg: Behandlingssteg,
                                      behandlingsstegstatus: Behandlingsstegstatus) {

        assertTrue {
            behandlingsstegstilstand.any {
                behandlingssteg == it.behandlingssteg &&
                behandlingsstegstatus == it.behandlingsstegsstatus
            }
        }
    }

}