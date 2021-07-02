package no.nav.familie.tilbake.faktaomfeilutbetaling.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype


object HendelsestypePerYtelsestype {

    private val HIERARKI = mapOf(Ytelsestype.BARNETRYGD to setOf(Hendelsestype.ANNET,
                                                                 Hendelsestype.BOR_MED_SØKER,
                                                                 Hendelsestype.DØDSFALL,
                                                                 Hendelsestype.BOSATT_I_RIKET,
                                                                 Hendelsestype.LOVLIG_OPPHOLD,
                                                                 Hendelsestype.DELT_BOSTED,
                                                                 Hendelsestype.BARNS_ALDER),
                                 Ytelsestype.OVERGANGSSTØNAD to setOf(Hendelsestype.ANNET,
                                                                      Hendelsestype.BOR_MED_SØKER,
                                                                      Hendelsestype.DØDSFALL),
                                 Ytelsestype.BARNETILSYN to setOf(Hendelsestype.ANNET,
                                                                  Hendelsestype.BOR_MED_SØKER,
                                                                  Hendelsestype.DØDSFALL),
                                 Ytelsestype.SKOLEPENGER to setOf(Hendelsestype.ANNET,
                                                                  Hendelsestype.BOR_MED_SØKER,
                                                                  Hendelsestype.DØDSFALL),
                                 Ytelsestype.KONTANTSTØTTE to setOf(Hendelsestype.ANNET,
                                                                    Hendelsestype.BOR_MED_SØKER,
                                                                    Hendelsestype.DØDSFALL))

    fun getHendelsestyper(ytelsestype: Ytelsestype): Set<Hendelsestype> {
        return HIERARKI[ytelsestype] ?: error("Ikke-støttet ytelsestype: $ytelsestype")
    }
}