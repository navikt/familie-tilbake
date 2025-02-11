package no.nav.familie.tilbake.database

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.OVERGANGSSTØNAD
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagService
import no.nav.familie.tilbake.log.SecureLog
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.Properties

@Component
@Profile("local")
class TestdataInitializer : ApplicationListener<ContextRefreshedEvent> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var kravgrunnlagService: KravgrunnlagService

    val opprettTilbakekrevingRequest =
        OpprettTilbakekrevingRequest(
            ytelsestype = OVERGANGSSTØNAD,
            fagsystem = Fagsystem.EF,
            eksternFagsakId = "1234567",
            personIdent = "321321322",
            eksternId = "f5ed9439-54da-4f50-8457-534c417e3430",
            manueltOpprettet = false,
            språkkode = Språkkode.NB,
            enhetId = "8020",
            enhetsnavn = "Oslo",
            varsel = null,
            revurderingsvedtaksdato = LocalDate.now().minusDays(10),
            verge = null,
            faktainfo = Faktainfo("årsak", "resultat", OPPRETT_TILBAKEKREVING_UTEN_VARSEL, setOf("ENF")),
            saksbehandlerIdent = "Z0000",
            institusjon = null,
            manuelleBrevmottakere = emptySet(),
            begrunnelseForTilbakekreving = null,
        )

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        val åpenTilbakekrevingsbehandling = behandlingRepository.finnÅpenTilbakekrevingsbehandling(OVERGANGSSTØNAD, "1234567")

        if (åpenTilbakekrevingsbehandling == null) {
            behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

            val behandling = behandlingRepository.finnNyesteTilbakekrevingsbehandlingForYtelsestypeAndEksternFagsakId(opprettTilbakekrevingRequest.ytelsestype, opprettTilbakekrevingRequest.eksternFagsakId)
            logger.info("Opprettet dummy-behandling. Hvis frontend kjøres lokalt kan du gå til: http://localhost:8000/fagsystem/${opprettTilbakekrevingRequest.fagsystem}/fagsak/${opprettTilbakekrevingRequest.eksternFagsakId}/behandling/${behandling?.eksternBrukId}")

            val mottattXml = readXml("/kravgrunnlagxml/kravgrunnlag_lokal_kjøring.xml")

            kravgrunnlagService.håndterMottattKravgrunnlag(mottattXml.replace("<urn:fagsystemId>testverdi</urn:fagsystemId>", "<urn:fagsystemId>1234567</urn:fagsystemId>"), 0L, Properties(), SecureLog.Context.tom())
        } else {
            logger.info("Opprettet dummy-behandling. Hvis frontend kjøres lokalt kan du gå til: http://localhost:8000/fagsystem/${opprettTilbakekrevingRequest.fagsystem}/fagsak/${opprettTilbakekrevingRequest.eksternFagsakId}/behandling/${åpenTilbakekrevingsbehandling.eksternBrukId}")
        }
    }

    fun readXml(fileName: String): String {
        val url = requireNotNull(this::class.java.getResource(fileName)) { "fil med filnavn=$fileName finnes ikke" }
        return url.readText()
    }
}
