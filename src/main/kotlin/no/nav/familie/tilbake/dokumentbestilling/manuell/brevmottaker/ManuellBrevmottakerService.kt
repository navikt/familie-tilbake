package no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.tilbake.api.dto.ManuellBrevmottakerDto
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class ManuellBrevmottakerService(
    private val manuellBrevmottakerRepository: ManuellBrevmottakerRepository,
    private val historikkService: HistorikkService
) {

    @Transactional
    fun leggTilBrevmottaker(behandlingId: UUID, manuellBrevmottakerDto: ManuellBrevmottakerDto) {
        val manuellBrevmottaker = ManuellBrevmottakerMapper.tilDomene(behandlingId, manuellBrevmottakerDto)
        manuellBrevmottakerRepository.insert(manuellBrevmottaker)
        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_LAGT_TIL,
            aktør = Aktør.SAKSBEHANDLER,
            opprettetTidspunkt = LocalDateTime.now()
        )
    }

    fun hentBrevmottakere(behandlingId: UUID) =
        manuellBrevmottakerRepository.findByBehandlingId(behandlingId).map { ManuellBrevmottakerMapper.tilRespons(it) }

    @Transactional
    fun oppdaterBrevmottaker(
        behandlingId: UUID,
        manuellBrevmottakerId: UUID,
        manuellBrevmottakerDto: ManuellBrevmottakerDto
    ) {
        val manuellBrevmottakere = manuellBrevmottakerRepository.findByBehandlingId(behandlingId)
        if (manuellBrevmottakere.none { it.id == manuellBrevmottakerId }) {
            throw Feil("Finnes ikke brevmottakere med id=$manuellBrevmottakerId for behandlingId=$behandlingId")
        }
        manuellBrevmottakerRepository.update(
            manuellBrevmottakere.single { it.id == manuellBrevmottakerId }.copy(
                type = manuellBrevmottakerDto.type,
                navn = manuellBrevmottakerDto.navn,
                adresselinje1 = manuellBrevmottakerDto.adresselinje1,
                adresselinje2 = manuellBrevmottakerDto.adresselinje2,
                postnummer = manuellBrevmottakerDto.postnummer,
                poststed = manuellBrevmottakerDto.poststed,
                landkode = manuellBrevmottakerDto.landkode
            )
        )
    }

    @Transactional
    fun fjernBrevmottaker(behandlingId: UUID, manuellBrevmottakerId: UUID) {
        val manuellBrevmottakere = manuellBrevmottakerRepository.findByBehandlingId(behandlingId)
        if (manuellBrevmottakere.none { it.id == manuellBrevmottakerId }) {
            throw Feil("Finnes ikke brevmottakere med id=$manuellBrevmottakerId for behandlingId=$behandlingId")
        }
        manuellBrevmottakerRepository.deleteById(manuellBrevmottakerId)
        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_FJERNET,
            aktør = Aktør.SAKSBEHANDLER,
            opprettetTidspunkt = LocalDateTime.now()
        )
    }
}
