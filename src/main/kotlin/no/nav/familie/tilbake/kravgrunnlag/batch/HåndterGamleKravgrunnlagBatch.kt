package no.nav.familie.tilbake.kravgrunnlag.batch

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.BARNETILSYN
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.BARNETRYGD
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.KONTANTSTØTTE
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.OVERGANGSSTØNAD
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.SKOLEPENGER
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.FagsystemUtil
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

@Service
class HåndterGamleKravgrunnlagBatch(
    private val mottattXmlService: ØkonomiXmlMottattService,
    private val taskService: TaskService,
    private val environment: Environment,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "\${CRON_HÅNDTER_GAMMEL_KRAVGRUNNLAG}")
    @Transactional
    fun utfør() {
        if(erLederEllerLokaltMiljø()) {
            logger.info("Starter HåndterGamleKravgrunnlagBatch..")

            logger.info("Henter kravgrunnlag som er eldre enn $ALDERSGRENSE_I_UKER uker")

            val frakobletKravgrunnlag =
                mottattXmlService.hentFrakobletKravgrunnlag(
                    beregnBestemtDato(BARNETRYGD),
                    beregnBestemtDato(BARNETILSYN),
                    beregnBestemtDato(OVERGANGSSTØNAD),
                    beregnBestemtDato(SKOLEPENGER),
                    beregnBestemtDato(KONTANTSTØTTE),
                )

            val frakobletKravgrunnlagGruppertPåEksternFagsakId = frakobletKravgrunnlag.groupBy { it.eksternFagsakId }

            if (frakobletKravgrunnlagGruppertPåEksternFagsakId.isEmpty()) {
                logger.info("Det finnes ingen kravgrunnlag som er eldre enn $ALDERSGRENSE_I_UKER uker fra dagens dato")
                logger.info("Stopper HåndterGamleKravgrunnlagBatch..")
                return
            }

            logger.info(
                "Det finnes ${frakobletKravgrunnlag.size} kravgrunnlag som er eldre enn " +
                        "$ALDERSGRENSE_I_UKER uker fra dagens dato",
            )

            val taskerMedStatus =
                taskService.finnTasksMedStatus(
                    RELEVANTE_TASK_STATUSER,
                    Pageable.unpaged(),
                )

            frakobletKravgrunnlagGruppertPåEksternFagsakId.forEach { (_, kravgrunnlagerPåFagsak) ->

                val kravgrunnlagSortertEtterKontrollfelt = sorterKravgrunnlagPåKontrollfelt(kravgrunnlagerPåFagsak)

                kravgrunnlagSortertEtterKontrollfelt.forEachIndexed { index, kravgrunnlagPåFagsak ->

                    val finnesAlleredeTaskPåKravgrunnlag = finnesAlleredeTaskForKravgrunnlag(taskerMedStatus, kravgrunnlagPåFagsak)

                    if (!finnesAlleredeTaskPåKravgrunnlag) {
                        taskService.save(opprettSpredtTaskForKravgrunnlagBasertPåIndex(index, kravgrunnlagPåFagsak))
                    } else {
                        logger.info(
                            "Det finnes allerede en feilet HåndterGammelKravgrunnlagTask " +
                                    "eller HentFagsystemsbehandlingTask " +
                                    "på det samme kravgrunnlaget med id ${kravgrunnlagPåFagsak.id}",
                        )
                    }
                }
            }

            logger.info("Stopper HåndterGamleKravgrunnlagBatch..")
        }
    }

    private fun erLederEllerLokaltMiljø(): Boolean {
        val erLeader = LeaderClient.isLeader() == true

        val erLokaltMiljø =
            environment.activeProfiles.any {
                it.contains("local") || it.contains("integrasjonstest")
            }
        return erLeader || erLokaltMiljø

    }

    private fun beregnBestemtDato(ytelsestype: Ytelsestype): LocalDate {
        return LocalDate.now().minusWeeks(ALDERSGRENSE_I_UKER.getValue(ytelsestype))
    }

    private fun sorterKravgrunnlagPåKontrollfelt(kravgrunnlagerPåFagsak: List<ØkonomiXmlMottatt>) =
        kravgrunnlagerPåFagsak.sortedBy {
            val kontrollfelt = it.kontrollfelt ?: throw IllegalStateException("Kontrollfelt eksisterer ikke på kravgrunnlag med id ${it.id}")
            LocalDate.parse(kontrollfelt, KONTROLLFELT_DATE_TIME_FORMATTER)
        }

    private fun finnesAlleredeTaskForKravgrunnlag(
        taskerMedStatus: List<Task>,
        kravgrunnlag: ØkonomiXmlMottatt,
    ) =
        taskerMedStatus.any {
            val harRiktigType = it.type == GammelKravgrunnlagTask.TYPE || it.type == HentFagsystemsbehandlingTask.TYPE
            it.payload == kravgrunnlag.id.toString() && harRiktigType
        }

    private fun opprettSpredtTaskForKravgrunnlagBasertPåIndex(
        index: Int,
        kravgrunnlagPåFagsak: ØkonomiXmlMottatt,
    ): Task {
        // Dersom det eksistere flere kravgunnlag på samme eksternFagsakId ønsker vi å spre utførelsen
        // av taskene for å unngå at flere behandlinger blir opprettet på samme fagsak
        val triggerTid = LocalDateTime.now().plusHours(index.toLong())
        return Task(
            type = HentFagsystemsbehandlingTask.TYPE,
            payload = kravgrunnlagPåFagsak.id.toString(),
            properties =
                Properties().apply {
                    setProperty(
                        PropertyName.FAGSYSTEM,
                        FagsystemUtil.hentFagsystemFraYtelsestype(kravgrunnlagPåFagsak.ytelsestype).name,
                    )
                },
        ).medTriggerTid(triggerTid)
    }

    companion object {
        val KONTROLLFELT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

        val RELEVANTE_TASK_STATUSER =
            listOf(
                Status.FEILET,
                Status.KLAR_TIL_PLUKK,
                Status.MANUELL_OPPFØLGING,
            )

        val ALDERSGRENSE_I_UKER =
            mapOf<Ytelsestype, Long>(
                BARNETRYGD to 8,
                BARNETILSYN to 8,
                OVERGANGSSTØNAD to 8,
                SKOLEPENGER to 8,
                KONTANTSTØTTE to 8,
            )
    }
}
