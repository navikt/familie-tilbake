package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("okonomi_xml_mottatt_arkiv")
data class ØkonomiXmlMottattArkiv(@Id
                                  val id: UUID = UUID.randomUUID(),
                                  val melding: String,
                                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                  val sporbar: Sporbar = Sporbar())