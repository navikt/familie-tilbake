package no.nav.familie.tilbake.dokumentbestilling.felles.pdf

import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BrevsporingService(private val brevsporingRepository: BrevsporingRepository) {


    fun lagreInfoOmUtsendtBrev(behandlingId: UUID,
                               dokumentId: String,
                               journalpostId: String,
                               brevtype: Brevtype) {
        val brevSporing = Brevsporing(behandlingId = behandlingId,
                                      dokumentId = dokumentId,
                                      journalpostId = journalpostId,
                                      brevtype = brevtype)
        brevsporingRepository.insert(brevSporing)
    }

    fun finnSisteVarsel(behandlingId: UUID): Brevsporing? {
        return brevsporingRepository.findFirstByBehandlingIdAndBrevtypeOrderBySporbarOpprettetTidDesc(behandlingId,
                                                                                                      Brevtype.VARSEL)

    }
}