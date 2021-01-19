package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.*

data class Verge(@Id
                 val id: UUID = UUID.randomUUID(),
                 val ident: String?,
                 val gyldigFom: LocalDate,
                 val gyldigTom: LocalDate,
                 val type: String,
                 val orgNr: String?,
                 val navn: String,
                 val kilde: String,
                 val begrunnelse: String?,
                 @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                 val sporbar: Sporbar = Sporbar())