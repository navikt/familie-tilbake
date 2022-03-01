package no.nav.familie.tilbake.faktaomfeilutbetaling.domain

import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class FaktaFeilutbetalingsperiode(@Id
                                       val id: UUID = UUID.randomUUID(),
                                       @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                       val periode: Periode,
                                       val hendelsestype: Hendelsestype,
                                       val hendelsesundertype: Hendelsesundertype,
                                       @Version
                                       val versjon: Long = 0,
                                       @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                       val sporbar: Sporbar = Sporbar())

enum class Hendelsestype {
    ANNET,
    BOR_MED_SØKER,
    BOSATT_I_RIKET,
    LOVLIG_OPPHOLD,
    DØDSFALL,
    DELT_BOSTED,
    BARNS_ALDER,
    MEDLEMSKAP,
    OPPHOLD_I_NORGE,
    ENSLIG_FORSØRGER,
    OVERGANGSSTØNAD,
    YRKESRETTET_AKTIVITET,
    STØNADSPERIODE,
    INNTEKT,
    PENSJONSYTELSER,
    STØNAD_TIL_BARNETILSYN,
    SKOLEPENGER,
}

enum class Hendelsesundertype {

    ANNET_FRITEKST,
    BOR_IKKE_MED_BARN,
    BARN_FLYTTET_FRA_NORGE,
    BRUKER_FLYTTET_FRA_NORGE,
    BARN_BOR_IKKE_I_NORGE,
    BRUKER_BOR_IKKE_I_NORGE,
    UTEN_OPPHOLDSTILLATELSE,
    BARN_DØD,
    BRUKER_DØD,
    ENIGHET_OM_OPPHØR_DELT_BOSTED,
    UENIGHET_OM_OPPHØR_DELT_BOSTED,
    BARN_OVER_18_ÅR,
    BARN_OVER_6_ÅR,
    MEDLEM_SISTE_5_ÅR,
    LOVLIG_OPPHOLD,
    BRUKER_IKKE_OPPHOLD_I_NORGE,
    BARN_IKKE_OPPHOLD_I_NORGE,
    OPPHOLD_UTLAND_6_UKER_ELLER_MER,
    UGIFT,
    SEPARERT_SKILT,
    SAMBOER,
    NYTT_BARN_SAMME_PARTNER,
    ENDRET_SAMVÆRSORDNING,
    BARN_FLYTTET,
    NÆRE_BOFORHOLD,
    FORELDRE_LEVER_SAMMEN,
    BARN_8_ÅR,
    UTDANNING,
    ETABLERER_EGEN_VIRKSOMHET,
    HOVEDPERIODE_3_ÅR,
    UTVIDELSE_UTDANNING,
    UTVIDELSE_SÆRLIG_TILSYNSKREVENDE_BARN,
    UTVIDELSE_FORBIGÅENDE_SYKDOM,
    PÅVENTE_AV_SKOLESTART_STARTET_IKKE,
    PÅVENTE_SKOLESTART_STARTET_TIDLIGERE,
    PÅVENTE_ARBEIDSTILBUD_STARTET_IKKE,
    PÅVENTE_ARBEIDSTILBUD_STARTET_TIDLIGERE,
    PÅVENTE_BARNETILSYN_IKKE_HA_TILSYN,
    PÅVENTE_BARNETILSYN_STARTET_TIDLIGERE,
    REELL_ARBEIDSSØKER,
    ARBEIDSINNTEKT_FÅTT_INNTEKT,
    ARBEIDSINNTEKT_ENDRET_INNTEKT,
    ANDRE_FOLKETRYGDYTELSER,
    SELVSTENDIG_NÆRINGSDRIVENDE_FÅTT_INNTEKT,
    SELVSTENDIG_NÆRINGSDRIVENDE_ENDRET_INNTEKT,
    UFØRETRYGD,
    GJENLEVENDE_EKTEFELLE,
    ARBEID,
    EGEN_VIRKSOMHET,
    TILSYNSUTGIFTER_OPPHØRT,
    TILSYNSUTGIFTER_ENDRET,
    FORBIGÅENDE_SYKDOM,
    ETTER_4_SKOLEÅR_UTGIFTENE_OPPHØRT,
    ETTER_4_SKOLEÅR_ENDRET_ARBEIDSTID,
    INNTEKT_OVER_6G,
    KONTANTSTØTTE,
    ØKT_KONTANTSTØTTE,
    IKKE_RETT_TIL_OVERGANGSSTØNAD,
    SLUTTET_I_UTDANNING,

}
