package no.nav.familie.tilbake.kravgrunnlag.batch

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.BARNETILSYN
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.BARNETRYGD
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.KONTANTSTØTTE
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.OVERGANGSSTØNAD
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.SKOLEPENGER
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class HåndterGamleKravgrunnlagBatch(private val mottattXmlService: ØkonomiXmlMottattService,
                                    private val taskService: TaskService,
                                    private val environment: Environment) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "\${CRON_HÅNDTER_GAMMEL_KRAVGRUNNLAG}")
    @Transactional
    fun utfør() {
        when {
            LeaderClient.isLeader() == true || environment.activeProfiles.any {
                it.contains("local") ||
                it.contains("integrasjonstest")
            } -> {
                logger.info("Starter HåndterGamleKravgrunnlagBatch..")
                logger.info("Henter gamle kravgrunnlag som er gamle enn $ALDERSGRENSE_I_UKER")
                val mottattXmlIds: List<UUID> =
                        mottattXmlService.hentFrakobletGamleMottattXmlIds(barnetrygdBestemtDato = beregnBestemtDato(BARNETRYGD),
                                                                          barnetilsynBestemtDato = beregnBestemtDato(BARNETILSYN),
                                                                          overgangsstønadBestemtDato = beregnBestemtDato(
                                                                                  OVERGANGSSTØNAD),
                                                                          skolePengerBestemtDato = beregnBestemtDato(SKOLEPENGER),
                                                                          kontantStøtteBestemtDato = beregnBestemtDato(
                                                                                  KONTANTSTØTTE))

                when {
                    mottattXmlIds.isEmpty() -> {
                        logger.info("Det finnes ingen gamle kravgrunnlag som er gamle enn $ALDERSGRENSE_I_UKER fra dagens dato")
                    }
                    else -> {
                        logger.info("Det finnes ${mottattXmlIds.size} kravgrunnlag som er gamle enn " +
                                    "$ALDERSGRENSE_I_UKER fra dagens dato")
                        logger.info("Oppretter tasker for å håndtere enkel kravgrunnlag")
                        mottattXmlIds.forEach {
                            taskService.save(Task(type = HåndterGammelKravgrunnlagTask.TYPE, payload = it.toString()))
                        }
                    }
                }
                logger.info("Stopper HåndterGamleKravgrunnlagBatch..")
            }
            false -> logger.info("Poden er ikke satt opp som leader - kjører ikke HåndterGamleKravgrunnlagBatch")
        }
    }

    private fun beregnBestemtDato(ytelsestype: Ytelsestype): LocalDate {
        return LocalDate.now().minusWeeks(ALDERSGRENSE_I_UKER.getValue(ytelsestype))
    }

    companion object {

        val ALDERSGRENSE_I_UKER = mapOf<Ytelsestype, Long>(BARNETRYGD to 8,
                                                           BARNETILSYN to 6,
                                                           OVERGANGSSTØNAD to 6,
                                                           SKOLEPENGER to 6,
                                                           KONTANTSTØTTE to 8)
    }

}