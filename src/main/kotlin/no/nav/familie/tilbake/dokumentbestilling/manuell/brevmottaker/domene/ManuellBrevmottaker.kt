package no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene

import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.tilbakekreving.kontrakter.tilbakekreving.MottakerType
import no.nav.tilbakekreving.kontrakter.tilbakekreving.Vergetype
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
    val ident: String? = null,
    @Column("org_nr")
    val orgNr: String? = null,
    @Column("adresselinje_1")
    val adresselinje1: String? = null,
    @Column("adresselinje_2")
    val adresselinje2: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val landkode: String? = null,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    override fun toString(): String = "${javaClass.simpleName}(id=$id,behandlingId=$behandlingId)"

    fun hasManuellAdresse(): Boolean =
        !(
            adresselinje1.isNullOrBlank() ||
                landkode.isNullOrBlank()
        )

    fun harGyldigAdresse(): Boolean =
        if (landkode == "NO") {
            navn.isNotEmpty() &&
                !adresselinje1.isNullOrEmpty() &&
                !postnummer.isNullOrEmpty() &&
                !poststed.isNullOrEmpty()
        } else {
            // Utenlandske manuelle brevmottakere skal ha postnummer og poststed satt i adresselinjene
            navn.isNotEmpty() &&
                !adresselinje1.isNullOrEmpty() &&
                postnummer.isNullOrEmpty() &&
                poststed.isNullOrEmpty()
        }

    val erTilleggsmottaker get() = type == MottakerType.VERGE || type == MottakerType.FULLMEKTIG
}
