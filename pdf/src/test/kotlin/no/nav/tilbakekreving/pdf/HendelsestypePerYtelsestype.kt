package no.nav.tilbakekreving.pdf

import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO

object HendelsestypePerYtelsestype {
    private val HIERARKI =
        mapOf(
            YtelsestypeDTO.BARNETRYGD to
                setOf(
                    Hendelsestype.ANNET,
                    Hendelsestype.SATSER,
                    Hendelsestype.SMÅBARNSTILLEGG,
                    Hendelsestype.MEDLEMSKAP_BA,
                    Hendelsestype.BOR_MED_SØKER,
                    Hendelsestype.DØDSFALL,
                    Hendelsestype.BOSATT_I_RIKET,
                    Hendelsestype.LOVLIG_OPPHOLD,
                    Hendelsestype.DELT_BOSTED,
                    Hendelsestype.BARNS_ALDER,
                    Hendelsestype.UTVIDET,
                ),
            YtelsestypeDTO.OVERGANGSSTØNAD to
                setOf(
                    Hendelsestype.ANNET,
                    Hendelsestype.MEDLEMSKAP,
                    Hendelsestype.OPPHOLD_I_NORGE,
                    Hendelsestype.ENSLIG_FORSØRGER,
                    Hendelsestype.OVERGANGSSTØNAD,
                    Hendelsestype.YRKESRETTET_AKTIVITET,
                    Hendelsestype.STØNADSPERIODE,
                    Hendelsestype.INNTEKT,
                    Hendelsestype.PENSJONSYTELSER,
                    Hendelsestype.DØDSFALL,
                ),
            YtelsestypeDTO.BARNETILSYN to
                setOf(
                    Hendelsestype.ANNET,
                    Hendelsestype.MEDLEMSKAP,
                    Hendelsestype.OPPHOLD_I_NORGE,
                    Hendelsestype.ENSLIG_FORSØRGER,
                    Hendelsestype.STØNAD_TIL_BARNETILSYN,
                    Hendelsestype.DØDSFALL,
                ),
            YtelsestypeDTO.SKOLEPENGER to
                setOf(
                    Hendelsestype.ANNET,
                    Hendelsestype.MEDLEMSKAP,
                    Hendelsestype.OPPHOLD_I_NORGE,
                    Hendelsestype.ENSLIG_FORSØRGER,
                    Hendelsestype.DØDSFALL,
                    Hendelsestype.SKOLEPENGER,
                ),
            YtelsestypeDTO.KONTANTSTØTTE to
                setOf(
                    Hendelsestype.VILKÅR_BARN,
                    Hendelsestype.VILKÅR_SØKER,
                    Hendelsestype.BARN_I_FOSTERHJEM_ELLER_INSTITUSJON,
                    Hendelsestype.KONTANTSTØTTENS_STØRRELSE,
                    Hendelsestype.STØTTEPERIODE,
                    Hendelsestype.UTBETALING,
                    Hendelsestype.KONTANTSTØTTE_FOR_ADOPTERTE_BARN,
                    Hendelsestype.ANNET_KS,
                ),
        )

    fun getHendelsestyper(ytelsestype: YtelsestypeDTO): Set<Hendelsestype> = HIERARKI[ytelsestype] ?: error("Ikke-støttet ytelsestype: $ytelsestype")
}
