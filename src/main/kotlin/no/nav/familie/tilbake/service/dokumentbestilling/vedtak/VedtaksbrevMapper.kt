package no.nav.familie.tilbake.service.dokumentbestilling.vedtak

import no.nav.familie.tilbake.api.dto.PeriodeMedTekstDto
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.domain.tbd.Fritekstavsnittstype
import no.nav.familie.tilbake.domain.tbd.Vedtaksbrevsperiode

object VedtaksbrevMapper {

    fun mapFritekstFraDb(fritekstPerioder: List<Vedtaksbrevsperiode>): List<PeriodeMedTekstDto> {

        val perioderTilMap = HashMap<Periode, MutableMap<Fritekstavsnittstype, String>>()

        fritekstPerioder.forEach {
            val avsnittTilTekst = perioderTilMap.getOrDefault(it.periode, mutableMapOf())
            avsnittTilTekst[it.fritekststype] = it.fritekst
            perioderTilMap[it.periode] = avsnittTilTekst
        }

        return perioderTilMap.entries.map { (periode, avsnittTilTekst) ->
            PeriodeMedTekstDto(periode = periode,
                               faktaAvsnitt = avsnittTilTekst[Fritekstavsnittstype.FAKTA],
                               foreldelseAvsnitt = avsnittTilTekst[Fritekstavsnittstype.FORELDELSE],
                               vilkårAvsnitt = avsnittTilTekst[Fritekstavsnittstype.VILKÅR],
                               særligeGrunnerAvsnitt = avsnittTilTekst[Fritekstavsnittstype.SÆRLIGE_GRUNNER],
                               særligeGrunnerAnnetAvsnitt = avsnittTilTekst[Fritekstavsnittstype.SÆRLIGE_GRUNNER_ANNET])
        }
    }

}