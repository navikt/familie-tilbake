package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.*

data class Foreldelsesperiode(@Id
                              val id: UUID = UUID.randomUUID(),
                              val vurdertForeldelseId: UUID,
                              val fom: LocalDate,
                              val tom: LocalDate,
                              val foreldelsesvurderingstype: String,
                              val begrunnelse: String,
                              val foreldelsesfrist: LocalDate?,
                              val oppdagelsesdato: LocalDate?,
                              @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                              val sporbar: Sporbar = Sporbar())