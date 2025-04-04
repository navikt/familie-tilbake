package no.nav.familie.tilbake.totrinn

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.totrinn.domain.Totrinnsvurdering
import no.nav.tilbakekreving.api.v1.dto.TotrinnsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.VurdertTotrinnDto
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class TotrinnService(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository,
    private val totrinnsvurderingRepository: TotrinnsvurderingRepository,
) {
    @Transactional(readOnly = true)
    fun hentTotrinnsvurderinger(behandlingId: UUID): TotrinnsvurderingDto {
        val totrinnsvurderinger = totrinnsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        return TotrinnMapper.tilRespons(totrinnsvurderinger, behandlingsstegstilstand)
    }

    @Transactional
    fun lagreTotrinnsvurderinger(
        behandlingId: UUID,
        totrinnsvurderinger: List<VurdertTotrinnDto>,
        logContext: SecureLog.Context,
    ) {
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        // valider request
        validerOmAlleBesluttendeStegFinnes(totrinnsvurderinger, behandlingsstegstilstand, logContext)

        // deaktiver eksisterende totrinnsvurderinger
        val eksisterendeTotrinnsvurdering = totrinnsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        eksisterendeTotrinnsvurdering.forEach { totrinnsvurderingRepository.update(it.copy(aktiv = false)) }

        totrinnsvurderinger
            .filter { finnOmStegKanBesluttes(it.behandlingssteg, behandlingsstegstilstand) }
            .forEach {
                totrinnsvurderingRepository.insert(
                    Totrinnsvurdering(
                        behandlingId = behandlingId,
                        behandlingssteg = it.behandlingssteg,
                        godkjent = it.godkjent,
                        begrunnelse = it.begrunnelse,
                    ),
                )
            }
    }

    @Transactional
    fun lagreFastTotrinnsvurderingerForAutomatiskSaksbehandling(behandlingId: UUID) {
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        val totrinnsvurderinger =
            behandlingsstegstilstand.filter { it.behandlingssteg.kanBesluttes }.map {
                Totrinnsvurdering(
                    behandlingId = behandlingId,
                    behandlingssteg = it.behandlingssteg,
                    godkjent = true,
                    begrunnelse = Constants.AUTOMATISK_SAKSBEHANDLING_BEGRUNNELSE,
                )
            }
        totrinnsvurderinger.forEach { totrinnsvurderingRepository.insert(it) }
    }

    fun validerAnsvarligBeslutter(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.ansvarligSaksbehandler == ContextService.hentSaksbehandler(logContext)) {
            throw Feil(
                message = "ansvarlig beslutter kan ikke være samme som ansvarlig saksbehandler",
                frontendFeilmelding = "ansvarlig beslutter kan ikke være samme som ansvarlig saksbehandler",
                logContext = logContext,
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    fun finnesUnderkjenteStegITotrinnsvurdering(behandlingId: UUID): Boolean = totrinnsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId).any { !it.godkjent }

    @Transactional
    fun oppdaterAnsvarligBeslutter(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(ansvarligBeslutter = ContextService.hentSaksbehandler(logContext)))
    }

    @Transactional
    fun fjernAnsvarligBeslutter(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(ansvarligBeslutter = null))
    }

    fun finnForrigeBeslutterMedNyVurderingEllerNull(behandlingId: UUID): String? =
        totrinnsvurderingRepository
            .findByBehandlingIdAndAktivIsTrue(behandlingId)
            .find { !it.godkjent }
            ?.takeIf { erEndretTidUnder1Mnd(it) }
            ?.sporbar
            ?.endret
            ?.endretAv

    private fun erEndretTidUnder1Mnd(totrinnsvurdering: Totrinnsvurdering): Boolean {
        val endretTid =
            totrinnsvurdering.sporbar.endret.endretTid
                .toLocalDate()
        return endretTid.isAfter(LocalDate.now().minusMonths(1))
    }

    private fun finnOmStegKanBesluttes(
        behandlingssteg: Behandlingssteg,
        behandlingsstegstilstand: List<Behandlingsstegstilstand>,
    ): Boolean =
        behandlingsstegstilstand.any {
            behandlingssteg == it.behandlingssteg &&
                it.behandlingssteg.kanBesluttes &&
                it.behandlingsstegsstatus != no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus.AUTOUTFØRT
        }

    private fun validerOmAlleBesluttendeStegFinnes(
        totrinnsvurderinger: List<VurdertTotrinnDto>,
        behandlingsstegstilstand: List<Behandlingsstegstilstand>,
        logContext: SecureLog.Context,
    ) {
        val stegSomBørVurderes: List<Behandlingssteg> =
            behandlingsstegstilstand
                .filter {
                    it.behandlingssteg.kanBesluttes &&
                        it.behandlingsstegsstatus != no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus.AUTOUTFØRT
                }.map { it.behandlingssteg }

        val vurderteSteg: List<Behandlingssteg> = totrinnsvurderinger.map { it.behandlingssteg }
        val manglendeSteg = stegSomBørVurderes.minus(vurderteSteg)
        if (manglendeSteg.isNotEmpty()) {
            throw Feil(
                message = "Stegene $manglendeSteg mangler totrinnsvurdering",
                frontendFeilmelding = "Stegene $manglendeSteg mangler totrinnsvurdering",
                logContext = logContext,
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }
}
