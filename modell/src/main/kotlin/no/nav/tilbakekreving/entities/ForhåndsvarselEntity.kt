package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.Forhåndsvarsel
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
        val brukeruttalelse = brukeruttalelseEntity?.fraEntity()
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
}

enum class ForhåndsvarselVurderingstype {
    IKKE_VURDERT,
    VARSEL_SENDT,
    MÅ_VURDERES_PÅ_NYTT,
    UNNTAK,
}
