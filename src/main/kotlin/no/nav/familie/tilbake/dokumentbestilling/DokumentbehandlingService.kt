package no.nav.familie.tilbake.dokumentbestilling

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon.InnhentDokumentasjonbrevService
import no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon.InnhentDokumentasjonbrevTask
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.BrevmottakerAdresseValidering
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.dokumentbestilling.varsel.manuelt.ManueltVarselbrevService
import no.nav.familie.tilbake.dokumentbestilling.varsel.manuelt.SendManueltVarselbrevTask
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.brev.Dokumentmalstype
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class DokumentbehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val taskService: TracableTaskService,
    private val manueltVarselBrevService: ManueltVarselbrevService,
    private val innhentDokumentasjonBrevService: InnhentDokumentasjonbrevService,
    private val manuellBrevmottakerRepository: ManuellBrevmottakerRepository,
    private val logService: LogService,
) {
    fun bestillBrev(
        behandlingId: UUID,
        maltype: Dokumentmalstype,
        fritekst: String,
    ) {
        val behandling: Behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val logContext = logService.contextFraBehandling(behandling.id)
        val ansvarligSaksbehandler = ContextService.hentSaksbehandler(logContext)

        val manuelleBrevmottakere = manuellBrevmottakerRepository.findByBehandlingId(behandlingId)
        if (!BrevmottakerAdresseValidering.erBrevmottakereGyldige(manuelleBrevmottakere)) {
            throw Feil(
                message = "Det finnes ugyldige brevmottakere i utsending av manuelt brev",
                frontendFeilmelding = "Adressen som er lagt til manuelt har ugyldig format, og brevet kan ikke sendes. Du må legge til manuell adresse på nytt.",
                logContext = logContext,
            )
        }

        if (behandling.ansvarligSaksbehandler != ansvarligSaksbehandler) {
            behandlingRepository.update(behandling.copy(ansvarligSaksbehandler = ansvarligSaksbehandler))
        }
        if (Dokumentmalstype.VARSEL == maltype || Dokumentmalstype.KORRIGERT_VARSEL == maltype) {
            opprettSendManueltVarselbrevTaskOgSettPåVent(behandling, maltype, fritekst, logContext)
        } else if (Dokumentmalstype.INNHENT_DOKUMENTASJON == maltype) {
            håndterInnhentDokumentasjon(behandling, fritekst, logContext)
        }
    }

    fun forhåndsvisBrev(
        behandlingId: UUID,
        maltype: Dokumentmalstype,
        fritekst: String,
    ): ByteArray {
        var dokument = ByteArray(0)
        if (Dokumentmalstype.VARSEL == maltype || Dokumentmalstype.KORRIGERT_VARSEL == maltype) {
            dokument = manueltVarselBrevService.hentForhåndsvisningManueltVarselbrev(behandlingId, maltype, fritekst)
        } else if (Dokumentmalstype.INNHENT_DOKUMENTASJON == maltype) {
            dokument = innhentDokumentasjonBrevService.hentForhåndsvisningInnhentDokumentasjonBrev(behandlingId, fritekst)
        }
        return dokument
    }

    private fun opprettSendManueltVarselbrevTaskOgSettPåVent(
        behandling: Behandling,
        maltype: Dokumentmalstype,
        fritekst: String,
        logContext: SecureLog.Context,
    ) {
        if (!kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id)) {
            error("Kan ikke sende varselbrev fordi grunnlag finnes ikke for behandlingId = ${behandling.id}")
        }
        val fagsystem = fagsakRepository.findByIdOrThrow(behandling.fagsakId).fagsystem
        val sendVarselbrev =
            SendManueltVarselbrevTask.opprettTask(behandling.id, fagsystem, maltype, fritekst)
        taskService.save(sendVarselbrev, logContext)
        settPåVent(behandling, logContext)
    }

    private fun settPåVent(
        behandling: Behandling,
        logContext: SecureLog.Context,
    ) {
        val tidsfrist = Constants.saksbehandlersTidsfrist()
        behandlingskontrollService.settBehandlingPåVent(
            behandling.id,
            Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
            tidsfrist,
            logContext,
        )
    }

    private fun håndterInnhentDokumentasjon(
        behandling: Behandling,
        fritekst: String,
        logContext: SecureLog.Context,
    ) {
        if (!kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id)) {
            error("Kan ikke sende innhent dokumentasjonsbrev fordi grunnlag finnes ikke for behandlingId = ${behandling.id}")
        }
        val fagsystem = fagsakRepository.findByIdOrThrow(behandling.fagsakId).fagsystem
        val sendInnhentDokumentasjonBrev =
            InnhentDokumentasjonbrevTask.opprettTask(behandling.id, fagsystem, fritekst)
        taskService.save(sendInnhentDokumentasjonBrev, logContext)
        settPåVent(behandling, logContext)
    }
}
