package no.nav.familie.tilbake.faktaomfeilutbetaling.domain

object HendelsesundertypePerHendelsestype {

    val HIERARKI = mapOf(Hendelsestype.ANNET to setOf(Hendelsesundertype.ANNET_FRITEKST),
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
                                                            Hendelsesundertype.BARN_OVER_18_ÅR),
                         Hendelsestype.MEDLEMSKAP to setOf(Hendelsesundertype.MEDLEM_SISTE_5_ÅR,
                                                           Hendelsesundertype.LOVLIG_OPPHOLD),
                         Hendelsestype.OPPHOLD_I_NORGE to setOf(Hendelsesundertype.BRUKER_IKKE_OPPHOLD_I_NORGE,
                                                                Hendelsesundertype.BARN_IKKE_OPPHOLD_I_NORGE,
                                                                Hendelsesundertype.BRUKER_FLYTTET_FRA_NORGE,
                                                                Hendelsesundertype.BARN_FLYTTET_FRA_NORGE,
                                                                Hendelsesundertype.OPPHOLD_UTLAND_6_UKER_ELLER_MER),
                         Hendelsestype.ENSLIG_FORSØRGER to setOf(Hendelsesundertype.UGIFT,
                                                                 Hendelsesundertype.SEPARERT_SKILT,
                                                                 Hendelsesundertype.SAMBOER,
                                                                 Hendelsesundertype.NYTT_BARN_SAMME_PARTNER,
                                                                 Hendelsesundertype.ENDRET_SAMVÆRSORDNING,
                                                                 Hendelsesundertype.BOR_IKKE_MED_BARN,
                                                                 Hendelsesundertype.NÆRE_BOFORHOLD,
                                                                 Hendelsesundertype.FORELDRE_LEVER_SAMMEN),
                         Hendelsestype.OVERGANGSSTØNAD to setOf(Hendelsesundertype.BARN_8_ÅR),
                         Hendelsestype.YRKESRETTET_AKTIVITET to setOf(Hendelsesundertype.ARBEID,
                                                                      Hendelsesundertype.REELL_ARBEIDSSØKER,
                                                                      Hendelsesundertype.UTDANNING,
                                                                      Hendelsesundertype.ETABLERER_EGEN_VIRKSOMHET),
                         Hendelsestype.STØNADSPERIODE to setOf(Hendelsesundertype.HOVEDPERIODE_3_ÅR,
                                                               Hendelsesundertype.UTVIDELSE_SÆRLIG_TILSYNSKREVENDE_BARN,
                                                               Hendelsesundertype.UTVIDELSE_FORBIGÅENDE_SYKDOM,
                                                               Hendelsesundertype.PÅVENTE_AV_SKOLESTART_STARTET_IKKE,
                                                               Hendelsesundertype.PÅVENTE_SKOLESTART_STARTET_TIDLIGERE,
                                                               Hendelsesundertype.PÅVENTE_ARBEIDSTILBUD_STARTET_IKKE,
                                                               Hendelsesundertype.PÅVENTE_ARBEIDSTILBUD_STARTET_TIDLIGERE,
                                                               Hendelsesundertype.PÅVENTE_BARNETILSYN_IKKE_HA_TILSYN,
                                                               Hendelsesundertype.PÅVENTE_BARNETILSYN_STARTET_TIDLIGERE,
                                                               Hendelsesundertype.REELL_ARBEIDSSØKER),
                         Hendelsestype.INNTEKT to setOf(Hendelsesundertype.ARBEIDSINNTEKT_FÅTT_INNTEKT,
                                                        Hendelsesundertype.ARBEIDSINNTEKT_ENDRET_INNTEKT,
                                                        Hendelsesundertype.ANDRE_FOLKETRYGDYTELSER,
                                                        Hendelsesundertype.SELVSTENDIG_NÆRINGSDRIVENDE_FÅTT_INNTEKT,
                                                        Hendelsesundertype.SELVSTENDIG_NÆRINGSDRIVENDE_ENDRET_INNTEKT),
                         Hendelsestype.PENSJONSYTELSER to setOf(Hendelsesundertype.UFØRETRYGD,
                                                                Hendelsesundertype.GJENLEVENDE_EKTEFELLE),
                         Hendelsestype.STØNAD_TIL_BARNETILSYN to setOf(Hendelsesundertype.ARBEID,
                                                                       Hendelsesundertype.EGEN_VIRKSOMHET,
                                                                       Hendelsesundertype.TILSYNSUTGIFTER_OPPHØRT,
                                                                       Hendelsesundertype.TILSYNSUTGIFTER_ENDRET,
                                                                       Hendelsesundertype.FORBIGÅENDE_SYKDOM,
                                                                       Hendelsesundertype.ETTER_4_SKOLEÅR_UTGIFTENE_OPPHØRT,
                                                                       Hendelsesundertype.ETTER_4_SKOLEÅR_ENDRET_ARBEIDSTID,
                                                                       Hendelsesundertype.INNTEKT_OVER_6G,
                                                                       Hendelsesundertype.KONTANTSTØTTE,
                                                                       Hendelsesundertype.ØKT_KONTANTSTØTTE),
                         Hendelsestype.SKOLEPENGER to setOf(Hendelsesundertype.IKKE_RETT_TIL_OVERGANGSSTØNAD,
                                                            Hendelsesundertype.SLUTTET_I_UTDANNING)


    )


    fun getHendelsesundertyper(hendelsestype: Hendelsestype): Set<Hendelsesundertype> {
        return HIERARKI[hendelsestype] ?: error("Ikke-støttet hendelseType: $hendelsestype")
    }
}

