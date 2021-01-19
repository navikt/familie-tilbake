package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("vilkar_serlig_grunn")
data class VilkårSærligGrunn(@Id
                             val id: UUID = UUID.randomUUID(),
                             @Column("vilkar_aktsomhet_id")
                             val vilkårAktsomhetId: UUID,
                             @Column("serlig_grunn")
                             val særligGrunn: String,
                             val begrunnelse: String?,
                             @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                             val sporbar: Sporbar = Sporbar())