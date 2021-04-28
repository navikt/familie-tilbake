package no.nav.familie.tilbake.service.dokumentbestilling.vedtak

import no.nav.familie.tilbake.api.dto.PeriodeMedTekstDto
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.domain.tbd.Friteksttype
import no.nav.familie.tilbake.domain.tbd.Vedtaksbrevsperiode

object VedtaksbrevMapper {

    fun mapFritekstFraDb(fritekstPerioder: List<Vedtaksbrevsperiode>): List<PeriodeMedTekstDto> {

        val perioderTilMap = HashMap<Periode, MutableMap<Friteksttype, String>>()

        fritekstPerioder.forEach {
            val avsnittTilTekst = perioderTilMap.getOrDefault(it.periode, mutableMapOf())
            avsnittTilTekst[it.fritekststype] = it.fritekst
            perioderTilMap[it.periode] = avsnittTilTekst
        }

        return perioderTilMap.entries.map { (periode, avsnittTilTekst) ->
            PeriodeMedTekstDto(periode = periode.toDto(),
                               faktaAvsnitt = avsnittTilTekst[Friteksttype.FAKTA],
                               foreldelseAvsnitt = avsnittTilTekst[Friteksttype.FORELDELSE],
                               vilkårAvsnitt = avsnittTilTekst[Friteksttype.VILKÅR],
                               særligeGrunnerAvsnitt = avsnittTilTekst[Friteksttype.SÆRLIGE_GRUNNER],
                               særligeGrunnerAnnetAvsnitt = avsnittTilTekst[Friteksttype.SÆRLIGE_GRUNNER_ANNET])
        }
    }

}