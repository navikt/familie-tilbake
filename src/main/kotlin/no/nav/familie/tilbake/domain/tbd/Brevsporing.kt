package no.nav.familie.tilbake.domain.tbd

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Brevsporing(@Id
                       val id: UUID = UUID.randomUUID(),
                       val behandlingId: UUID,
                       val journalpostId: String,
                       val dokumentId: String,
                       val brevtype: Brevtype,
                       @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                       val sporbar: Sporbar = Sporbar())

enum class Brevtype {
    VARSEL,
    KORRIGERT_VARSEL,
    VEDTAK,
    HENLEGGELSE,
    INNHENT_DOKUMENTASJON,
    FRITEKST,
    UDEFINERT;

    fun gjelderVarsel(): Boolean {
        return this in setOf(VARSEL)
    }
}
