package no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene

import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class ManuellBrevmottaker(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val type: MottakerType,
    var vergetype: Vergetype? = null,
    val navn: String,
    @Column("adresselinje_1")
    val adresselinje1: String,
    @Column("adresselinje_2")
    val adresselinje2: String? = null,
    val postnummer: String,
    val poststed: String,
    val landkode: String,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar()
) {
    override fun toString(): String = "${javaClass.simpleName}(id=$id,behandlingId=$behandlingId)"
}

enum class MottakerType(val visningsnavn: String) {
    BRUKER_MED_UTENLANDSK_ADRESSE("Bruker med utenlandsk adresse"),
    FULLMEKTIG("Fullmektig"),
    VERGE("Verge"),
    DØDSBO("Dødsbo")
}
