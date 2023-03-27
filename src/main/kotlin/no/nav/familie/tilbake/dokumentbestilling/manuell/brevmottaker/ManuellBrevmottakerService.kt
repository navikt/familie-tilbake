package no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker

import no.nav.familie.kontrakter.felles.dokdist.AdresseType.norskPostadresse
import no.nav.familie.kontrakter.felles.dokdist.AdresseType.utenlandskPostadresse
import no.nav.familie.kontrakter.felles.dokdist.ManuellAdresse
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE
import no.nav.familie.tilbake.api.dto.ManuellBrevmottakerRequestDto
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Service
class ManuellBrevmottakerService(
    private val manuellBrevmottakerRepository: ManuellBrevmottakerRepository,
    private val historikkService: HistorikkService
) {

    @Transactional
    fun leggTilBrevmottaker(behandlingId: UUID, manuellBrevmottakerRequestDto: ManuellBrevmottakerRequestDto) {
        val manuellBrevmottaker = ManuellBrevmottakerMapper.tilDomene(behandlingId, manuellBrevmottakerRequestDto)
        manuellBrevmottakerRepository.insert(manuellBrevmottaker)
        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_LAGT_TIL,
            aktør = Aktør.SAKSBEHANDLER,
            opprettetTidspunkt = LocalDateTime.now()
        )
    }

    fun hentBrevmottakere(behandlingId: UUID) = manuellBrevmottakerRepository.findByBehandlingId(behandlingId)

    @Transactional
    fun oppdaterBrevmottaker(
        manuellBrevmottakerId: UUID,
        manuellBrevmottakerRequestDto: ManuellBrevmottakerRequestDto
    ) {
        val manuellBrevmottaker = manuellBrevmottakerRepository.findById(manuellBrevmottakerId).getOrNull()
            ?: throw Feil("Finnes ikke brevmottakere med id=$manuellBrevmottakerId")

        manuellBrevmottakerRepository.update(
            manuellBrevmottaker.copy(
                type = manuellBrevmottakerRequestDto.type,
                navn = manuellBrevmottakerRequestDto.navn,
                ident = manuellBrevmottakerRequestDto.personIdent,
                orgNr = manuellBrevmottakerRequestDto.organisasjonsnummer,
                adresselinje1 = manuellBrevmottakerRequestDto.manuellAdresseInfo?.adresselinje1,
                adresselinje2 = manuellBrevmottakerRequestDto.manuellAdresseInfo?.adresselinje2,
                postnummer = manuellBrevmottakerRequestDto.manuellAdresseInfo?.postnummer,
                poststed = manuellBrevmottakerRequestDto.manuellAdresseInfo?.poststed,
                landkode = manuellBrevmottakerRequestDto.manuellAdresseInfo?.landkode,
                vergetype = manuellBrevmottakerRequestDto.vergetype
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

fun List<ManuellBrevmottaker>.toManuelleAdresser(): List<ManuellAdresse> =
    this.mapNotNull { manuellBrevmottaker ->
        if (manuellBrevmottaker.hasManuellAdresse()) {
            val addresseType = when {
                manuellBrevmottaker.type == BRUKER_MED_UTENLANDSK_ADRESSE -> utenlandskPostadresse
                else -> norskPostadresse
            }

            ManuellAdresse(
                adresseType = addresseType,
                adresselinje1 = manuellBrevmottaker.adresselinje1,
                adresselinje2 = manuellBrevmottaker.adresselinje2,
                postnummer = manuellBrevmottaker.postnummer,
                poststed = manuellBrevmottaker.poststed,
                land = manuellBrevmottaker.landkode!!
            )
        } else {
            null
        }
    }
