package no.nav.tilbakekreving

import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.endring.EndringObservatør
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.saksbehandler.Behandler

private val ansvarligSaksbehandler = Behandler.Saksbehandler("Z999999")
private val ansvarligBeslutter = Behandler.Saksbehandler("Z111111")

fun saksbehandlerContext(
    endringObservatør: EndringObservatør = EndringObservatørOppsamler(),
    behovObservatør: BehovObservatør = BehovObservatørOppsamler(),
    features: FeatureToggles = defaultFeatures(),
    klokke: Klokke = SystemKlokke,
    behandlingslogg: Behandlingslogg = Behandlingslogg(mutableListOf()),
) = SideeffektContext(ansvarligSaksbehandler, endringObservatør, behovObservatør, BigQueryServiceStub(), features, klokke, behandlingslogg)

fun beslutterContext(
    endringObservatør: EndringObservatør = EndringObservatørOppsamler(),
    behovObservatør: BehovObservatør = BehovObservatørOppsamler(),
    features: FeatureToggles = defaultFeatures(),
    klokke: Klokke = SystemKlokke,
    behandlingslogg: Behandlingslogg = Behandlingslogg(mutableListOf()),
) = SideeffektContext(ansvarligBeslutter, endringObservatør, behovObservatør, BigQueryServiceStub(), features, klokke, behandlingslogg)

fun behandlerContext(
    behandler: Behandler,
    endringObservatør: EndringObservatør = EndringObservatørOppsamler(),
    behovObservatør: BehovObservatør = BehovObservatørOppsamler(),
    features: FeatureToggles = defaultFeatures(),
    klokke: Klokke = SystemKlokke,
    behandlingslogg: Behandlingslogg = Behandlingslogg(mutableListOf()),
) = SideeffektContext(behandler, endringObservatør, behovObservatør, BigQueryServiceStub(), features, klokke, behandlingslogg)

fun systemContext(
    endringObservatør: EndringObservatør = EndringObservatørOppsamler(),
    behovObservatør: BehovObservatør = BehovObservatørOppsamler(),
    features: FeatureToggles = defaultFeatures(),
    klokke: Klokke = SystemKlokke,
    behandlingslogg: Behandlingslogg = Behandlingslogg(mutableListOf()),
) = SideeffektContext(Behandler.Vedtaksløsning, endringObservatør, behovObservatør, BigQueryServiceStub(), features, klokke, behandlingslogg)

fun lesContext() = LesContext(ansvarligSaksbehandler, defaultFeatures(), SystemKlokke)
