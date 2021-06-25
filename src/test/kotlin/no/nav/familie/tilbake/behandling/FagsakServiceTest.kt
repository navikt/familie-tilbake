package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.task.OpprettBehandlingManueltTask
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.integration.pdl.internal.Kjønn
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.Properties
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class FagsakServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Test
    fun `hentFagsak skal hente fagsak for barnetrygd`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD, eksternFagsakId)

        val fagsakDto = fagsakService.hentFagsak(Fagsystem.BA, eksternFagsakId)

        assertEquals(eksternFagsakId, fagsakDto.eksternFagsakId)
        assertEquals(Språkkode.NB, fagsakDto.språkkode)
        assertEquals(Ytelsestype.BARNETRYGD, fagsakDto.ytelsestype)
        assertEquals(Fagsystem.BA, fagsakDto.fagsystem)

        val brukerDto = fagsakDto.bruker
        assertEquals("123", brukerDto.personIdent)
        assertEquals("testverdi", brukerDto.navn)
        assertEquals(Kjønn.MANN, brukerDto.kjønn)
        assertEquals(LocalDate.now().minusYears(20), brukerDto.fødselsdato)

        val behandlinger = fagsakDto.behandlinger
        assertEquals(1, behandlinger.size)
        val behandlingsoppsummeringtDto = behandlinger.toList()[0]
        assertEquals(behandling.id, behandlingsoppsummeringtDto.behandlingId)
        assertEquals(behandling.eksternBrukId, behandlingsoppsummeringtDto.eksternBrukId)
        assertEquals(behandling.status, behandlingsoppsummeringtDto.status)
        assertEquals(behandling.type, behandlingsoppsummeringtDto.type)
    }

    @Test
    fun `hentFagsak skal ikke hente fagsak for barnetrygd når det ikke finnes`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        val exception = assertFailsWith<RuntimeException>(block =
                                                          { fagsakService.hentFagsak(Fagsystem.BA, eksternFagsakId) })
        assertEquals("Fagsak finnes ikke for Barnetrygd og $eksternFagsakId", exception.message)
    }

    @Test
    fun `finnesÅpenTilbakekrevingsbehandling skal returnere false om fagsak ikke finnes`() {
        val finnesBehandling = fagsakService.finnesÅpenTilbakekrevingsbehandling(Fagsystem.BA, UUID.randomUUID().toString())
        assertFalse { finnesBehandling.finnesÅpenBehandling }
    }

    @Test
    fun `finnesÅpenTilbakekrevingsbehandling skal returnere false om behandling er avsluttet`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        var behandling = opprettBehandling(Ytelsestype.BARNETRYGD, eksternFagsakId)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val finnesBehandling = fagsakService.finnesÅpenTilbakekrevingsbehandling(Fagsystem.BA, eksternFagsakId)
        assertFalse { finnesBehandling.finnesÅpenBehandling }
    }

    @Test
    fun `finnesÅpenTilbakekrevingsbehandling skal returnere true om det finnes en åpen behandling`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        opprettBehandling(Ytelsestype.BARNETRYGD, eksternFagsakId)

        val finnesBehandling = fagsakService.finnesÅpenTilbakekrevingsbehandling(Fagsystem.BA, eksternFagsakId)
        assertTrue { finnesBehandling.finnesÅpenBehandling }
    }

    @Test
    fun `kanBehandlingOpprettesManuelt skal returnere false når det finnes en åpen tilbakekrevingsbehandling`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        opprettBehandling(Ytelsestype.BARNETRYGD, eksternFagsakId)

        val respons = fagsakService.kanBehandlingOpprettesManuelt(eksternFagsakId, Ytelsestype.BARNETRYGD)
        assertFalse { respons.kanBehandlingOpprettes }
        assertNull(respons.kravgrunnlagsreferanse)
        assertEquals("Det finnes allerede en åpen tilbakekrevingsbehandling." +
                     "Kan ikke opprette manuelt tilbakekreving.", respons.melding)
    }

    @Test
    fun `kanBehandlingOpprettesManuelt skal returnere false når det ikke finnes et frakoblet kravgrunnlag`() {
        val respons = fagsakService.kanBehandlingOpprettesManuelt(UUID.randomUUID().toString(), Ytelsestype.BARNETRYGD)
        assertFalse { respons.kanBehandlingOpprettes }
        assertNull(respons.kravgrunnlagsreferanse)
        assertEquals("Det finnes ikke frakoblet kravgrunnlag. Kan ikke opprette manuelt tilbakekreving.", respons.melding)
    }

    @Test
    fun `kanBehandlingOpprettesManuelt skal returnere false når det allerede finnes en opprettelse request`() {
        val mottattXml = Testdata.økonomiXmlMottatt
        økonomiXmlMottattRepository.insert(mottattXml)

        val properties = Properties()
        properties["eksternFagsakId"] = mottattXml.eksternFagsakId
        properties["ytelsestype"] = Ytelsestype.BARNETRYGD.kode
        properties["eksternId"] = mottattXml.referanse
        taskRepository.save(Task(type = OpprettBehandlingManueltTask.TYPE, properties = properties, payload = ""))

        val respons = fagsakService.kanBehandlingOpprettesManuelt(mottattXml.eksternFagsakId, Ytelsestype.BARNETRYGD)
        assertFalse { respons.kanBehandlingOpprettes }
        assertNull(respons.kravgrunnlagsreferanse)
        assertEquals("Det ligger allerede en opprettelse request.Kan ikke opprette manuelt tilbakekreving igjen.",
                     respons.melding)
    }

    @Test
    fun `kanBehandlingOpprettesManuelt skal returnere true når det finnes et frakoblet grunnlag`() {
        val mottattXml = Testdata.økonomiXmlMottatt
        økonomiXmlMottattRepository.insert(mottattXml)

        val respons = fagsakService.kanBehandlingOpprettesManuelt(mottattXml.eksternFagsakId, Ytelsestype.BARNETRYGD)
        assertTrue { respons.kanBehandlingOpprettes }
        assertEquals(mottattXml.referanse, respons.kravgrunnlagsreferanse)
        assertEquals("Det er mulig å opprette behandling manuelt.", respons.melding)
    }

    private fun opprettBehandling(ytelsestype: Ytelsestype, eksternFagsakId: String): Behandling {
        val fagsak = Fagsak(eksternFagsakId = eksternFagsakId,
                            bruker = Bruker("123", Språkkode.NB),
                            ytelsestype = ytelsestype,
                            fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype))
        fagsakRepository.insert(fagsak)

        val behandling = Behandling(fagsakId = fagsak.id,
                                    type = Behandlingstype.TILBAKEKREVING,
                                    ansvarligSaksbehandler = "VL",
                                    behandlendeEnhet = "8020",
                                    behandlendeEnhetsNavn = "Oslo",
                                    manueltOpprettet = false)
        behandlingRepository.insert(behandling)
        return behandling
    }
}
