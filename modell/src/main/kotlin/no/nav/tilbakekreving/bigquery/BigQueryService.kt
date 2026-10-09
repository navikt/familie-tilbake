package no.nav.tilbakekreving.bigquery

import no.nav.tilbakekreving.api.v1.dto.BigQueryBehandlingDataDto
import no.nav.tilbakekreving.api.v1.dto.BigQueryVilkårsvurderingDataDto

interface BigQueryService {
    fun oppdaterBehandling(
        bigqueryData: BigQueryBehandlingDataDto,
    )

    /**
     * Sender vurderte perioder ved fattet vedtak fra både gammel og ny løsning.
     * Kopierte vurderinger beholder den enkelte periodens ID og datoer.
     * Senere statusoppdateringer sender ikke periodene på nytt, og historiske saker etterfylles ikke.
     *
     * BigQuery-feil logges uten å stoppe vedtaket eller opprette en retry-jobb.
     * En stabil insert-ID basert på behandling og periode reduserer duplikater ved gjentatte
     * sendinger, men BigQuery garanterer ikke deduplisering.
     */
    fun lagreVilkårsvurdering(bigqueryData: BigQueryVilkårsvurderingDataDto)
}
