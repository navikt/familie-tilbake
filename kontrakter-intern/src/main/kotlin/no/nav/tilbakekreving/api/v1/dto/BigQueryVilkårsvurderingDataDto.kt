package no.nav.tilbakekreving.api.v1.dto

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.util.UUID

data class BigQueryVilkårsvurderingDataDto(
    val behandlingId: String,
    val ytelse: String,
    val perioder: List<BigQueryVilkårsvurderingsperiodeDto>,
)

data class BigQueryVilkårsvurderingsperiodeDto(
    val periodeId: UUID,
    val periode: Datoperiode,
    val aktsomhetsgrad: Aktsomhetsgrad,
)

enum class Aktsomhetsgrad {
    GOD_TRO,
    FORSTOD,
    BURDE_FORSTÅTT,
    UAKTSOM,
    GROVT_UAKTSOM,
    FORSETT,
}
