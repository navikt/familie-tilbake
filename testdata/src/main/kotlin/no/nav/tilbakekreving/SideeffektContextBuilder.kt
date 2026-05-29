package no.nav.tilbakekreving

import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.endring.EndringObservatør
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.saksbehandler.Behandler
import java.util.EnumMap

val ansvarligSaksbehandler = Behandler.Saksbehandler("Z999999")
val ansvarligBeslutter = Behandler.Saksbehandler("Z111111")

fun defaultFeatures(
    featureOverrides: Array<Pair<Toggle, Boolean>> = emptyArray(),
    fagsystemToggleOverrides: Array<Pair<FagsystemToggle, Boolean>> = emptyArray(),
): FeatureToggles = FeatureToggles(
    overrides = EnumMap<Toggle, Boolean>(Toggle::class.java).apply {
        putAll(featureOverrides)
    },
    fagsystemToggles = EnumMap<FagsystemDTO, EnumMap<FagsystemToggle, Boolean>>(
        FagsystemDTO.entries.associateWith {
            EnumMap<FagsystemToggle, Boolean>(FagsystemToggle::class.java).apply {
                putAll(fagsystemToggleOverrides.toMap())
            }
        },
    ),
)

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

fun lesContext(
    klokke: Klokke = SystemKlokke,
) = LesContext(ansvarligSaksbehandler, defaultFeatures(), klokke)
