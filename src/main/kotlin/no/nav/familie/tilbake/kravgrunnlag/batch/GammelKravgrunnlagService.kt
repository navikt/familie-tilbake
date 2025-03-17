package no.nav.familie.tilbake.kravgrunnlag.batch

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsystemUtil
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.common.exceptionhandler.KravgrunnlagIkkeFunnetFeil
import no.nav.familie.tilbake.common.exceptionhandler.SperretKravgrunnlagFeil
import no.nav.familie.tilbake.common.exceptionhandler.UgyldigKravgrunnlagFeil
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.HentKravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagMapper
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottattArkiv
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.kontrakter.Behandlingstype
import no.nav.tilbakekreving.kontrakter.Faktainfo
import no.nav.tilbakekreving.kontrakter.HentFagsystemsbehandling
import no.nav.tilbakekreving.kontrakter.OpprettTilbakekrevingRequest
import no.nav.tilbakekreving.kontrakter.Tilbakekrevingsvalg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class GammelKravgrunnlagService(
    private val behandlingRepository: BehandlingRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val behandlingService: BehandlingService,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val økonomiXmlMottattService: ØkonomiXmlMottattService,
    private val hentKravgrunnlagService: HentKravgrunnlagService,
    private val kravgrunnlagService: KravgrunnlagService,
    private val stegService: StegService,
    private val historikkService: HistorikkService,
) {
    private val log = TracedLogger.getLogger<GammelKravgrunnlagService>()

    fun hentFrakobletKravgrunnlag(mottattXmlId: UUID): ØkonomiXmlMottatt = økonomiXmlMottattService.hentMottattKravgrunnlag(mottattXmlId)

    fun hentFrakobletKravgrunnlagNullable(mottattXmlId: UUID): ØkonomiXmlMottatt? = økonomiXmlMottattService.hentMottattKravgrunnlagNullable(mottattXmlId)

    fun sjekkOmDetFinnesEnAktivBehandling(mottattXml: ØkonomiXmlMottatt) {
        val eksternFagsakId = mottattXml.eksternFagsakId
        val ytelsestype = mottattXml.ytelsestype
        val mottattXmlId = mottattXml.id
        val logContext = SecureLog.Context.utenBehandling(eksternFagsakId)

        log.medContext(logContext) {
            info("Sjekker om det finnes en aktiv behandling for fagsak=$eksternFagsakId og ytelsestype=$ytelsestype")
        }
        if (behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype, eksternFagsakId) != null) {
            throw UgyldigKravgrunnlagFeil(
                melding =
                    "Kravgrunnlag med $mottattXmlId er ugyldig." +
                        "Det finnes allerede en åpen behandling for " +
                        "fagsak=$eksternFagsakId og ytelsestype=$ytelsestype. " +
                        "Kravgrunnlaget skulle være koblet. Kravgrunnlaget arkiveres manuelt" +
                        "ved å bruke forvaltningsrutine etter feilundersøkelse.",
                logContext = SecureLog.Context.utenBehandling(eksternFagsakId),
            )
        }
    }

    fun sjekkArkivForDuplikatKravgrunnlagMedKravstatusAvsluttet(kravgrunnlagIkkeFunnet: ØkonomiXmlMottatt): Boolean {
        val arkiverteXmlMottattPåSammeFagsak =
            økonomiXmlMottattService.hentArkiverteMottattXml(
                eksternFagsakId = kravgrunnlagIkkeFunnet.eksternFagsakId,
                ytelsestype = kravgrunnlagIkkeFunnet.ytelsestype,
            )
        val arkiverteKravgrunnlag =
            arkiverteXmlMottattPåSammeFagsak
                .filter { it.melding.contains(Constants.KRAVGRUNNLAG_XML_ROOT_ELEMENT) }
        val arkiverteStatusmeldinger =
            arkiverteXmlMottattPåSammeFagsak
                .filter { it.melding.contains(Constants.STATUSMELDING_XML_ROOT_ELEMENT) }

        return arkiverteKravgrunnlag
            .any { arkivertKravgrunnlag ->
                arkivertKravgrunnlag.sporbar.opprettetTid.isAfter(kravgrunnlagIkkeFunnet.sporbar.opprettetTid) &&
                    sjekkDiff(
                        arkivertXml = arkivertKravgrunnlag,
                        mottattXml = kravgrunnlagIkkeFunnet,
                        forventedeAvvik = listOf("kravgrunnlagId", "vedtakId", "kontrollfelt"),
                    ) &&
                    arkivertKravgrunnlag.harKravstatusAvsluttet(arkiverteStatusmeldinger)
            }
    }

    @Transactional(rollbackFor = [Exception::class])
    fun håndter(
        fagsystemsbehandlingData: HentFagsystemsbehandling,
        mottattXml: ØkonomiXmlMottatt,
        task: Task,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Håndterer kravgrunnlag med kravgrunnlagId=${mottattXml.eksternKravgrunnlagId}")
        }
        val (hentetKravgrunnlag, kravgrunnlagErSperret) =
            try {
                hentKravgrunnlagFraØkonomi(mottattXml, logContext)
            } catch (e: KravgrunnlagIkkeFunnetFeil) {
                if (sjekkArkivForDuplikatKravgrunnlagMedKravstatusAvsluttet(kravgrunnlagIkkeFunnet = mottattXml)) {
                    log.medContext(logContext) {
                        warn(
                            "Kravgrunnlag(id=${mottattXml.id}, eksternFagsakId=${mottattXml.eksternFagsakId}) ble ikke funnet hos økonomi," +
                                " men identisk kravgrunnlag med påfølgende melding om at kravet er avsluttet ble funnet i arkivet.",
                        )
                    }
                    arkiverKravgrunnlag(mottattXml.id)
                    task.metadata["merknad"] =
                        "Arkivert da kravgrunnlag ikke ble funnet hos økonomi, og duplikat kravgrunnlag med kravstatus AVSLUTTET funnet i arkivet"
                    return
                } else {
                    throw e
                }
            }

        arkiverKravgrunnlag(mottattXml.id)

        // Hvis det finnes en åpen behandling for fagsak og ytelsestype, så skal kravgrunnlaget knyttes til denne behandlingen og arkiveres
        val åpenBehandling =
            behandlingRepository.finnÅpenTilbakekrevingsbehandling(
                ytelsestype = Fagområdekode.fraKode(hentetKravgrunnlag.kodeFagomraade).ytelsestype,
                eksternFagsakId = hentetKravgrunnlag.fagsystemId,
            )

        val behandling = åpenBehandling ?: opprettBehandlingFraKravgrunnlag(hentetKravgrunnlag, fagsystemsbehandlingData)

        val behandlingId = behandling.id

        val mottattKravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(mottattXml.melding)
        val diffs = KravgrunnlagUtil.sammenlignKravgrunnlag(mottattKravgrunnlag, hentetKravgrunnlag)
        if (diffs.isNotEmpty()) {
            log.medContext(logContext) {
                warn("Det finnes avvik mellom hentet kravgrunnlag og mottatt kravgrunnlag for ${hentetKravgrunnlag.kodeFagomraade}. Avvikene er $diffs")
            }
        }
        log.medContext(logContext) {
            info(
                "Kobler kravgrunnlag med kravgrunnlagId=${hentetKravgrunnlag.kravgrunnlagId} " +
                    "til behandling=$behandlingId",
            )
        }
        val kravgrunnlag = KravgrunnlagMapper.tilKravgrunnlag431(hentetKravgrunnlag, behandlingId)
        kravgrunnlagService.sjekkIdentiskKravgrunnlag(kravgrunnlag, behandling, logContext)
        kravgrunnlagRepository.insert(kravgrunnlag)

        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_HENT,
            aktør = Aktør.Vedtaksløsning,
            opprettetTidspunkt = LocalDateTime.now(),
        )

        stegService.håndterSteg(behandlingId, logContext)
        if (kravgrunnlagErSperret) {
            log.medContext(logContext) {
                info(
                    "Hentet kravgrunnlag med kravgrunnlagId=${hentetKravgrunnlag.kravgrunnlagId} " +
                        "til behandling=$behandlingId er sperret. Venter behandlingen på ny kravgrunnlag fra økonomi",
                )
            }
            sperKravgrunnlag(behandlingId, logContext)
        }
    }

    @Transactional
    fun arkiverKravgrunnlag(mottattXmlId: UUID) {
        val mottattXml = hentFrakobletKravgrunnlag(mottattXmlId)
        økonomiXmlMottattService.arkiverMottattXml(mottattXmlId = mottattXmlId, mottattXml.melding, mottattXml.eksternFagsakId, mottattXml.ytelsestype)
        økonomiXmlMottattService.slettMottattXml(mottattXmlId)
    }

    private fun hentKravgrunnlagFraØkonomi(
        mottattXml: ØkonomiXmlMottatt,
        logContext: SecureLog.Context,
    ): Pair<DetaljertKravgrunnlagDto, Boolean> =
        try {
            hentKravgrunnlagService.hentKravgrunnlagFraØkonomi(
                mottattXml.eksternKravgrunnlagId!!,
                KodeAksjon.HENT_KORRIGERT_KRAVGRUNNLAG,
                logContext,
            ) to false
        } catch (e: SperretKravgrunnlagFeil) {
            log.medContext(logContext) {
                warn(e.melding)
            }
            KravgrunnlagUtil.unmarshalKravgrunnlag(mottattXml.melding) to true
        }

    fun opprettBehandlingFraKravgrunnlag(
        hentetKravgrunnlag: DetaljertKravgrunnlagDto,
        fagsystemsbehandlingData: HentFagsystemsbehandling,
    ): Behandling {
        val opprettTilbakekrevingRequest =
            lagOpprettBehandlingsrequest(
                eksternFagsakId = hentetKravgrunnlag.fagsystemId,
                ytelsestype =
                    Fagområdekode
                        .fraKode(hentetKravgrunnlag.kodeFagomraade)
                        .ytelsestype,
                eksternId = hentetKravgrunnlag.referanse,
                fagsystemsbehandlingData = fagsystemsbehandlingData,
            )
        return behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
    }

    private fun lagOpprettBehandlingsrequest(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
        eksternId: String,
        fagsystemsbehandlingData: HentFagsystemsbehandling,
    ): OpprettTilbakekrevingRequest =
        OpprettTilbakekrevingRequest(
            fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype),
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
            faktainfo = setFaktainfo(fagsystemsbehandlingData.faktainfo),
            verge = fagsystemsbehandlingData.verge,
            varsel = null,
            begrunnelseForTilbakekreving = null,
        )

    private fun sperKravgrunnlag(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        kravgrunnlagRepository.update(kravgrunnlag.copy(sperret = true))
        val venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG
        behandlingskontrollService
            .tilbakehoppBehandlingssteg(
                behandlingId,
                Behandlingsstegsinfo(
                    behandlingssteg = Behandlingssteg.GRUNNLAG,
                    behandlingsstegstatus = Behandlingsstegstatus.VENTER,
                    venteårsak = venteårsak,
                    tidsfrist =
                        LocalDate
                            .now()
                            .plusWeeks(venteårsak.defaultVenteTidIUker),
                ),
                logContext,
            )
        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
            aktør = Aktør.Vedtaksløsning,
            beskrivelse = venteårsak.beskrivelse,
            opprettetTidspunkt = LocalDateTime.now(),
        )
    }

    private fun setFaktainfo(faktainfo: Faktainfo): Faktainfo =
        Faktainfo(
            revurderingsresultat = faktainfo.revurderingsresultat,
            revurderingsårsak = faktainfo.revurderingsårsak,
            tilbakekrevingsvalg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
            konsekvensForYtelser = faktainfo.konsekvensForYtelser,
        )

    private fun sjekkDiff(
        arkivertXml: ØkonomiXmlMottattArkiv,
        mottattXml: ØkonomiXmlMottatt,
        forventedeAvvik: List<String>,
    ) = arkivertXml.melding.linjeformatert.lines().minus(mottattXml.melding.linjeformatert.lines()).none { avvik ->
        forventedeAvvik.none { it in avvik }
    }
}

private val String.linjeformatert: String
    get() = replace("<urn", "\n<urn")

private fun ØkonomiXmlMottattArkiv.harKravstatusAvsluttet(statusmeldingerMottatt: List<ØkonomiXmlMottattArkiv>): Boolean {
    val kravgrunnlagDto = KravgrunnlagUtil.unmarshalKravgrunnlag(melding)

    return statusmeldingerMottatt.any {
        KravgrunnlagUtil.unmarshalStatusmelding(it.melding).let { statusmelding ->
            statusmelding.vedtakId == kravgrunnlagDto.vedtakId &&
                statusmelding.kodeStatusKrav == Kravstatuskode.AVSLUTTET.kode
        }
    }
}
