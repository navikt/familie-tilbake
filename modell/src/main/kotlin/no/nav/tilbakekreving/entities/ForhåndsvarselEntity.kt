package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.Forhåndsvarsel
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behandling.saksbehandling.ÅrsakTilTilbakeføring
import java.util.UUID

data class ForhåndsvarselEntity(
    val id: UUID,
    val vurderingstype: ForhåndsvarselVurderingstype,
    val brukeruttalelseEntity: BrukeruttalelseEntity?,
    val forhåndsvarselUnntakEntity: ForhåndsvarselUnntakEntity?,
    val uttalelsesfristEntity: UttalelsesfristEntity?,
    val tilbakeført: ÅrsakTilTilbakeføring?,
) {
    fun fraEntity(): Forhåndsvarsel {
        val brukeruttalelse = midlertidigMapping()?.fraEntity()
        return Forhåndsvarsel(
            when (vurderingstype) {
                ForhåndsvarselVurderingstype.IKKE_VURDERT -> Forhåndsvarsel.IkkeVurdert
                ForhåndsvarselVurderingstype.VARSEL_SENDT -> {
                    val uttalelsesfrist = requireNotNull(uttalelsesfristEntity) {
                        "Uttalelsesfrist må finnes når forhåndsvarsel er sendt"
                    }
                    Forhåndsvarsel.VarselSendt(uttalelsesfrist.fraEntity(), brukeruttalelse, tilbakeført)
                }

                ForhåndsvarselVurderingstype.MÅ_VURDERES_PÅ_NYTT -> Forhåndsvarsel.MåVurderesPåNytt(brukeruttalelse)
                ForhåndsvarselVurderingstype.UNNTAK -> {
                    val unntak = requireNotNull(forhåndsvarselUnntakEntity) {
                        "Forhåndsvarselunntak må finnes når vurderingstypen er unntak"
                    }
                    unntak.fraEntity(brukeruttalelse, tilbakeført ?: unntak.tilbakeført)
                }
            },
        )
    }

    // Fjernes etter prodsatt og migrering kjørt
    private fun midlertidigMapping(): BrukeruttalelseEntity? {
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

enum class ForhåndsvarselVurderingstype {
    IKKE_VURDERT,
    VARSEL_SENDT,
    MÅ_VURDERES_PÅ_NYTT,
    UNNTAK,
}
