package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("okonomi_xml_mottatt")
data class ØkonomiXmlMottatt(@Id
                             val id: UUID = UUID.randomUUID(),
                             val melding: String,
                             val sekvens: Int?,
                             val tilkoblet: Boolean?,
                             val eksternFagsakId: String?,
                             val henvisning: String?,
                             @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                             val sporbar: Sporbar = Sporbar())