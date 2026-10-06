package no.nav.tilbakekreving

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.PdlKjønnType
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.api.v2.fagsystem.behov.FagsysteminfoBehovHendelse
import no.nav.tilbakekreving.behov.Behov
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.behov.IverksettelseBehov
import no.nav.tilbakekreving.behov.KorrigertKravgrunnlagBehov
import no.nav.tilbakekreving.behov.VarselbrevDistribusjonBehov
import no.nav.tilbakekreving.behov.VarselbrevJournalføringBehov
import no.nav.tilbakekreving.behov.VedtaksbrevDistribusjonBehov
import no.nav.tilbakekreving.behov.VedtaksbrevJournalføringBehov
import no.nav.tilbakekreving.brev.varselbrev.ForhåndsvarselService
import no.nav.tilbakekreving.brev.vedtaksbrev.NyVedtaksbrevService
import no.nav.tilbakekreving.config.FeatureService
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.DistribusjonHendelse
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.JournalføringHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevDistribueringHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevJournalføringHendelse
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.DokdistClient
import no.nav.tilbakekreving.integrasjoner.oppdrag.OppdragRestClient
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.KodeAksjonDto
import no.nav.tilbakekreving.kontrakter.bruker.Kjønn
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagMapper
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BehovMediator(
    private val pdlClient: PdlClient,
    private val iverksettService: IverksettService,
    private val kafkaProducer: KafkaProducer,
    private val dokdistService: DokdistClient,
    private val forhåndsvarselService: ForhåndsvarselService,
    private val vedtaksbrevService: NyVedtaksbrevService,
    private val featureService: FeatureService,
    private val oppdragRestClient: OppdragRestClient,
) {
    fun håndterBehov(
        tilbakekreving: Tilbakekreving,
        sideeffektContext: SideeffektContext,
        behov: Behov,
        logContext: SecureLog.Context,
    ) {
        when (behov) {
            is BrukerinfoBehov -> {
                val personinfo = pdlClient.hentPersoninfo(
                    ident = behov.ident,
                    fagsystem = behov.ytelse.tilFagsystemDTO(),
                    logContext = SecureLog.Context.fra(tilbakekreving),
                )
                tilbakekreving.håndter(
                    BrukerinfoHendelse(
                        ident = personinfo.ident,
                        fødselsdato = personinfo.fødselsdato,
                        navn = personinfo.navn,
                        kjønn = when (personinfo.kjønn) {
                            PdlKjønnType.MANN -> Kjønn.MANN
                            PdlKjønnType.KVINNE -> Kjønn.KVINNE
                            PdlKjønnType.UKJENT -> Kjønn.UKJENT
                        },
                        dødsdato = personinfo.dødsdato,
                    ),
                    sideeffektContext,
                )
            }

            is VarselbrevJournalføringBehov -> {
                val journalpostResponse = forhåndsvarselService.journalførVarselbrev(
                    varselbrevBehov = behov,
                    logContext = logContext,
                    features = featureService.modellFeatures,
                )
                if (journalpostResponse.journalpostId == null) {
                    throw Feil(
                        message = "journalførin av varselbrev til behandlingId ${behov.behandlingId} misslykket med denne meldingen: ${journalpostResponse.melding}",
                        frontendFeilmelding = "journalførin av varselbrev til behandlingId ${behov.behandlingId} misslykket med denne meldingen: ${journalpostResponse.melding}",
                        logContext = SecureLog.Context.fra(tilbakekreving),
                    )
                }

                if (journalpostResponse.dokumenter.isNullOrEmpty()) {
                    throw Feil(
                        message = "Response fra journalføring av varselbrev til behandlingId ${behov.behandlingId} mangler dokumenter. Dokumenter er enten null eller tom. ${journalpostResponse.melding}",
                        frontendFeilmelding = "Response fra journalføring av varselbrev til behandlingId ${behov.behandlingId} mangler dokumenter. Dokumenter er enten null eller tom. ${journalpostResponse.melding}",
                        logContext = SecureLog.Context.fra(tilbakekreving),
                    )
                }

                tilbakekreving.håndter(
                    VarselbrevJournalføringHendelse(
                        varselbrevId = behov.info.id,
                        journalpostId = journalpostResponse.journalpostId,
                        dokumentInfoId = journalpostResponse.dokumenter[0].dokumentInfoId!!,
                    ),
                    sideeffektContext,
                )
            }

            is VarselbrevDistribusjonBehov -> {
                dokdistService.brevTilUtsending(
                    behandlingId = behov.behandlingId,
                    journalpostId = behov.journalpostId,
                    fagsystem = behov.ytelse.tilFagsystemDTO(),
                    distribusjonstype = Distribusjonstype.VIKTIG,
                    distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID,
                    adresse = null,
                    logContext = logContext,
                )
                tilbakekreving.håndter(
                    VarselbrevDistribueringHendelse(
                        brevId = behov.brevId,
                        journalpostId = behov.journalpostId,
                        dokumentInfoId = behov.dokumentInfoId,
                    ),
                    sideeffektContext,
                )
            }

            is FagsysteminfoBehov -> {
                val logContext = SecureLog.Context.utenBehandling(behov.eksternFagsakId)
                kafkaProducer.sendKafkaEvent(
                    kafkamelding = FagsysteminfoBehovHendelse(
                        eksternFagsakId = behov.eksternFagsakId,
                        kravgrunnlagReferanse = behov.eksternBehandlingId,
                        hendelseOpprettet = LocalDateTime.now(),
                    ),
                    metadata = FagsysteminfoBehovHendelse.METADATA,
                    vedtakGjelderId = behov.vedtakGjelderId,
                    ytelse = behov.ytelse,
                    logContext = logContext,
                )
            }

            is IverksettelseBehov -> {
                val iverksattVedtak = iverksettService.iverksett(behov, logContext)

                tilbakekreving.håndter(
                    IverksettelseHendelse(
                        iverksattVedtakId = iverksattVedtak.id,
                        behandlingId = iverksattVedtak.behandlingId,
                        vedtakId = iverksattVedtak.vedtakId,
                    ),
                    sideeffektContext,
                )
            }

            is VedtaksbrevJournalføringBehov -> {
                val journalpost = vedtaksbrevService.journalførVedtaksbrev(behov)
                if (journalpost.journalpostId == null) {
                    throw Feil(
                        message = "journalføring av vedtaksbrev til behandlingId ${behov.behandlingId} misslykket med denne meldingen: ${journalpost.melding}",
                        frontendFeilmelding = "journalføring av vedtaksbrev til behandlingId ${behov.behandlingId} misslykket med denne meldingen: ${journalpost.melding}",
                        logContext = SecureLog.Context.fra(tilbakekreving),
                    )
                }
                if (journalpost.dokumenter.isNullOrEmpty()) {
                    throw Feil(
                        message = "Response fra journalføring av vedtaksbrev til behandlingId ${behov.behandlingId} mangler dokumenter. Dokumenter er enten null eller tom. ${journalpost.melding}",
                        frontendFeilmelding = "Response fra journalføring av vedtaksbrev til behandlingId ${behov.behandlingId} mangler dokumenter. Dokumenter er enten null eller tom. ${journalpost.melding}",
                        logContext = SecureLog.Context.fra(tilbakekreving),
                    )
                }

                tilbakekreving.håndter(
                    JournalføringHendelse(
                        brevId = behov.brevId,
                        behandlingId = behov.behandlingId,
                        journalpostId = journalpost.journalpostId,
                        fagsakId = behov.fagsakId,
                        dokumentInfoId = journalpost.dokumenter[0].dokumentInfoId!!,
                    ),
                    sideeffektContext,
                )
            }

            is VedtaksbrevDistribusjonBehov -> {
                vedtaksbrevService.distribuereVedtaksbrev(behov, logContext)
                tilbakekreving.håndter(
                    DistribusjonHendelse(
                        behandlingId = behov.behandlingId,
                        brevId = behov.brevId,
                        fagsakId = behov.fagsakId,
                        journalpostId = behov.journalpostId,
                        dokumentInfoId = behov.dokumentInfoId,
                    ),
                    sideeffektContext,
                )
            }
            is KorrigertKravgrunnlagBehov -> korrigerKravgrunnlag(behov, tilbakekreving, sideeffektContext)
        }
    }

    internal fun hentKorrigertKravgrunnlag(behov: KorrigertKravgrunnlagBehov): KravgrunnlagHendelse {
        val oppdatertKravgrunnlag = oppdragRestClient.hentKravgrunnlag(
            kravgrunnlagId = behov.kravgrunnlagId.toBigInteger(),
            kodeAksjon = KodeAksjonDto.HENT_KRAVGRUNNLAG_FOR_DANNING_AV_NYTT_TILBAKEKREVINGSVEDTAK,
        )
        return KravgrunnlagMapper.tilKravgrunnlagHendelse(oppdatertKravgrunnlag, SystemKlokke)
    }

    internal fun korrigerKravgrunnlag(
        behov: KorrigertKravgrunnlagBehov,
        tilbakekreving: Tilbakekreving,
        sideeffektContext: SideeffektContext,
    ) {
        tilbakekreving.håndter(hentKorrigertKravgrunnlag(behov), sideeffektContext)
    }
}
