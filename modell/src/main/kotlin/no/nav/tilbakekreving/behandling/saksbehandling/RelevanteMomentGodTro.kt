package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.entities.RelevanteMomentGodTroEntity
import no.nav.tilbakekreving.kontrakter.frontend.models.MomentDto
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.RelevanteMomentType

sealed interface RelevanteMomentGodTro {
    val type: RelevanteMomentType

    fun tilFrontendDto() = MomentDto(
        moment = type.name,
        beskrivelse = type.navn,
    )

    fun tilEntity(): RelevanteMomentGodTroEntity

    object StørrelseBeløp : RelevanteMomentGodTro {
        override val type: RelevanteMomentType = RelevanteMomentType.STØRRELSE_BELØP

        override fun tilEntity(): RelevanteMomentGodTroEntity {
            return RelevanteMomentGodTroEntity(
                type = type,
                annetBegrunnelse = null,
            )
        }
    }

    object TidFraUtbetaling : RelevanteMomentGodTro {
        override val type: RelevanteMomentType = RelevanteMomentType.TID_FRA_UTBETALING

        override fun tilEntity(): RelevanteMomentGodTroEntity {
            return RelevanteMomentGodTroEntity(
                type = type,
                annetBegrunnelse = null,
            )
        }
    }

    object UtbetalingTillit : RelevanteMomentGodTro {
        override val type: RelevanteMomentType = RelevanteMomentType.UTBETALING_TILLIT

        override fun tilEntity(): RelevanteMomentGodTroEntity {
            return RelevanteMomentGodTroEntity(
                type = type,
                annetBegrunnelse = null,
            )
        }
    }

    data class Annet(val begrunnelse: String) : RelevanteMomentGodTro {
        override val type: RelevanteMomentType = RelevanteMomentType.ANNET

        override fun tilEntity(): RelevanteMomentGodTroEntity {
            return RelevanteMomentGodTroEntity(
                type = type,
                annetBegrunnelse = begrunnelse,
            )
        }
    }
}
