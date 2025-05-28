package no.nav.familie.tilbake.config

import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.Rettsgebyr
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Period

object Constants {
    private val brukersSvarfrist: Period = Period.ofWeeks(2)

    fun brukersSvarfrist(): LocalDate = LocalDate.now().plus(brukersSvarfrist)

    fun saksbehandlersTidsfrist(): LocalDate =
        LocalDate
            .now()
            .plusWeeks(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING.defaultVenteTidIUker)

    const val KRAVGRUNNLAG_XML_ROOT_ELEMENT: String = "urn:detaljertKravgrunnlagMelding"

    const val STATUSMELDING_XML_ROOT_ELEMENT: String = "urn:endringKravOgVedtakstatus"

    const val BRUKER_ID_VEDTAKSLØSNINGEN = "VL"

    val MAKS_FEILUTBETALTBELØP_PER_YTELSE =
        mapOf<Ytelsestype, BigDecimal>(
            Ytelsestype.BARNETRYGD to BigDecimal.valueOf(500),
            Ytelsestype.BARNETILSYN to BigDecimal.valueOf(Rettsgebyr.rettsgebyr).multiply(BigDecimal(0.5)),
            Ytelsestype.OVERGANGSSTØNAD to
                BigDecimal
                    .valueOf(Rettsgebyr.rettsgebyr)
                    .multiply(BigDecimal(0.5)),
            Ytelsestype.SKOLEPENGER to
                BigDecimal
                    .valueOf(Rettsgebyr.rettsgebyr)
                    .multiply(BigDecimal(0.5)),
            Ytelsestype.KONTANTSTØTTE to
                BigDecimal
                    .valueOf(Rettsgebyr.rettsgebyr)
                    .multiply(BigDecimal(0.5)),
        )

    const val AUTOMATISK_SAKSBEHANDLING_BEGRUNNELSE = "Automatisk satt verdi"
    const val AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_FAKTA_BEGRUNNELSE = "Automatisk behandling av tilbakekreving under 4 rettsgebyr. Ingen tilbakekreving."
    const val AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_FORELDELSE_BEGRUNNELSE = "Automatisk behandlet under 4 rettsgebyr på foreldet periode. Ikke relevant."
    const val AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_VILKÅRSVURDERING_BEGRUNNELSE = "Automatisk behandlet. Feilutbetaling under 4 rettsgebyr. Beløpet skal ikke tilbakebetales."
    const val AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_VILKÅRSVURDERING_AKTSOMHET_BEGRUNNELSE = "Automatisk behandlet. Feilutbetaling under 4 rettsgebyr. Bruker har ikke handlet forsettlig eller grovt uaktsomt."

    fun hentAutomatiskSaksbehandlingBegrunnelse(
        behandling: Behandling,
        logContext: SecureLog.Context,
    ): String =
        when (behandling.saksbehandlingstype) {
            Saksbehandlingstype.ORDINÆR -> throw Feil(
                message = "Kan ikke utlede automatisk saksbehandlingsbegrunnelse for ordinære saker",
                logContext = logContext,
            )
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR -> behandling.begrunnelseForTilbakekreving ?: AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_FAKTA_BEGRUNNELSE
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP -> AUTOMATISK_SAKSBEHANDLING_BEGRUNNELSE
        }

    fun hentAutomatiskForeldelsesbegrunnelse(
        saksbehandlingstype: Saksbehandlingstype,
        logContext: SecureLog.Context,
    ): String =
        when (saksbehandlingstype) {
            Saksbehandlingstype.ORDINÆR -> throw Feil(
                message = "Kan ikke utlede automatisk foreldelsesbegrunnelse for ordinære saker",
                logContext = logContext,
            )
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR -> AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_FORELDELSE_BEGRUNNELSE
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP -> AUTOMATISK_SAKSBEHANDLING_BEGRUNNELSE
        }

    fun hentAutomatiskVilkårsvurderingBegrunnelse(
        saksbehandlingstype: Saksbehandlingstype,
        logContext: SecureLog.Context,
    ): String =
        when (saksbehandlingstype) {
            Saksbehandlingstype.ORDINÆR -> throw Feil(
                message = "Kan ikke utlede automatisk vilkårsvurderingsbegrunnelse for ordinære saker",
                logContext = logContext,
            )
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR -> AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_VILKÅRSVURDERING_BEGRUNNELSE
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP -> AUTOMATISK_SAKSBEHANDLING_BEGRUNNELSE
        }

    fun hentAutomatiskVilkårsvurderingAktsomhetBegrunnelse(
        saksbehandlingstype: Saksbehandlingstype,
        logContext: SecureLog.Context,
    ): String =
        when (saksbehandlingstype) {
            Saksbehandlingstype.ORDINÆR -> throw Feil(
                message = "Kan ikke utlede automatisk aktsomhetsbegrunnelse for ordinære saker",
                logContext = logContext,
            )
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR -> AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_VILKÅRSVURDERING_AKTSOMHET_BEGRUNNELSE
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP -> AUTOMATISK_SAKSBEHANDLING_BEGRUNNELSE
        }
}

object PropertyName {
    const val FAGSYSTEM = "fagsystem"
    const val ENHET = "enhet"
    const val BESLUTTER = "beslutter"
}
