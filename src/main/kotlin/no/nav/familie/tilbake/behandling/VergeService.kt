package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kontrakter.Applikasjon
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.person.PersonService
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.api.v1.dto.VergeDto
import no.nav.tilbakekreving.arbeidsforhold.ArbeidsforholdService
import no.nav.tilbakekreving.config.FeatureService
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.verge.Vergetype
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class VergeService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val historikkService: HistorikkService,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val integrasjonerClient: IntegrasjonerClient,
    private val personService: PersonService,
    private val logService: LogService,
    private val featureService: FeatureService,
    private val arbeidsforholdService: ArbeidsforholdService,
) {
    @Transactional
    fun lagreVerge(
        behandlingId: UUID,
        vergeDto: VergeDto,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandling.id.toString())

        validerBehandling(behandling, logContext)
        validerVergeData(vergeDto, fagsak.fagsystem, logContext)

        val verge = tilDomene(vergeDto)
        val oppdatertBehandling = behandling.copy(verger = behandling.verger.map { it.copy(aktiv = false) }.toSet() + verge)
        behandlingRepository.update(oppdatertBehandling)
        historikkService.lagHistorikkinnslag(
            behandlingId = behandling.id,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.VERGE_OPPRETTET,
            aktør = Aktør.Saksbehandler.fraBehandling(behandlingId, behandlingRepository),
            opprettetTidspunkt = LocalDateTime.now(),
        )
    }

    @Transactional
    fun opprettVergeSteg(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val logContext = logService.contextFraBehandling(behandling.id)
        validerBehandling(behandling, logContext)
        behandlingskontrollService.behandleVergeSteg(behandlingId, logContext)
    }

    @Transactional
    fun fjernVerge(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val logContext = logService.contextFraBehandling(behandling.id)
        val finnesAktivVerge = behandling.harVerge

        if (finnesAktivVerge) {
            val oppdatertBehandling = behandling.copy(verger = behandling.verger.map { it.copy(aktiv = false) }.toSet())
            behandlingRepository.update(oppdatertBehandling)
            historikkService.lagHistorikkinnslag(
                behandling.id,
                TilbakekrevingHistorikkinnslagstype.VERGE_FJERNET,
                Aktør.Saksbehandler.fraBehandling(behandlingId, behandlingRepository),
                LocalDateTime.now(),
            )
        }
        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(
                Behandlingssteg.VERGE,
                Behandlingsstegstatus.TILBAKEFØRT,
            ),
            logContext,
        )
        behandlingskontrollService.fortsettBehandling(behandlingId, logContext)
    }

    @Transactional(readOnly = true)
    fun hentVerge(behandlingId: UUID): VergeDto? {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        return behandling.aktivVerge?.let { tilRespons(it) }
    }

    private fun validerBehandling(
        behandling: Behandling,
        logContext: SecureLog.Context,
    ) {
        if (behandling.erSaksbehandlingAvsluttet) {
            throw Feil(
                message = "Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                frontendFeilmelding = "Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                logContext = logContext,
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        if (behandlingskontrollService.erBehandlingPåVent(behandling.id)) {
            throw Feil(
                "Behandling med id=${behandling.id} er på vent.",
                frontendFeilmelding = "Behandling med id=${behandling.id} er på vent.",
                logContext = logContext,
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun validerVergeData(
        vergeDto: VergeDto,
        fagsystem: Fagsystem,
        logContext: SecureLog.Context,
    ) {
        when (vergeDto.type) {
            Vergetype.ADVOKAT -> {
                val orgnummer = requireNotNull(vergeDto.orgNr) { "orgNr kan ikke være null for ${Vergetype.ADVOKAT}" }
                val erGyldig = if (featureService.modellFeatures[Toggle.EregServices]) {
                    arbeidsforholdService.validerOrganisasjon(orgnummer)
                } else {
                    integrasjonerClient.validerOrganisasjon(orgnummer)
                }
                if (!erGyldig) {
                    throw Feil(
                        message = "Organisasjon ${vergeDto.orgNr} er ikke gyldig",
                        frontendFeilmelding = "Organisasjon ${vergeDto.orgNr} er ikke gyldig",
                        logContext = logContext,
                    )
                }
            }
            else -> {
                val ident = requireNotNull(vergeDto.ident) { "ident kan ikke være null for ${vergeDto.type}" }
                // Henter personen å verifisere om det finnes. Hvis det ikke finnes, kaster det en exception
                personService.hentPersoninfo(ident, fagsystem, logContext)
            }
        }
    }

    private fun tilDomene(vergeDto: VergeDto): Verge =
        Verge(
            ident = vergeDto.ident,
            orgNr = vergeDto.orgNr,
            aktiv = true,
            type = vergeDto.type,
            navn = vergeDto.navn,
            kilde = Applikasjon.FAMILIE_TILBAKE.name,
            begrunnelse = vergeDto.begrunnelse,
        )

    private fun tilRespons(verge: Verge): VergeDto =
        VergeDto(
            ident = verge.ident,
            orgNr = verge.orgNr,
            type = verge.type,
            navn = verge.navn,
            begrunnelse = verge.begrunnelse,
        )
}
