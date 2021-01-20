package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("vilkarsvurdering_serlig_grunn")
data class VilkårsvurderingSærligGrunn(@Id
                                       val id: UUID = UUID.randomUUID(),
                                       @Column("vilkarsvurdering_aktsomhet_id")
                                       val vilkårsvurderingAktsomhetId: UUID,
                                       @Column("serlig_grunn")
                                       val særligGrunn: SærligGrunn,
                                       val begrunnelse: String?,
                                       @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                       val sporbar: Sporbar = Sporbar())

enum class SærligGrunn(navn: String) {
    GRAD_AV_UAKTSOMHET("Graden av uaktsomhet hos den kravet retter seg mot"),
    HELT_ELLER_DELVIS_NAVS_FEIL("Om feilen helt eller delvis kan tilskrives NAV"),
    STØRRELSE_BELØP("Størrelsen på feilutbetalt beløp"),
    TID_FRA_UTBETALING("Hvor lang tid siden utbetalingen fant sted"),
    ANNET("Annet");
}
