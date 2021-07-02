package no.nav.familie.tilbake.faktaomfeilutbetaling.domain

object HendelsesundertypePerHendelsestype {

    private val HIERARKI = mapOf(Hendelsestype.ANNET to setOf(Hendelsesundertype.ANNET_FRITEKST),
                                 Hendelsestype.BOR_MED_SØKER to setOf(Hendelsesundertype.BOR_IKKE_MED_BARN),
                                 Hendelsestype.BOSATT_I_RIKET to setOf(Hendelsesundertype.BARN_FLYTTET_FRA_NORGE,
                                                                       Hendelsesundertype.BRUKER_FLYTTET_FRA_NORGE,
                                                                       Hendelsesundertype.BARN_BOR_IKKE_I_NORGE,
                                                                       Hendelsesundertype.BRUKER_BOR_IKKE_I_NORGE),
                                 Hendelsestype.LOVLIG_OPPHOLD to setOf(Hendelsesundertype.UTEN_OPPHOLDSTILLATELSE),
                                 Hendelsestype.DØDSFALL to setOf(Hendelsesundertype.BARN_DØD,
                                                                 Hendelsesundertype.BRUKER_DØD),
                                 Hendelsestype.DELT_BOSTED to setOf(Hendelsesundertype.ENIGHET_OM_OPPHØR_DELT_BOSTED,
                                                                    Hendelsesundertype.UENIGHET_OM_OPPHØR_DELT_BOSTED),
                                 Hendelsestype.BARNS_ALDER to setOf(Hendelsesundertype.BARN_OVER_6_ÅR,
                                                                    Hendelsesundertype.BARN_OVER_18_ÅR))


    fun getHendelsesundertyper(hendelsestype: Hendelsestype): Set<Hendelsesundertype> {
        return HIERARKI[hendelsestype] ?: error("Ikke-støttet hendelseType: $hendelsestype")
    }
}

