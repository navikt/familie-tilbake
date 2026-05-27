package no.nav.tilbakekreving

import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.bigquery.BigQueryService
import no.nav.tilbakekreving.endring.EndringObservatør
import no.nav.tilbakekreving.saksbehandler.Behandler

class SideeffektContext(
    behandler: Behandler,
    val endringObservatør: EndringObservatør,
    val behovObservatør: BehovObservatør,
    val bigQueryService: BigQueryService,
    features: FeatureToggles,
    klokke: Klokke,
    val behandlingslogg: Behandlingslogg,
) : LesContext(behandler, features, klokke)
