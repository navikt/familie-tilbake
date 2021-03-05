package no.nav.familie.tilbake.domain.tbd

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("okonomi_xml_mottatt_arkiv")
data class ØkonomiXmlMottattArkiv(@Id
                                  val id: UUID = UUID.randomUUID(),
                                  val melding: String,
                                  @Version
                                  val versjon: Long = 0,
                                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                  val sporbar: Sporbar = Sporbar())
