package no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.behandling.ValiderBrevmottakerService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerRequestDto
import no.nav.tilbakekreving.arbeidsforhold.ArbeidsforholdService
import no.nav.tilbakekreving.config.FeatureService
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Service
class ManuellBrevmottakerService(
    private val manuellBrevmottakerRepository: ManuellBrevmottakerRepository,
    private val historikkService: HistorikkService,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val fagsakService: FagsakService,
    private val pdlClient: PdlClient,
    private val integrasjonerClient: IntegrasjonerClient,
    private val validerBrevmottakerService: ValiderBrevmottakerService,
    private val logService: LogService,
    private val featureService: FeatureService,
    private val arbeidsforholdService: ArbeidsforholdService,
) {
    @Transactional
    fun leggTilBrevmottaker(
        behandlingId: UUID,
        requestDto: ManuellBrevmottakerRequestDto,
    ): UUID {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val logContext = logService.contextFraBehandling(behandling.id)

        val navnFraRegister: String? =
            hentPersonEllerOrganisasjonNavnFraRegister(
                dto = requestDto,
                behandlingId = behandlingId,
                logContext = logContext,
            )
        val manuellBrevmottaker = ManuellBrevmottakerMapper.tilDomene(behandlingId, requestDto, navnFraRegister)
        validerBrevmottakerService.validerAtBehandlingenIkkeInneholderStrengtFortroligPerson(behandlingId = behandling.id, fagsakId = behandling.fagsakId)
        val id = manuellBrevmottakerRepository.insert(manuellBrevmottaker).id
        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_LAGT_TIL,
            aktør = Aktør.Saksbehandler.fraBehandling(behandlingId, behandlingRepository),
            opprettetTidspunkt = LocalDateTime.now(),
            beskrivelse = lagHistorikkBeskrivelseForBrevmottaker(manuellBrevmottaker),
            tittel = "${manuellBrevmottaker.type.visningsnavn} er lagt til som brevmottaker",
        )
        return id
    }

    fun hentBrevmottakere(behandlingId: UUID) = manuellBrevmottakerRepository.findByBehandlingId(behandlingId)

    @Transactional
    fun oppdaterBrevmottaker(
        behandlingId: UUID,
        manuellBrevmottakerId: UUID,
        manuellBrevmottakerRequestDto: ManuellBrevmottakerRequestDto,
    ) {
        val logContext = logService.contextFraBehandling(behandlingId)
        val manuellBrevmottaker =
            manuellBrevmottakerRepository.findById(manuellBrevmottakerId).getOrNull()
                ?: throw Feil(
                    message = "Finnes ikke brevmottakere med id=$manuellBrevmottakerId",
                    logContext = logContext,
                )

        val oppdatertBrevmottaker =
            manuellBrevmottaker.copy(
                type = manuellBrevmottakerRequestDto.type,
                navn =
                    hentPersonEllerOrganisasjonNavnFraRegister(manuellBrevmottakerRequestDto, behandlingId, logContext)
                        ?: manuellBrevmottakerRequestDto.navn,
                ident = manuellBrevmottakerRequestDto.personIdent,
                orgNr = manuellBrevmottakerRequestDto.organisasjonsnummer,
                adresselinje1 = manuellBrevmottakerRequestDto.manuellAdresseInfo?.adresselinje1,
                adresselinje2 = manuellBrevmottakerRequestDto.manuellAdresseInfo?.adresselinje2,
                postnummer = manuellBrevmottakerRequestDto.manuellAdresseInfo?.postnummer,
                poststed = manuellBrevmottakerRequestDto.manuellAdresseInfo?.poststed,
                landkode = manuellBrevmottakerRequestDto.manuellAdresseInfo?.landkode,
                vergetype = manuellBrevmottakerRequestDto.vergetype,
            )

        val historikkinnslagtittel =
            if (manuellBrevmottaker.type == oppdatertBrevmottaker.type) {
                "${manuellBrevmottaker.type.visningsnavn} er endret"
            } else {
                "${manuellBrevmottaker.type.visningsnavn} er endret til ${oppdatertBrevmottaker.type.visningsnavn}"
            }

        historikkService.lagHistorikkinnslag(
            behandlingId = manuellBrevmottaker.behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_ENDRET,
            aktør = Aktør.Saksbehandler.fraBehandling(behandlingId, behandlingRepository),
            opprettetTidspunkt = LocalDateTime.now(),
            beskrivelse = lagHistorikkBeskrivelseForBrevmottaker(oppdatertBrevmottaker),
            tittel = historikkinnslagtittel,
        )

        manuellBrevmottakerRepository.update(oppdatertBrevmottaker)
    }

    @Transactional
    fun fjernBrevmottaker(
        behandlingId: UUID,
        manuellBrevmottakerId: UUID,
    ) {
        val logContext = logService.contextFraBehandling(behandlingId)
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val manuellBrevmottakere = manuellBrevmottakerRepository.findByBehandlingId(behandlingId)
        if (manuellBrevmottakere.none { it.id == manuellBrevmottakerId }) {
            throw Feil(
                message = "Finnes ikke brevmottakere med id=$manuellBrevmottakerId for behandlingId=$behandlingId",
                logContext = logContext,
            )
        }
        fjernBrevmottakerOgLagHistorikkinnslag(
            manuellBrevmottakere.single { it.id == manuellBrevmottakerId },
            behandlingId,
            Aktør.Saksbehandler.fraBehandling(behandlingId, behandlingRepository),
        )
    }

    @Transactional
    fun opprettBrevmottakerSteg(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val logContext = logService.contextFraBehandling(behandling.id)
        validerBrevmottakerStegopprettelse(behandling, logContext)
        behandlingskontrollService.behandleBrevmottakerSteg(behandlingId, logContext)
    }

    @Transactional
    fun fjernManuelleBrevmottakereOgTilbakeførSteg(behandlingId: UUID) {
        val logContext = logService.contextFraBehandling(behandlingId)
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        hentBrevmottakere(behandlingId).forEach { manuellBrevmottaker ->
            fjernBrevmottakerOgLagHistorikkinnslag(manuellBrevmottaker, behandlingId, Aktør.Saksbehandler.fraBehandling(behandlingId, behandlingRepository))
        }

        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(Behandlingssteg.BREVMOTTAKER, Behandlingsstegstatus.TILBAKEFØRT),
            logContext,
        )
        behandlingskontrollService.fortsettBehandling(behandlingId, logContext)
    }

    private fun fjernBrevmottakerOgLagHistorikkinnslag(
        manuellBrevmottaker: ManuellBrevmottaker,
        behandlingId: UUID,
        aktør: Aktør,
    ) {
        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_FJERNET,
            aktør = aktør,
            opprettetTidspunkt = LocalDateTime.now(),
            beskrivelse = lagHistorikkBeskrivelseForBrevmottaker(manuellBrevmottaker),
            tittel = "${manuellBrevmottaker.type.visningsnavn} er fjernet som brevmottaker",
        )

        manuellBrevmottakerRepository.deleteById(manuellBrevmottaker.id)
    }

    private fun validerBrevmottakerStegopprettelse(
        behandling: Behandling,
        logContext: SecureLog.Context,
    ) {
        if (behandling.erSaksbehandlingAvsluttet) {
            throw Feil(
                "Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                frontendFeilmelding = "Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                httpStatus = HttpStatus.BAD_REQUEST,
                logContext = logContext,
            )
        }
        if (behandlingskontrollService.erBehandlingPåVent(behandling.id)) {
            throw Feil(
                "Behandling med id=${behandling.id} er på vent.",
                frontendFeilmelding = "Behandling med id=${behandling.id} er på vent.",
                httpStatus = HttpStatus.BAD_REQUEST,
                logContext = logContext,
            )
        }
        validerBrevmottakerService.validerAtBehandlingenIkkeInneholderStrengtFortroligPerson(behandlingId = behandling.id, fagsakId = behandling.fagsakId)
    }

    private fun lagHistorikkBeskrivelseForBrevmottaker(brevmottaker: ManuellBrevmottaker) =
        listOf(
            brevmottaker.navn,
            brevmottaker.adresselinje1,
            brevmottaker.adresselinje2,
            brevmottaker.postnummer,
            brevmottaker.poststed,
            brevmottaker.landkode,
        ).filter { !it.isNullOrEmpty() }.joinToString(separator = System.lineSeparator())

    private fun hentPersonEllerOrganisasjonNavnFraRegister(
        dto: ManuellBrevmottakerRequestDto,
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ): String? =
        dto.personIdent?.let {
            pdlClient
                .hentPersoninfo(
                    ident = it,
                    fagsystem = fagsakService.finnFagsystemForBehandlingId(behandlingId).tilDTO(),
                    logContext = logContext,
                ).navn
        } ?: dto.organisasjonsnummer?.let {
            val erGyldig = if (featureService.modellFeatures[Toggle.EregServices]) {
                arbeidsforholdService.validerOrganisasjon(it)
            } else {
                integrasjonerClient.validerOrganisasjon(it)
            }
            if (!erGyldig) {
                throw Feil(
                    message = "Organisasjon $it er ikke gyldig",
                    frontendFeilmelding = "Organisasjon $it er ikke gyldig",
                    logContext = logContext,
                )
            }
            if (featureService.modellFeatures[Toggle.EregServices]) {
                "${arbeidsforholdService.hentOrganisasjon(it).navn}${if (dto.navn.isNotBlank()) " v/ ${dto.navn}" else ""}"
            } else {
                "${integrasjonerClient.hentOrganisasjon(it).navn}${if (dto.navn.isNotBlank()) " v/ ${dto.navn}" else ""}"
            }
        }
}
