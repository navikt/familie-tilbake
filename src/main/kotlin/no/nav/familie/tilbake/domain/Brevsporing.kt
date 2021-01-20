package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Brevsporing(@Id
                       val id: UUID = UUID.randomUUID(),
                       val behandlingId: UUID,
                       val journalpostId: String,
                       val dokumentId: String,
                       val brevtype: Brevtype,
                       @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                       val sporbar: Sporbar = Sporbar())

enum class Brevtype {
    VARSEL_BREV,
    VEDTAK_BREV,
    HENLEGGELSE_BREV,
    INNHENT_DOKUMENTASJONBREV,
    FRITEKST,
    UDEFINERT
}
