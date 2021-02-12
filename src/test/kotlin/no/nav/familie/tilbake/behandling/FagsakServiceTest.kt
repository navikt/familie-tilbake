package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsaksstatus
import no.nav.familie.tilbake.behandling.domain.Fagsystem
import no.nav.familie.tilbake.behandling.domain.Ytelsestype
import no.nav.familie.tilbake.integration.pdl.internal.Kjønn
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class FagsakServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Test
    fun `hentFagsak skal hente fagsak for barnetrygd`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD, eksternFagsakId)

        val fagsakResponsDto = fagsakService.hentFagsak(Ytelsestype.BARNETRYGD, eksternFagsakId)

        val fagsakDto = fagsakResponsDto.fagsak
        assertEquals(eksternFagsakId, fagsakDto.eksternFagsakId)
        assertEquals("NB", fagsakDto.språkkode)
        assertEquals(Fagsaksstatus.OPPRETTET, fagsakDto.status)
        assertEquals(Ytelsestype.BARNETRYGD, fagsakDto.ytelsestype)

        val brukerDto = fagsakDto.bruker
        assertEquals("123", brukerDto.søkerFødselsnummer)
        assertEquals("testverdi", brukerDto.navn)
        assertEquals(Kjønn.MANN, brukerDto.kjønn)
        assertEquals(LocalDate.now().minusYears(20), brukerDto.fødselsdato)

        val behandlinger = fagsakResponsDto.behandlinger
        assertEquals(1, behandlinger.size)
        val behandlingsoppsummeringtDto = behandlinger.toList().get(0)
        assertEquals(behandling.id, behandlingsoppsummeringtDto.behandlingId)
        assertEquals(behandling.eksternBrukId, behandlingsoppsummeringtDto.eksternBrukId)
        assertEquals(behandling.status, behandlingsoppsummeringtDto.status)
        assertEquals(behandling.type, behandlingsoppsummeringtDto.type)
    }

    @Test
    fun `hentFagsak skal ikke hente fagsak for barnetrygd når det ikke finnes`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        val exception = assertFailsWith<RuntimeException>(block =
                                                          { fagsakService.hentFagsak(Ytelsestype.BARNETRYGD, eksternFagsakId) })
        assertEquals("Fagsak finnes ikke for BARNETRYGD og $eksternFagsakId", exception.message)
    }

    private fun opprettBehandling(ytelsestype: Ytelsestype, eksternFagsakId: String): Behandling {
        val fagsak = Fagsak(eksternFagsakId = eksternFagsakId,
                            bruker = Bruker("123", "NB"),
                            ytelsestype = ytelsestype,
                            fagsystem = Fagsystem.fraYtelsestype(ytelsestype))
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
