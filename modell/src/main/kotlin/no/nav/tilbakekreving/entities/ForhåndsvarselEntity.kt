package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.Forhåndsvarsel
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import java.time.LocalDate

data class ForhåndsvarselEntity(
    val brukeruttalelseEntity: BrukeruttalelseEntity?,
    val forhåndsvarselUnntakEntity: ForhåndsvarselUnntakEntity?,
    val fristUtsettelseEntity: FristUtsettelseEntity?,
    val underkjent: Boolean,
) {
    fun fraEntity(opprinneligFrist: LocalDate?): Forhåndsvarsel = Forhåndsvarsel(
        brukeruttalelse = midlertidigMapping(forhåndsvarselUnntakEntity, brukeruttalelseEntity)?.fraEntity(),
        forhåndsvarselUnntak = forhåndsvarselUnntakEntity?.fraEntity(),
        utsattFrist = fristUtsettelseEntity?.fraEntity(),
        opprinneligFrist = opprinneligFrist,
    )

    // Fjernes etter prodsatt og migrering kjørt
    private fun midlertidigMapping(
        forhåndsvarselUnntakEntity: ForhåndsvarselUnntakEntity?,
        brukeruttalelseEntity: BrukeruttalelseEntity?,
    ): BrukeruttalelseEntity? {
        val entity = brukeruttalelseEntity ?: return null

        val legacy = entity.uttalelseVurdering
        val harUnntak = forhåndsvarselUnntakEntity != null

        val nyVurdering = when (legacy) {
            UttalelseVurdering.JA -> {
                if (harUnntak) {
                    UttalelseVurdering.UNNTAK_ALLEREDE_UTTALT_SEG
                } else {
                    UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL
                }
            }

            UttalelseVurdering.NEI -> {
                if (harUnntak) {
                    UttalelseVurdering.UNNTAK_INGEN_UTTALELSE
                } else {
                    UttalelseVurdering.NEI_ETTER_FORHÅNDSVARSEL
                }
            }

            else -> {
                legacy
            }
        }

        return entity.copy(uttalelseVurdering = nyVurdering)
    }
}
