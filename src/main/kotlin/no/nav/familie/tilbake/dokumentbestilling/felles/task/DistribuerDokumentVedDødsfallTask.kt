package no.nav.familie.tilbake.dokumentbestilling.felles.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.http.RessursException
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype
import no.nav.familie.tilbake.log.SecureLog.Context.Companion.logContext
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

const val ANTALL_SEKUNDER_I_EN_UKE = 604800L

@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerDokumentVedDødsfallTask.TYPE,
    beskrivelse = "Send dødsfall dokument til Dokdist",
    triggerTidVedFeilISekunder = ANTALL_SEKUNDER_I_EN_UKE,
    // ~8 måneder dersom vi prøver én gang i uka.
    // Tasken skal stoppe etter 6 måneder, så om vi kommer hit har det skjedd noe galt.
    maxAntallFeil = 4 * 8,
    settTilManuellOppfølgning = true,
)
class DistribuerDokumentVedDødsfallTask(
    private val integrasjonerClient: IntegrasjonerClient,
    private val historikkService: HistorikkService,
    private val behandlingRepository: BehandlingRepository,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<DistribuerDokumentVedDødsfallTask>()

    override fun doTask(task: Task) {
        val logContext = task.logContext()
        log.medContext(logContext) { info("{} prosesserer med id={} og metadata {}", this::class.simpleName, task.id, task.metadata.toString()) }

        val journalpostId = task.metadata.getProperty("journalpostId")
        val fagsystem = task.metadata.getProperty("fagsystem")

        val erTaskEldreEnn6Mnd = task.opprettetTid.isBefore(LocalDateTime.now().minusMonths(6))

        if (erTaskEldreEnn6Mnd) {
            log.medContext(logContext) { info("Stopper \"DistribuerDokumentVedDødsfallTask\" fordi den er eldre enn 6 måneder.") }
            opprettHistorikkinnslag(task, TilbakekrevingHistorikkinnslagstype.DISTRIBUSJON_BREV_DØDSBO_FEILET_6_MND, true)
        } else {
            try {
                integrasjonerClient.distribuerJournalpost(
                    journalpostId,
                    Fagsystem.valueOf(fagsystem),
                    Distribusjonstype.valueOf(task.metadata.getProperty("distribusjonstype")),
                    Distribusjonstidspunkt.valueOf(task.metadata.getProperty("distribusjonstidspunkt")),
                )

                log.medContext(logContext) { info("Task \"DistribuerDokumentVedDødsfallTask\" har kjørt suksessfullt, og brev er sendt") }
                opprettHistorikkinnslag(task, TilbakekrevingHistorikkinnslagstype.DISTRIBUSJON_BREV_DØDSBO_SUKSESS)
            } catch (ressursException: RessursException) {
                if (mottakerErDødUtenDødsboadresse(ressursException)) {
                    log.medContext(logContext) { info("Klarte ikke å distribuere journalpost {} for fagsystem {}. Prøver igjen om 7 dager.", journalpostId, fagsystem) }
                    throw ressursException
                } else {
                    throw ressursException
                }
            }
        }
    }

    private fun opprettHistorikkinnslag(
        task: Task,
        tilbakekrevingHistorikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
        feilet: Boolean = false,
    ) {
        val behandlingId = UUID.fromString(task.payload)
        val mottager = Brevmottager.valueOf(task.metadata.getProperty("mottager"))
        val brevtype = Brevtype.valueOf(task.metadata.getProperty("brevtype"))
        val ansvarligSaksbehandler = task.metadata.getProperty("ansvarligSaksbehandler")
        val opprinneligHistorikkinnslagstype = LagreBrevsporingTask.utledHistorikkinnslagType(brevtype, mottager)
        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = tilbakekrevingHistorikkinnslagstype,
            aktør = LagreBrevsporingTask.utledAktør(brevtype, ansvarligSaksbehandler, behandlingId, behandlingRepository),
            opprettetTidspunkt = LocalDateTime.now(),
            beskrivelse = opprinneligHistorikkinnslagstype.tekst,
            brevtype = if (!feilet) brevtype else null,
        )
    }

    companion object {
        const val TYPE = "distribuerDokumentVedDødsfallPåFagsak"

        // 410 GONE er unikt for bruker død og ingen dødsboadresse mot Dokdist
        // https://nav-it.slack.com/archives/C6W9E5GPJ/p1647956660364779?thread_ts=1647936835.099329&cid=C6W9E5GPJ
        fun mottakerErDødUtenDødsboadresse(ressursException: RessursException): Boolean = ressursException.httpStatus == HttpStatus.GONE
    }
}
