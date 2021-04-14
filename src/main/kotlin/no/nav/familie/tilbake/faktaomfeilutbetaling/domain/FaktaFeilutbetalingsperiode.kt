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
    BA_ANNET,
    EF_ANNET,
    KS_ANNET,
    ENDRING_STØNADSPERIODEN,
    ØKONOMIFEIL,
    MEDLEMSKAP
}

enum class Hendelsesundertype {

    MOTTAKER_DØD,
    BARN_DØD,
    IKKE_OMSORG,
    ANNET_FRITEKST,
    IKKE_BOSATT,
    MEDLEM_I_ANNET_LAND,
    IKKE_LOVLIG_OPPHOLD,
    UTVANDRET,
    DOBBELUTBETALING,
    FOR_MYE_UTBETALT,
    ØKONOMI_FEIL_TREKK,
    ØKONOMI_FEIL_FERIEPENGER

}

