package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.entities.ReduksjonMomentEntity
import no.nav.tilbakekreving.kontrakter.frontend.models.MomentDto
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.RelevanteMomentTypeGodTro

sealed interface RelevanteMomentGodTro : MomentEllerSærligGrunn<RelevanteMomentTypeGodTro> {
    override val type: RelevanteMomentTypeGodTro

    override fun tilFrontendDto() = MomentDto(
        moment = type.name,
        beskrivelse = type.navn,
    )

    fun tilEntity(): ReduksjonMomentEntity

    object StørrelseBeløp : RelevanteMomentGodTro {
        override val type: RelevanteMomentTypeGodTro = RelevanteMomentTypeGodTro.STØRRELSE_BELØP

        override fun tilEntity(): ReduksjonMomentEntity {
            return ReduksjonMomentEntity(
                type = type,
                annetBegrunnelse = null,
            )
        }
    }

    object TidFraUtbetaling : RelevanteMomentGodTro {
        override val type: RelevanteMomentTypeGodTro = RelevanteMomentTypeGodTro.TID_FRA_UTBETALING

        override fun tilEntity(): ReduksjonMomentEntity {
            return ReduksjonMomentEntity(
                type = type,
                annetBegrunnelse = null,
            )
        }
    }

    object UtbetalingTillit : RelevanteMomentGodTro {
        override val type: RelevanteMomentTypeGodTro = RelevanteMomentTypeGodTro.UTBETALING_TILLIT

        override fun tilEntity(): ReduksjonMomentEntity {
            return ReduksjonMomentEntity(
                type = type,
                annetBegrunnelse = null,
            )
        }
    }

    data class Annet(val begrunnelse: String) : RelevanteMomentGodTro {
        override val type: RelevanteMomentTypeGodTro = RelevanteMomentTypeGodTro.ANNET

        override fun tilEntity(): ReduksjonMomentEntity {
            return ReduksjonMomentEntity(
                type = type,
                annetBegrunnelse = begrunnelse,
            )
        }
    }
}
