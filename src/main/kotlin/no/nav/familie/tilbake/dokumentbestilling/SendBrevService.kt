package no.nav.familie.tilbake.dokumentbestilling

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata

import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class SendBrevService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val pdfBrevService: PdfBrevService,
    private val featureToggleService: FeatureToggleService
) {


    fun sendBrev(behandling: Behandling, brevtype: Brevtype, brevdata: (Brevmottager, Brevmetadata?) -> Brevdata) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)

        val brevmottakere = utledMottakere(behandling, fagsak)

        brevmottakere.forEach { brevmottaker ->
            val data = when (brevmottaker) {
                is BrevmottagerType -> brevdata(brevmottaker.type, null)
                is ManueltRegistrertMottaker -> { brevdata(Brevmottager.BRUKER, null)/*.retrofit(mottakertype)*/ }
            }
            pdfBrevService.sendBrev(
                behandling,
                fagsak,
                brevtype,
                data
            )
        }
    }

    private fun utledMottakere(behandling: Behandling, fagsak: Fagsak): List<Brevmottakere> {
        if (BehandlingService.sjekkOmManuelleBrevmottakereErStøttet(fagsak, featureToggleService)) {

        }
        TODO(
            "Utled brevmottakere fra manuelt registrerte brevmottakere, eller på gamelt vis dersom behandlingen ikke støtter det"
        )
    }

    fun genererMetadataForBrev(
        brevtype: Brevtype,
        behandlingId: UUID,
        brevmottager: Brevmottager = Brevmottager.MANUELL
    ): Brevmetadata? {
        // TODO
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        return null
    }
}

sealed interface Brevmottakere

class BrevmottagerType(val type: Brevmottager) : Brevmottakere
class ManueltRegistrertMottaker(val brevmottaker: ManuellBrevmottaker): Brevmottakere