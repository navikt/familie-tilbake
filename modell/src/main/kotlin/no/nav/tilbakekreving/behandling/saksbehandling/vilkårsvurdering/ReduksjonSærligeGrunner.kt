package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.api.v1.dto.VurdertSærligGrunnDto
import no.nav.tilbakekreving.behandling.saksbehandling.SærligGrunn
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.breeeev.PåkrevdBegrunnelse
import no.nav.tilbakekreving.endring.VurdertUtbetaling
import no.nav.tilbakekreving.entities.SkalReduseresEntity
import no.nav.tilbakekreving.entities.SkalReduseresType
import no.nav.tilbakekreving.entities.SærligeGrunnerEntity
import java.util.UUID

// §22-15 4. ledd
class ReduksjonSærligeGrunner(
    val begrunnelse: String,
    val grunner: Set<SærligGrunn>,
    val skalReduseres: SkalReduseres,
) {
    fun tilEntity(periodeRef: UUID): SærligeGrunnerEntity {
        return SærligeGrunnerEntity(
            periodeRef = periodeRef,
            begrunnelse = begrunnelse,
            grunner = grunner.map { it.tilEntity() },
            skalReduseres = skalReduseres.tilEntity(),
        )
    }

    fun oppsummerVurdering(): VurdertUtbetaling.SærligeGrunner {
        return VurdertUtbetaling.SærligeGrunner(
            beløpReduseres = skalReduseres.lagStatistikk(),
            grunner = grunner,
        )
    }

    fun vurderteGrunner(): List<VurdertSærligGrunnDto> {
        return grunner.map {
            when (it) {
                is SærligGrunn.Annet -> VurdertSærligGrunnDto(it.type, it.begrunnelse)
                else -> VurdertSærligGrunnDto(it.type, null)
            }
        }
    }

    sealed interface SkalReduseres {
        fun reduksjon(): Reduksjon

        fun tilEntity(): SkalReduseresEntity

        fun lagStatistikk(): VurdertUtbetaling.JaNeiVurdering

        fun påkrevdeVurderinger(): Set<PåkrevdBegrunnelse> = setOf(PåkrevdBegrunnelse.SÆRLIGE_GRUNNER)

        class Ja(val prosentdel: Int) : SkalReduseres {
            override fun reduksjon(): Reduksjon {
                return Reduksjon.Prosentdel(prosentdel.toBigDecimal())
            }

            override fun lagStatistikk(): VurdertUtbetaling.JaNeiVurdering = VurdertUtbetaling.JaNeiVurdering.Ja

            override fun tilEntity(): SkalReduseresEntity {
                return SkalReduseresEntity(SkalReduseresType.Ja, prosentdel)
            }
        }

        data object Nei : SkalReduseres {
            override fun reduksjon(): Reduksjon {
                return Reduksjon.FullstendigTilbakekreving()
            }

            override fun lagStatistikk(): VurdertUtbetaling.JaNeiVurdering = VurdertUtbetaling.JaNeiVurdering.Nei

            override fun tilEntity(): SkalReduseresEntity {
                return SkalReduseresEntity(SkalReduseresType.Nei, null)
            }
        }
    }
}
