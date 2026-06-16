package no.nav.tilbakekreving

import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.endring.EndringObservatør
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.test.FellesTestdata.ANSVARLIG_BESLUTTER
import no.nav.tilbakekreving.test.FellesTestdata.ANSVARLIG_SAKSBEHANDLER
import java.util.EnumMap

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
) = SideeffektContext(ANSVARLIG_SAKSBEHANDLER, endringObservatør, behovObservatør, BigQueryServiceStub(), features, klokke, behandlingslogg)

fun beslutterContext(
    endringObservatør: EndringObservatør = EndringObservatørOppsamler(),
    behovObservatør: BehovObservatør = BehovObservatørOppsamler(),
    features: FeatureToggles = defaultFeatures(),
    klokke: Klokke = SystemKlokke,
    behandlingslogg: Behandlingslogg = Behandlingslogg(mutableListOf()),
) = SideeffektContext(ANSVARLIG_BESLUTTER, endringObservatør, behovObservatør, BigQueryServiceStub(), features, klokke, behandlingslogg)

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
) = LesContext(ANSVARLIG_SAKSBEHANDLER, defaultFeatures(), klokke)
