package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.entities.ReduksjonMomentEntity
import no.nav.tilbakekreving.kontrakter.frontend.models.MomentDto
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.MomentEllerSærligGrunnType
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType

sealed interface MomentEllerSærligGrunn<T : MomentEllerSærligGrunnType> {
    val type: T

    fun tilFrontendDto() = MomentDto(
        moment = type.name,
        beskrivelse = type.navn,
    )
}

sealed interface SærligGrunn : MomentEllerSærligGrunn<SærligGrunnType> {
    override val type: SærligGrunnType

    override fun tilFrontendDto() = MomentDto(
        moment = type.name,
        beskrivelse = type.navn,
    )

    fun tilEntity(): ReduksjonMomentEntity

    object GradAvUaktsomhet : SærligGrunn {
        override val type = SærligGrunnType.GRAD_AV_UAKTSOMHET

        override fun tilEntity(): ReduksjonMomentEntity {
            return ReduksjonMomentEntity(type, null)
        }
    }

    object HeltEllerDelvisNavsFeil : SærligGrunn {
        override val type = SærligGrunnType.HELT_ELLER_DELVIS_NAVS_FEIL

        override fun tilEntity(): ReduksjonMomentEntity {
            return ReduksjonMomentEntity(type, null)
        }
    }

    object StørrelseBeløp : SærligGrunn {
        override val type = SærligGrunnType.STØRRELSE_BELØP

        override fun tilEntity(): ReduksjonMomentEntity {
            return ReduksjonMomentEntity(type, null)
        }
    }

    object TidFraUtbetaling : SærligGrunn {
        override val type = SærligGrunnType.TID_FRA_UTBETALING

        override fun tilEntity(): ReduksjonMomentEntity {
            return ReduksjonMomentEntity(type, null)
        }
    }

    data class Annet(val begrunnelse: String) : SærligGrunn {
        override val type = SærligGrunnType.ANNET

        override fun tilEntity(): ReduksjonMomentEntity {
            return ReduksjonMomentEntity(type, begrunnelse)
        }
    }
}
