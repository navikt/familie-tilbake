package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.FeatureToggleConfig
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.kontrakter.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.familie.tilbake.log.SecureLog
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class ValiderBehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val featureToggleService: FeatureToggleService,
    private val økonomiXmlMottattRepository: ØkonomiXmlMottattRepository,
) {
    fun validerOpprettBehandling(request: OpprettTilbakekrevingRequest) {
        val logContext = SecureLog.Context.utenBehandling(request.eksternFagsakId)
        if (FagsystemUtil.hentFagsystemFraYtelsestype(request.ytelsestype) != request.fagsystem) {
            throw Feil(
                message = "Behandling kan ikke opprettes med ytelsestype=${request.ytelsestype} og fagsystem=${request.fagsystem}",
                frontendFeilmelding = "Behandling kan ikke opprettes med ytelsestype=${request.ytelsestype} og fagsystem=${request.fagsystem}",
                logContext = logContext,
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }

        val åpenTilbakekrevingsbehandling: Behandling? =
            behandlingRepository.finnÅpenTilbakekrevingsbehandling(request.ytelsestype, request.eksternFagsakId)
        if (åpenTilbakekrevingsbehandling != null) {
            val feilMelding =
                "Det finnes allerede en åpen behandling for ytelsestype=${request.ytelsestype} " +
                    "og eksternFagsakId=${request.eksternFagsakId}, kan ikke opprette en ny."
            throw Feil(
                message = feilMelding,
                frontendFeilmelding = feilMelding,
                logContext = logContext,
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }

        // hvis behandlingen er henlagt, kan det opprettes ny behandling
        // hvis toggelen KAN_OPPRETTE_BEH_MED_EKSTERNID_SOM_HAR_AVSLUTTET_TBK er på,
        // sjekker ikke om det finnes en avsluttet tilbakekreving for eksternId
        if (!featureToggleService.isEnabled(FeatureToggleConfig.KAN_OPPRETTE_BEH_MED_EKSTERNID_SOM_HAR_AVSLUTTET_TBK)) {
            val avsluttetBehandlinger = behandlingRepository.finnAvsluttetTilbakekrevingsbehandlinger(request.eksternId, request.fagsystem)
            if (avsluttetBehandlinger.isNotEmpty()) {
                val sisteAvsluttetBehandling: Behandling = avsluttetBehandlinger.first()
                val erSisteBehandlingHenlagt: Boolean =
                    sisteAvsluttetBehandling.resultater.any { it.erBehandlingHenlagt() }
                if (!erSisteBehandlingHenlagt) {
                    val feilMelding =
                        "Det finnes allerede en avsluttet behandling for ytelsestype=${request.ytelsestype} " +
                            "og eksternFagsakId=${request.eksternFagsakId} som ikke er henlagt, kan ikke opprette en ny."
                    throw Feil(
                        message = feilMelding,
                        frontendFeilmelding = feilMelding,
                        logContext = logContext,
                        httpStatus = HttpStatus.BAD_REQUEST,
                    )
                }
            }
        }

        // uten kravgrunnlag er det ikke mulig å opprette behandling manuelt
        if (request.manueltOpprettet &&
            !økonomiXmlMottattRepository
                .existsByEksternFagsakIdAndYtelsestypeAndReferanse(request.eksternFagsakId, request.ytelsestype, request.eksternId)
        ) {
            val feilMelding =
                "Det finnes intet kravgrunnlag for ytelsestype=${request.ytelsestype},eksternFagsakId=${request.eksternFagsakId} " +
                    "og eksternId=${request.eksternId}. Tilbakekrevingsbehandling kan ikke opprettes manuelt."
            throw Feil(message = feilMelding, frontendFeilmelding = feilMelding, logContext = logContext)
        }
    }
}
