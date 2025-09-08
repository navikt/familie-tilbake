package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.api.v1.dto.VurdertSærligGrunnDto
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.entities.SkalReduseresEntity
import no.nav.tilbakekreving.entities.SkalReduseresType
import no.nav.tilbakekreving.entities.SærligeGrunnerEntity
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunn

// §22-15 4. ledd
class ReduksjonSærligeGrunner(
    val begrunnelse: String,
    val grunner: Set<SærligGrunn>,
    val skalReduseres: SkalReduseres,
) {
    fun tilEntity(): SærligeGrunnerEntity {
        return SærligeGrunnerEntity(
            begrunnelse = begrunnelse,
            grunner = grunner.map { it },
            skalReduseres = skalReduseres.tilEntity(),
        )
    }

    fun vurderteGrunner(): List<VurdertSærligGrunnDto> {
        return grunner.map { VurdertSærligGrunnDto(it, null) }
    }

    sealed interface SkalReduseres {
        fun reduksjon(): Reduksjon

        fun tilEntity(): SkalReduseresEntity

        class Ja(val prosentdel: Int) : SkalReduseres {
            override fun reduksjon(): Reduksjon {
                return Reduksjon.Prosentdel(prosentdel.toBigDecimal())
            }

            override fun tilEntity(): SkalReduseresEntity {
                return SkalReduseresEntity(SkalReduseresType.Ja, prosentdel)
            }
        }

        data object Nei : SkalReduseres {
            override fun reduksjon(): Reduksjon {
                return Reduksjon.FullstendigTilbakekreving()
            }

            override fun tilEntity(): SkalReduseresEntity {
                return SkalReduseresEntity(SkalReduseresType.Nei, null)
            }
        }
    }
}
