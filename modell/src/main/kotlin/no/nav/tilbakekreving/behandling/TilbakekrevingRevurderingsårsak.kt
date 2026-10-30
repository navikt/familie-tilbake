package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.entities.TilbakekrevingRevurderingEntity
import no.nav.tilbakekreving.kontrakter.frontend.models.EtterKlageDto
import no.nav.tilbakekreving.kontrakter.frontend.models.FeilutbetaltBelopEndretDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForeldelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RevurderingsinstansDto
import no.nav.tilbakekreving.kontrakter.frontend.models.TilbakekrevingRevurderingsarsakDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UtenKlageDto
import java.util.UUID

sealed interface TilbakekrevingRevurderingsårsak {
    val årsakType: TilbakekrevingRevurderingsårsakType
    val vedtakOmgjortAv: VedtakOmgjortAv?
    val beskrivelse: String

    fun tilEntity(behandlingId: UUID): TilbakekrevingRevurderingEntity

    fun tilFrontend(): TilbakekrevingRevurderingsarsakDto

    data class EtterKlage(
        override val årsakType: TilbakekrevingRevurderingsårsakType,
        override val vedtakOmgjortAv: VedtakOmgjortAv?,
        override val beskrivelse: String,
    ) : TilbakekrevingRevurderingsårsak {
        override fun tilEntity(behandlingId: UUID): TilbakekrevingRevurderingEntity {
            return TilbakekrevingRevurderingEntity(
                id = UUID.randomUUID(),
                behandlingRef = behandlingId,
                revurderingsårsakType = årsakType,
                omgjortAv = vedtakOmgjortAv!!,
                beskrivelse = beskrivelse,
            )
        }

        override fun tilFrontend(): EtterKlageDto {
            return EtterKlageDto(
                instans = RevurderingsinstansDto.forValue(vedtakOmgjortAv!!.name),
                begrunnelse = beskrivelse,
            )
        }
    }

    data class FeilutbetaltBelopEndret(
        override val årsakType: TilbakekrevingRevurderingsårsakType,
        override val vedtakOmgjortAv: VedtakOmgjortAv?,
        override val beskrivelse: String,
    ) : TilbakekrevingRevurderingsårsak {
        override fun tilEntity(behandlingId: UUID): TilbakekrevingRevurderingEntity {
            return TilbakekrevingRevurderingEntity(
                id = UUID.randomUUID(),
                behandlingRef = behandlingId,
                revurderingsårsakType = årsakType,
                omgjortAv = vedtakOmgjortAv!!,
                beskrivelse = beskrivelse,
            )
        }

        override fun tilFrontend(): FeilutbetaltBelopEndretDto {
            return FeilutbetaltBelopEndretDto(
                instans = RevurderingsinstansDto.forValue(vedtakOmgjortAv!!.name),
                begrunnelse = beskrivelse,
            )
        }
    }

    data class Foreldelse(
        override val årsakType: TilbakekrevingRevurderingsårsakType,
        override val vedtakOmgjortAv: VedtakOmgjortAv?,
        override val beskrivelse: String,
    ) : TilbakekrevingRevurderingsårsak {
        override fun tilEntity(behandlingId: UUID): TilbakekrevingRevurderingEntity {
            return TilbakekrevingRevurderingEntity(
                id = UUID.randomUUID(),
                behandlingRef = behandlingId,
                revurderingsårsakType = årsakType,
                omgjortAv = null,
                beskrivelse = beskrivelse,
            )
        }

        override fun tilFrontend(): ForeldelseDto {
            return ForeldelseDto(
                begrunnelse = beskrivelse,
            )
        }
    }

    data class UtenKlage(
        override val årsakType: TilbakekrevingRevurderingsårsakType,
        override val vedtakOmgjortAv: VedtakOmgjortAv?,
        override val beskrivelse: String,
    ) : TilbakekrevingRevurderingsårsak {
        override fun tilEntity(behandlingId: UUID): TilbakekrevingRevurderingEntity {
            return TilbakekrevingRevurderingEntity(
                id = UUID.randomUUID(),
                behandlingRef = behandlingId,
                revurderingsårsakType = årsakType,
                omgjortAv = vedtakOmgjortAv!!,
                beskrivelse = beskrivelse,
            )
        }

        override fun tilFrontend(): UtenKlageDto {
            return UtenKlageDto(
                instans = UtenKlageDto.Instans.forValue(vedtakOmgjortAv!!.name),
                begrunnelse = beskrivelse,
            )
        }
    }
}

enum class TilbakekrevingRevurderingsårsakType(
    val navn: String,
) {
    ETTER_KLAGE("Omgjøring av vedtaket om tilbakekreving etter klage"),
    FEILUTBETALT_BELØP_ENDRET("Omgjøring av vedtaket om tilbakekreving fordi det feilutbetalte beløpet er endret"),
    FORELDELSE("Endring av kravet på grunn av foreldelse"),
    UTEN_KLAGE("Omgjøring av vedtaket om tilbakekreving uten klage (fvl. § 35)"),
}

enum class VedtakOmgjortAv {
    Førsteinstansen,
    Klageinstansen,
    Trygderetten,
    Domstolen,
}
