package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class MottakersVarselrespons(@Id
                                  val id: UUID = UUID.randomUUID(),
                                  val behandlingId: UUID,
                                  val akseptertFaktagrunnlag: Boolean?,
                                  val kilde: String,
                                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                  val sporbar: Sporbar = Sporbar())