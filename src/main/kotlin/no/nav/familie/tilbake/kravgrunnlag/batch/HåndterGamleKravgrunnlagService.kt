package no.nav.familie.tilbake.kravgrunnlag.batch

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsystemUtil
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.common.exceptionhandler.SperretKravgrunnlagFeil
import no.nav.familie.tilbake.common.exceptionhandler.UgyldigKravgrunnlagFeil
import no.nav.familie.tilbake.kravgrunnlag.HentKravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagMapper
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattService
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class HåndterGamleKravgrunnlagService(private val behandlingRepository: BehandlingRepository,
                                      private val kravgrunnlagRepository: KravgrunnlagRepository,
                                      private val behandlingService: BehandlingService,
                                      private val økonomiXmlMottattService: ØkonomiXmlMottattService,
                                      private val hentKravgrunnlagService: HentKravgrunnlagService,
                                      private val stegService: StegService) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentFrakobletKravgrunnlag(mottattXmlId: UUID): ØkonomiXmlMottatt {
        return økonomiXmlMottattService.hentMottattKravgrunnlag(mottattXmlId)
    }

    fun sjekkOmDetFinnesEnAktivBehandling(mottattXml: ØkonomiXmlMottatt) {
        val eksternFagsakId = mottattXml.eksternFagsakId
        val ytelsestype = mottattXml.ytelsestype
        val mottattXmlId = mottattXml.id

        logger.info("Sjekker om det finnes en aktiv behandling for fagsak=$eksternFagsakId og ytelsestype=$ytelsestype")
        if (behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype, eksternFagsakId) != null) {
            throw UgyldigKravgrunnlagFeil(melding = "Kravgrunnlag med $mottattXmlId er ugyldig." +
                                                    "Det finnes allerede en åpen behandling for " +
                                                    "fagsak=$eksternFagsakId og ytelsestype=$ytelsestype. " +
                                                    "Kravgrunnlaget skulle være koblet. Kravgrunnlaget arkiveres manuelt" +
                                                    "ved å bruke forvaltningsrutine etter feilundersøkelse.")
        }
    }

    @Transactional
    fun håndter(respons: String, mottattXml: ØkonomiXmlMottatt) {
        logger.info("Håndterer kravgrunnlag med kravgrunnlagId=${mottattXml.eksternKravgrunnlagId}")
        val hentetData: Pair<DetaljertKravgrunnlagDto, Boolean> = hentKravgrunnlagFraØkonomi(mottattXml)
        val hentetKravgrunnlag = hentetData.first
        val erSperret = hentetData.second

        arkiverKravgrunnlag(mottattXml.id)
        val behandling = opprettBehandling(hentetKravgrunnlag, respons)
        val behandlingId = behandling.id
        if (!erSperret) {
            val mottattKravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(mottattXml.melding)
            val diffs = KravgrunnlagUtil.sammenlignKravgrunnlag(mottattKravgrunnlag, hentetKravgrunnlag)
            if (diffs.isNotEmpty()) {
                logger.warn("Det finnes avvik mellom hentet kravgrunnlag og mottatt kravgrunnlag. Avvikene er $diffs")
            }
            logger.info("Kobler kravgrunnlag med kravgrunnlagId=${hentetKravgrunnlag.kravgrunnlagId} " +
                        "til behandling=$behandlingId")
            val kravgrunnlag = KravgrunnlagMapper.tilKravgrunnlag431(hentetKravgrunnlag, behandlingId)
            kravgrunnlagRepository.insert(kravgrunnlag)

            stegService.håndterSteg(behandlingId)
        } else {
            logger.info("Hentet kravgrunnlag med kravgrunnlagId=${hentetKravgrunnlag.kravgrunnlagId} " +
                        "til behandling=$behandlingId er sperret. Venter behandlingen på ny kravgrunnlag fra økonomi")
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun arkiverKravgrunnlag(mottattXmlId: UUID) {
        val mottattXml = hentFrakobletKravgrunnlag(mottattXmlId)
        økonomiXmlMottattService.arkiverMottattXml(mottattXml.melding, mottattXml.eksternFagsakId, mottattXml.ytelsestype)
        økonomiXmlMottattService.slettMottattXml(mottattXmlId)
    }

    private fun hentKravgrunnlagFraØkonomi(mottattXml: ØkonomiXmlMottatt): Pair<DetaljertKravgrunnlagDto, Boolean> {
        return try {
            hentKravgrunnlagService.hentKravgrunnlagFraØkonomi(mottattXml.eksternKravgrunnlagId!!,
                                                               KodeAksjon.HENT_KORRIGERT_KRAVGRUNNLAG) to false
        } catch (e: SperretKravgrunnlagFeil) {
            logger.warn(e.melding)
            KravgrunnlagUtil.unmarshalKravgrunnlag(mottattXml.melding) to true
        }
    }

    private fun opprettBehandling(hentetKravgrunnlag: DetaljertKravgrunnlagDto,
                                  hentFagsystemsbehandlingRespons: String): Behandling {
        val fagsystemsbehandlingData = objectMapper.readValue(hentFagsystemsbehandlingRespons,
                                                              HentFagsystemsbehandlingRespons::class.java)
        val opprettTilbakekrevingRequest =
                lagOpprettBehandlingsrequest(eksternFagsakId = hentetKravgrunnlag.fagsystemId,
                                             ytelsestype = Fagområdekode.fraKode(hentetKravgrunnlag.kodeFagomraade)
                                                     .ytelsestype,
                                             eksternId = hentetKravgrunnlag.referanse,
                                             fagsystemsbehandlingData = fagsystemsbehandlingData)
        return behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
    }

    private fun lagOpprettBehandlingsrequest(eksternFagsakId: String,
                                             ytelsestype: Ytelsestype,
                                             eksternId: String,
                                             fagsystemsbehandlingData: HentFagsystemsbehandlingRespons)
            : OpprettTilbakekrevingRequest {
        return OpprettTilbakekrevingRequest(fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype),
                                            ytelsestype = ytelsestype,
                                            eksternFagsakId = eksternFagsakId,
                                            eksternId = eksternId,
                                            behandlingstype = Behandlingstype.TILBAKEKREVING,
                                            manueltOpprettet = false,
                                            saksbehandlerIdent = "VL",
                                            personIdent = fagsystemsbehandlingData.personIdent,
                                            språkkode = fagsystemsbehandlingData.språkkode,
                                            enhetId = fagsystemsbehandlingData.enhetId,
                                            enhetsnavn = fagsystemsbehandlingData.enhetsnavn,
                                            revurderingsvedtaksdato = fagsystemsbehandlingData.revurderingsvedtaksdato,
                                            faktainfo = fagsystemsbehandlingData.faktainfo,
                                            verge = fagsystemsbehandlingData.verge,
                                            varsel = null)
    }
}