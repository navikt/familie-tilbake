package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class AutomatiskSaksbehandlingService(private val behandlingRepository: BehandlingRepository,
                                      private val fagsakRepository: FagsakRepository,
                                      private val kravgrunnlagRepository: KravgrunnlagRepository,
                                      private val brevsporingRepository: BrevsporingRepository,
                                      private val stegService: StegService) {

    fun hentAlleBehandlingerSomKanBehandleAutomatisk(): List<UUID> {
        val behandlinger = behandlingRepository.finnAlleBehandlingerKlarForSaksbehandling()
        return behandlinger.filter {
            val fagsak = fagsakRepository.findByIdOrThrow(it.fagsakId)
            val bestemtDato = LocalDate.now().minusWeeks(ALDERSGRENSE_I_UKER.getValue(fagsak.ytelsestype))
            val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(it.id)
            val kontrollFelt = LocalDate.parse(kravgrunnlag.kontrollfelt,
                                               DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS"))
            val sumNyttBeløp: BigDecimal =
                    kravgrunnlag.perioder.sumOf { periode -> periode.beløp.sumOf { beløp -> beløp.nyttBeløp } }

            kontrollFelt < bestemtDato &&
            sumNyttBeløp < Constants.MAKS_FEILUTBETALTBELØP_PER_YTELSE.getValue(fagsak.ytelsestype) &&
            // behandlinger som ikke sendte brev
            !brevsporingRepository.existsByBehandlingId(it.id)
        }.map { it.id }
    }

    @Transactional
    fun oppdaterBehandling(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(saksbehandlingstype = Saksbehandlingstype
                .AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP, ansvarligSaksbehandler = "VL"))
    }

    @Transactional
    fun behandleAutomatisk(behandlingId: UUID) {
        stegService.håndterStegAutomatisk(behandlingId)
    }

    companion object {

        val ALDERSGRENSE_I_UKER = mapOf<Ytelsestype, Long>(Ytelsestype.BARNETRYGD to 8,
                                                           Ytelsestype.BARNETILSYN to 6,
                                                           Ytelsestype.OVERGANGSSTØNAD to 6,
                                                           Ytelsestype.SKOLEPENGER to 6,
                                                           Ytelsestype.KONTANTSTØTTE to 8)
    }

}