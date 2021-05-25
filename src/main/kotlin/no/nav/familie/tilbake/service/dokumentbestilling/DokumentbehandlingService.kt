package no.nav.familie.tilbake.service.dokumentbestilling

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.service.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon.InnhentDokumentasjonbrevService
import no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon.InnhentDokumentasjonbrevTask
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.manuelt.ManueltVarselbrevService
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.manuelt.SendManueltVarselbrevTask
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.Properties
import java.util.UUID

@Service
@Transactional
class DokumentbehandlingService(private val behandlingRepository: BehandlingRepository,
                                private val behandlingskontrollService: BehandlingskontrollService,
                                private val kravgrunnlagRepository: KravgrunnlagRepository,
                                private val taskService: TaskService,
                                private val manueltVarselBrevService: ManueltVarselbrevService,
                                private val innhentDokumentasjonBrevService: InnhentDokumentasjonbrevService) {

    fun bestillBrev(behandlingId: UUID, maltype: Dokumentmalstype, fritekst: String) {
        val behandling: Behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (Dokumentmalstype.VARSEL == maltype || Dokumentmalstype.KORRIGERT_VARSEL == maltype) {
            håndterManueltSendVarsel(behandling, maltype, fritekst)
        } else if (Dokumentmalstype.INNHENT_DOKUMENTASJON == maltype) {
            håndterInnhentDokumentasjon(behandling, fritekst)
        }
    }


    fun forhåndsvisBrev(behandlingId: UUID, maltype: Dokumentmalstype, fritekst: String): ByteArray {
        var dokument = ByteArray(0)
        if (Dokumentmalstype.VARSEL == maltype || Dokumentmalstype.KORRIGERT_VARSEL == maltype) {
            dokument = manueltVarselBrevService.hentForhåndsvisningManueltVarselbrev(behandlingId, maltype, fritekst)
        } else if (Dokumentmalstype.INNHENT_DOKUMENTASJON == maltype) {
            dokument = innhentDokumentasjonBrevService.hentForhåndsvisningInnhentDokumentasjonBrev(behandlingId, fritekst)
        }
        return dokument
    }

    private fun håndterManueltSendVarsel(behandling: Behandling, maltype: Dokumentmalstype, fritekst: String) {

        if (!kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id)) {
            error("Kan ikke sende varselbrev fordi grunnlag finnes ikke for behandlingId = ${behandling.id}")
        }
        val sendVarselbrev = Task(type = SendManueltVarselbrevTask.TYPE,
                                  payload = behandling.id.toString(),
                                  properties = Properties().apply {
                                      setProperty("maltype", maltype.name)
                                      setProperty("fritekst", fritekst)
                                  })
        taskService.save(sendVarselbrev)
        settPåVent(behandling)
    }

    private fun settPåVent(behandling: Behandling) {
        val fristTid = LocalDate.now().plusWeeks(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING.defaultVenteTidIUker)
        behandlingskontrollService.settBehandlingPåVent(behandling.id,
                                                        Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                                                        fristTid)
    }


    private fun håndterInnhentDokumentasjon(behandling: Behandling, fritekst: String) {
        if (!kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id)) {
            error("Kan ikke sende innhent dokumentasjonsbrev fordi grunnlag finnes ikke for behandlingId = ${behandling.id}")
        }
        val sendInnhentDokumentasjonBrev = Task(InnhentDokumentasjonbrevTask.TYPE,
                                                behandling.id.toString(),
                                                Properties().apply { setProperty("fritekst", fritekst) })
        taskService.save(sendInnhentDokumentasjonBrev)
        settPåVent(behandling)
    }
}
