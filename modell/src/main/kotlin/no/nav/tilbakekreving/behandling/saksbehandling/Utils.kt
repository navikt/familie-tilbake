package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype

fun Hendelsestype.beskrivelse(): String {
    return when (this) {
        Hendelsestype.ANNET -> "Annet"
        Hendelsestype.DØDSFALL -> "Dødsfall"
        // Barnetrygd
        Hendelsestype.BOR_MED_SØKER -> "Bor med søker"
        Hendelsestype.BOSATT_I_RIKET -> "Bosatt i riket"
        Hendelsestype.LOVLIG_OPPHOLD -> "Lovlig opphold"
        Hendelsestype.DELT_BOSTED -> "Delt bosted"
        Hendelsestype.BARNS_ALDER -> "Barns alder"
        Hendelsestype.MEDLEMSKAP_BA -> "Medlemskap"
        Hendelsestype.UTVIDET -> "Utvidet"
        Hendelsestype.SATSER -> "Satser"
        Hendelsestype.SMÅBARNSTILLEGG -> "Småbarnstillegg"
        // Felles Enslig forsørger
        Hendelsestype.MEDLEMSKAP -> "§15-2 Medlemskap"
        Hendelsestype.OPPHOLD_I_NORGE -> "§15-3 Opphold i Norge"
        Hendelsestype.ENSLIG_FORSØRGER -> "§15-4 Enslig forsørger"
        // Overgangsstønad
        Hendelsestype.OVERGANGSSTØNAD -> "§15-5 Overgangsstønad"
        Hendelsestype.YRKESRETTET_AKTIVITET -> "§15-6 Yrkesrettet aktivitet"
        Hendelsestype.STØNADSPERIODE -> "§15-8 Stønadsperiode"
        Hendelsestype.INNTEKT -> "§15-9 Inntekt"
        Hendelsestype.PENSJONSYTELSER -> "§15-13 Pensjonsytelser"
        // Barnetilsyn
        Hendelsestype.STØNAD_TIL_BARNETILSYN -> "§15-10 Stønad til barnetilsyn"
        // Skolepenger
        Hendelsestype.SKOLEPENGER -> "§15-11 Skolepenger"
        // Kontantstøtte
        Hendelsestype.VILKÅR_BARN -> "§2 Vilkår barn"
        Hendelsestype.VILKÅR_SØKER -> "§3 Vilkår søker (støttemottaker)"
        Hendelsestype.BARN_I_FOSTERHJEM_ELLER_INSTITUSJON -> "§6 Barn i fosterhjem eller institusjon"
        Hendelsestype.KONTANTSTØTTENS_STØRRELSE -> "§7 Kontantstøttens størrelse"
        Hendelsestype.STØTTEPERIODE -> "§8 Støtteperiode"
        Hendelsestype.UTBETALING -> "§9 Utbetaling"
        Hendelsestype.KONTANTSTØTTE_FOR_ADOPTERTE_BARN -> "§10 Kontantstøtte for adopterte barn"
        Hendelsestype.ANNET_KS -> "Annet"
    }
}

fun Hendelsesundertype.beskrivelse(): String {
    return when (this) {
        // Felles
        Hendelsesundertype.ANNET_FRITEKST -> "Annet fritekst"
        Hendelsesundertype.BRUKER_FLYTTET_FRA_NORGE -> "Bruker flyttet fra Norge"
        Hendelsesundertype.BARN_FLYTTET_FRA_NORGE -> "Barn flyttet fra Norge"
        Hendelsesundertype.BARN_DØD -> "Barnet død"
        Hendelsesundertype.BRUKER_DØD -> "Bruker død"
        // Barnetrygd
        Hendelsesundertype.BRUKER_OG_BARN_FLYTTET_FRA_NORGE -> "Bruker og barn flyttet fra Norge"
        Hendelsesundertype.BRUKER_OG_BARN_BOR_IKKE_I_NORGE -> "Bruker og barn bor ikke i Norge"
        Hendelsesundertype.BOR_IKKE_MED_BARN -> "Bor ikke med barn - ikke fast omsorg"
        Hendelsesundertype.BARN_BOR_IKKE_I_NORGE -> "Barn bor ikke i Norge"
        Hendelsesundertype.BRUKER_BOR_IKKE_I_NORGE -> "Bruker bor ikke i Norge"
        Hendelsesundertype.UTEN_OPPHOLDSTILLATELSE -> "Bruker uten oppholdstillatelse"
        Hendelsesundertype.ENIGHET_OM_OPPHØR_DELT_BOSTED -> "Enighet om opphør av avtale om delt bosted"
        Hendelsesundertype.UENIGHET_OM_OPPHØR_DELT_BOSTED -> "Uenighet om opphør av avtale om delt bosted"
        Hendelsesundertype.FLYTTET_SAMMEN -> "Flyttet sammen"
        Hendelsesundertype.BARN_OVER_18_ÅR -> "Barn over 18 år"
        Hendelsesundertype.BARN_OVER_6_ÅR -> "Barn over 6 år"
        Hendelsesundertype.UTENLANDS_IKKE_MEDLEM -> "Utenlands ikke medlem"
        Hendelsesundertype.MEDLEMSKAP_OPPHØRT -> "Medlemskap opphørt"
        Hendelsesundertype.ANNEN_FORELDER_IKKE_MEDLEM -> "Den andre forelderen ikke medlem"
        Hendelsesundertype.ANNEN_FORELDER_OPPHØRT_MEDLEMSKAP -> "Den andre forelderen opphørt medlemskap"
        Hendelsesundertype.FLERE_UTENLANDSOPPHOLD -> "Flere utenlandsopphold"
        Hendelsesundertype.BOSATT_IKKE_MEDLEM -> "Bosatt ikke medlem"
        Hendelsesundertype.GIFT -> "Gift"
        Hendelsesundertype.NYTT_BARN -> "Nytt barn"
        Hendelsesundertype.SAMBOER_12_MÅNEDER -> "Samboer mer enn 12 måneder"
        Hendelsesundertype.FLYTTET_SAMMEN_ANNEN_FORELDER -> "Flyttet sammen med annen forelder"
        Hendelsesundertype.FLYTTET_SAMMEN_EKTEFELLE -> "Flyttet sammen med ektefelle"
        Hendelsesundertype.FLYTTET_SAMMEN_SAMBOER -> "Flyttet sammen med samboer"
        Hendelsesundertype.GIFT_IKKE_EGEN_HUSHOLDNING -> "Gift ikke egen husholdning"
        Hendelsesundertype.SAMBOER_IKKE_EGEN_HUSHOLDNING -> "Samboer ikke egen husholdning"
        Hendelsesundertype.EKTEFELLE_AVSLUTTET_SONING -> "Ektefelle avsluttet soning"
        Hendelsesundertype.SAMBOER_AVSLUTTET_SONING -> "Samboer avsluttet soning"
        Hendelsesundertype.EKTEFELLE_INSTITUSJON -> "Ektefelle institusjon"
        Hendelsesundertype.SAMBOER_INSTITUSJON -> "Samboer institusjon"
        Hendelsesundertype.SATSENDRING -> "Satsendring"
        Hendelsesundertype.SMÅBARNSTILLEGG_3_ÅR -> "Småbarnstillegg 3 år"
        Hendelsesundertype.SMÅBARNSTILLEGG_OVERGANGSSTØNAD -> "Småbarnstillegg overgangsstønad"
        // Felles Enslig forsørger
        Hendelsesundertype.MEDLEM_SISTE_5_ÅR -> "Medlem siste 5 år"
        Hendelsesundertype.LOVLIG_OPPHOLD -> "Lovlig opphold"
        Hendelsesundertype.BRUKER_IKKE_OPPHOLD_I_NORGE -> "Bruker ikke opphold i Norge"
        Hendelsesundertype.BARN_IKKE_OPPHOLD_I_NORGE -> "Barn ikke opphold i Norge"
        Hendelsesundertype.OPPHOLD_UTLAND_6_UKER_ELLER_MER -> "Opphold utland 6 uker eller mer"
        Hendelsesundertype.UGIFT -> "Ugift (3. ledd)"
        Hendelsesundertype.SEPARERT_SKILT -> "Separert/ skilt (3. ledd)"
        Hendelsesundertype.SAMBOER -> "Samboer (3. ledd)"
        Hendelsesundertype.NYTT_BARN_SAMME_PARTNER -> "Nytt barn samme partner (3. ledd)"
        Hendelsesundertype.ENDRET_SAMVÆRSORDNING -> "Endret samværsordning (4.ledd)"
        Hendelsesundertype.BARN_FLYTTET -> "Barn flyttet (4.ledd)"
        Hendelsesundertype.NÆRE_BOFORHOLD -> "Nære boforhold (4.ledd)"
        Hendelsesundertype.FORELDRE_LEVER_SAMMEN -> "Foreldre lever sammen (4.ledd)"
        // Overgangsstønad
        Hendelsesundertype.BARN_8_ÅR -> "Barn 8 år (2. ledd)"
        Hendelsesundertype.ARBEID -> "Arbeid"
        Hendelsesundertype.REELL_ARBEIDSSØKER -> "Reell arbeidssøker"
        Hendelsesundertype.UTDANNING -> "Utdanning"
        Hendelsesundertype.ETABLERER_EGEN_VIRKSOMHET -> "Etablerer egen virksomhet"
        Hendelsesundertype.HOVEDPERIODE_3_ÅR -> "Hovedperiode 3 år (1.ledd)"
        Hendelsesundertype.UTVIDELSE_UTDANNING -> "Utvidelse utdanning (2. ledd)"
        Hendelsesundertype.UTVIDELSE_SÆRLIG_TILSYNSKREVENDE_BARN ->
            "Utvidelse særlig tilsynskrevende barn (3. ledd)"

        Hendelsesundertype.UTVIDELSE_FORBIGÅENDE_SYKDOM -> "Utvidelse forbigående sykdom (4. ledd)"
        Hendelsesundertype.PÅVENTE_AV_SKOLESTART_STARTET_IKKE ->
            "Påvente av skolestart startet ikke (5.ledd)"

        Hendelsesundertype.PÅVENTE_SKOLESTART_STARTET_TIDLIGERE ->
            "Påvente skolestart startet tidligere (5.ledd)"

        Hendelsesundertype.PÅVENTE_ARBEIDSTILBUD_STARTET_IKKE ->
            "Påvente arbeidstilbud startet ikke (5.ledd)"

        Hendelsesundertype.PÅVENTE_ARBEIDSTILBUD_STARTET_TIDLIGERE ->
            "Påvente arbeidstilbud startet tidligere (5.ledd)"

        Hendelsesundertype.PÅVENTE_BARNETILSYN_IKKE_HA_TILSYN ->
            "Påvente barnetilsyn ikke ha tilsyn (5.ledd)"

        Hendelsesundertype.PÅVENTE_BARNETILSYN_STARTET_TIDLIGERE ->
            "Påvente barnetilsyn startet tidligere (5.ledd)"

        Hendelsesundertype.ARBEIDSSØKER -> "Reell arbeidssøker (5.ledd)"
        Hendelsesundertype.ARBEIDSINNTEKT_FÅTT_INNTEKT -> "Arbeidsinntekt fått inntekt"
        Hendelsesundertype.ARBEIDSINNTEKT_ENDRET_INNTEKT -> "Arbeidsinntekt endret inntekt"
        Hendelsesundertype.ANDRE_FOLKETRYGDYTELSER -> "Andre folketrygdytelser"
        Hendelsesundertype.SELVSTENDIG_NÆRINGSDRIVENDE_FÅTT_INNTEKT ->
            "Selvstendig næringsdrivende fått inntekt"

        Hendelsesundertype.SELVSTENDIG_NÆRINGSDRIVENDE_ENDRET_INNTEKT ->
            "Selvstendig næringsdrivende endret inntekt"

        Hendelsesundertype.UFØRETRYGD -> "Uføretrygd"
        Hendelsesundertype.GJENLEVENDE_EKTEFELLE -> "Gjenlevende ektefelle"
        Hendelsesundertype.BARN_FYLT_1_ÅR -> "Barn fylt 1 år"
        // Barnetilsyn
        Hendelsesundertype.IKKE_ARBEID -> "Arbeid (1. ledd)"
        Hendelsesundertype.EGEN_VIRKSOMHET -> "Egen virksomhet (1. ledd)"
        Hendelsesundertype.TILSYNSUTGIFTER_OPPHØRT -> "Tilsynsutgifter opphørt"
        Hendelsesundertype.TILSYNSUTGIFTER_ENDRET -> "Tilsynsutgifter endret"
        Hendelsesundertype.FORBIGÅENDE_SYKDOM -> "Forbigående sykdom (2. ledd)"
        Hendelsesundertype.ETTER_4_SKOLEÅR_UTGIFTENE_OPPHØRT ->
            "Tilsynsutgifter etter 4. skoleår utgiftene opphørt (2. ledd)"

        Hendelsesundertype.ETTER_4_SKOLEÅR_ENDRET_ARBEIDSTID ->
            "Tilsynsutgifter etter 4. skoleår endret arbeidstid (2. ledd)"

        Hendelsesundertype.INNTEKT_OVER_6G -> "Inntekt over 6G (3. ledd)"
        Hendelsesundertype.KONTANTSTØTTE -> "Kontantstøtte"
        Hendelsesundertype.ØKT_KONTANTSTØTTE -> "Økt kontantstøtte"
        // Skolepenger
        Hendelsesundertype.IKKE_RETT_TIL_OVERGANGSSTØNAD -> "Ikke rett til overgangsstønad (5. ledd)"
        Hendelsesundertype.SLUTTET_I_UTDANNING -> "Sluttet i utdanning"
        // Kontantstøtte
        Hendelsesundertype.BARN_OVER_2_ÅR -> "Barn over 2 år"
        Hendelsesundertype.BARN_IKKE_BOSATT -> "Barn ikke bosatt"
        Hendelsesundertype.BARN_IKKE_OPPHOLDSTILLATELSE -> "Barn ikke oppholdstillatelse"
        Hendelsesundertype.DEN_ANDRE_FORELDEREN_IKKE_MEDLEM_FOLKETRYGDEN ->
            "Den andre forelderen ikke medlem folketrygden"

        Hendelsesundertype.DEN_ANDRE_FORELDEREN_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EØS ->
            "Den andre forelderen ikke medlem folketrygden eller EØS"

        Hendelsesundertype.SØKER_IKKE_MEDLEM_FOLKETRYGDEN -> "Søker ikke medlem folketrygden"
        Hendelsesundertype.SØKER_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EØS ->
            "Søker ikke medlem folketrygden eller EØS"

        Hendelsesundertype.BEGGE_FORELDRENE_IKKE_MEDLEM_FOLKETRYGDEN ->
            "Begge foreldrene ikke medlem folketrygden"

        Hendelsesundertype.BEGGE_FORELDRENE_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EØS ->
            "Begge foreldrene ikke medlem folketrygden eller EØS"

        Hendelsesundertype.BARN_BOR_IKKE_HOS_SØKER -> "Barn bor ikke hos søker"
        Hendelsesundertype.UTENLANDSOPPHOLD_OVER_3_MÅNEDER -> "Utenlandsopphold over 3 måneder"
        Hendelsesundertype.SØKER_FLYTTET_FRA_NORGE -> "Søker flyttet fra Norge"
        Hendelsesundertype.SØKER_IKKE_BOSATT -> "Søker ikke bosatt"
        Hendelsesundertype.SØKER_IKKE_OPPHOLDSTILLATELSE -> "Søker ikke oppholdstillatelse"
        Hendelsesundertype.SØKER_IKKE_OPPHOLDSTILLATELSE_I_MER_ENN_12_MÅNEDER ->
            "Søker ikke oppholdstillatelse i mer enn 12 måneder"

        Hendelsesundertype.BARN_I_FOSTERHJEM -> "Barn i fosterhjem"
        Hendelsesundertype.BARN_I_INSTITUSJON -> "Barn i institusjon"
        Hendelsesundertype.FULLTIDSPLASS_BARNEHAGE -> "Fulltidsplass barnehage"
        Hendelsesundertype.DELTIDSPLASS_BARNEHAGEPLASS -> "Deltidsplass barnehageplass"
        Hendelsesundertype.ØKT_TIMEANTALL_I_BARNEHAGE -> "Økt timeantall i barnehage"
        Hendelsesundertype.BARN_2_ÅR -> "Barn 2 år"
        Hendelsesundertype.DELT_BOSTED_AVTALE_OPPHØRT -> "Delt bosted avtale opphørt"
        Hendelsesundertype.DOBBELUTBETALING -> "Dobbelutbetaling"
        Hendelsesundertype.MER_ENN_11_MÅNEDER -> "Mer enn 11 måneder"
        Hendelsesundertype.BARN_STARTET_PÅ_SKOLEN -> "Barn startet på skolen"
    }
}
