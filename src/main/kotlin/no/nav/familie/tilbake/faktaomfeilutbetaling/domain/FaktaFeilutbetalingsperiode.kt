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
    BARN_OVER_6_ÅR
}
