package no.nav.familie.tilbake.faktaomfeilutbetaling.domain

import com.fasterxml.jackson.annotation.JsonIgnore
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

enum class Hendelsestype(@JsonIgnore val sortering: Int) {

    BA_ANNET(999),
    EF_ANNET(999),
    KS_ANNET(999),
}

enum class Hendelsesundertype(@JsonIgnore val sortering: Int) {

    ANNET_FRITEKST(999)
}
