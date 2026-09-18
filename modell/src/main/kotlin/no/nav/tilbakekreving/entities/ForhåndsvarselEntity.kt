package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.Forhåndsvarsel
import no.nav.tilbakekreving.behandling.Forhåndsvarsel.Unntak
import no.nav.tilbakekreving.behandling.UttalelseVurdering

data class ForhåndsvarselEntity(
    val brukeruttalelseEntity: BrukeruttalelseEntity?,
    val forhåndsvarselUnntakEntity: ForhåndsvarselUnntakEntity?,
    val uttalelsesfristEntity: UttalelsesfristEntity?,
) {
    fun fraEntity(): Forhåndsvarsel {
        val brukeruttalelse = midlertidigMapping(forhåndsvarselUnntakEntity, brukeruttalelseEntity)?.fraEntity()
        return when {
            uttalelsesfristEntity != null && forhåndsvarselUnntakEntity != null -> {
                error("Forhåndsvarsel kan ikke være både sendt og unntatt")
            }
            uttalelsesfristEntity != null -> Forhåndsvarsel(Forhåndsvarsel.VarselSendt(uttalelsesfristEntity.fraEntity(), brukeruttalelse))
            forhåndsvarselUnntakEntity != null -> Forhåndsvarsel(Unntak(forhåndsvarselUnntakEntity.fraEntity(), brukeruttalelse))
            brukeruttalelse != null -> error("Brukeruttalelse kan ikke eksistere uten forhåndsvarsel eller unntak")
            else -> Forhåndsvarsel(Forhåndsvarsel.IkkeVurdert)
        }
    }

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
