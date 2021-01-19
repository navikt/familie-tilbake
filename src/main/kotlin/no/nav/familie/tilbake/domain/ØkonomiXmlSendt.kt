package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("okonomi_xml_sendt")
data class ØkonomiXmlSendt(@Id
                           val id: UUID = UUID.randomUUID(),
                           val behandlingId: UUID,
                           val melding: String,
                           val kvittering: String?,
                           val meldingstype: String,
                           @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                           val sporbar: Sporbar = Sporbar())