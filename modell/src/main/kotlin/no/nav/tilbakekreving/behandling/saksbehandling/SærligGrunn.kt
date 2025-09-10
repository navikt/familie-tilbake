package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.entities.SærligGrunnEntity
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType

sealed interface SærligGrunn {
    val type: SærligGrunnType

    fun tilEntity(): SærligGrunnEntity

    object GradAvUaktsomhet : SærligGrunn {
        override val type = SærligGrunnType.GRAD_AV_UAKTSOMHET

        override fun tilEntity(): SærligGrunnEntity {
            return SærligGrunnEntity(type, null)
        }
    }

    object HeltEllerDelvisNavsFeil : SærligGrunn {
        override val type = SærligGrunnType.HELT_ELLER_DELVIS_NAVS_FEIL

        override fun tilEntity(): SærligGrunnEntity {
            return SærligGrunnEntity(type, null)
        }
    }

    object StørrelseBeløp : SærligGrunn {
        override val type = SærligGrunnType.STØRRELSE_BELØP

        override fun tilEntity(): SærligGrunnEntity {
            return SærligGrunnEntity(type, null)
        }
    }

    object TidFraUtbetaling : SærligGrunn {
        override val type = SærligGrunnType.TID_FRA_UTBETALING

        override fun tilEntity(): SærligGrunnEntity {
            return SærligGrunnEntity(type, null)
        }
    }

    data class Annet(val begrunnelse: String) : SærligGrunn {
        override val type = SærligGrunnType.ANNET

        override fun tilEntity(): SærligGrunnEntity {
            return SærligGrunnEntity(type, begrunnelse)
        }
    }
}
