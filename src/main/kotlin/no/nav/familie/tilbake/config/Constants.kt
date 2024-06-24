package no.nav.familie.tilbake.config

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Period

object Constants {
    private val rettsgebyrForDato =
        listOf(
            Datobeløp(LocalDate.of(2021, 1, 1), 1199),
            Datobeløp(LocalDate.of(2022, 1, 1), 1223),
            Datobeløp(LocalDate.of(2023, 1, 1), 1243),
            Datobeløp(LocalDate.of(2024, 1, 1), 1277),
        )

    private val brukersSvarfrist: Period = Period.ofWeeks(2)

    fun brukersSvarfrist(): LocalDate = LocalDate.now().plus(brukersSvarfrist)

    fun saksbehandlersTidsfrist(): LocalDate =
        LocalDate.now()
            .plusWeeks(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING.defaultVenteTidIUker)

    const val KRAVGRUNNLAG_XML_ROOT_ELEMENT: String = "urn:detaljertKravgrunnlagMelding"

    const val STATUSMELDING_XML_ROOT_ELEMENT: String = "urn:endringKravOgVedtakstatus"

    val rettsgebyr = rettsgebyrForDato.filter { it.gyldigFra <= LocalDate.now() }.maxByOrNull { it.gyldigFra }!!.beløp
    val FIRE_X_RETTSGEBYR = rettsgebyr * 4

    private class Datobeløp(val gyldigFra: LocalDate, val beløp: Long)

    const val BRUKER_ID_VEDTAKSLØSNINGEN = "VL"

    val MAKS_FEILUTBETALTBELØP_PER_YTELSE =
        mapOf<Ytelsestype, BigDecimal>(
            Ytelsestype.BARNETRYGD to BigDecimal.valueOf(500),
            Ytelsestype.BARNETILSYN to BigDecimal.valueOf(rettsgebyr).multiply(BigDecimal(0.5)),
            Ytelsestype.OVERGANGSSTØNAD to
                BigDecimal.valueOf(rettsgebyr)
                    .multiply(BigDecimal(0.5)),
            Ytelsestype.SKOLEPENGER to
                BigDecimal.valueOf(rettsgebyr)
                    .multiply(BigDecimal(0.5)),
            Ytelsestype.KONTANTSTØTTE to
                BigDecimal.valueOf(rettsgebyr)
                    .multiply(BigDecimal(0.5)),
        )

    const val AUTOMATISK_SAKSBEHANDLING_BEGRUNNELSE = "Automatisk satt verdi"
    const val AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_FAKTA_BEGRUNNELSE = "Automatisk behandling av tilbakekreving under 4 rettsgebyr. Ingen tilbakekreving."
    const val AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_FORELDELSE_BEGRUNNELSE = "Automatisk behandlet under 4 rettsgebyr på foreldet periode. Ikke relevant."
    const val AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_VILKÅRSVURDERING_BEGRUNNELSE = "Automatisk behandlet. Feilutbetaling under 4 rettsgebyr. Beløpet skal ikke tilbakebetales."
    const val AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_VILKÅRSVURDERING_AKTSOMHET_BEGRUNNELSE = "Automatisk behandlet. Feilutbetaling under 4 rettsgebyr. Bruker har ikke handlet forsettlig eller grovt uaktsomt."

    fun hentAutomatiskSaksbehandlingBegrunnelse(behandling: Behandling): String =
        when (behandling.saksbehandlingstype) {
            Saksbehandlingstype.ORDINÆR -> throw Feil("Kan ikke utlede automatisk saksbehandlingsbegrunnelse for ordinære saker")
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR -> behandling.begrunnelseForTilbakekreving ?: AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_FAKTA_BEGRUNNELSE
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP -> AUTOMATISK_SAKSBEHANDLING_BEGRUNNELSE
        }

    fun hentAutomatiskForeldelsesbegrunnelse(saksbehandlingstype: Saksbehandlingstype): String =
        when (saksbehandlingstype) {
            Saksbehandlingstype.ORDINÆR -> throw Feil("Kan ikke utlede automatisk foreldelsesbegrunnelse for ordinære saker")
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR -> AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_FORELDELSE_BEGRUNNELSE
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP -> AUTOMATISK_SAKSBEHANDLING_BEGRUNNELSE
        }

    fun hentAutomatiskVilkårsvurderingBegrunnelse(saksbehandlingstype: Saksbehandlingstype): String =
        when (saksbehandlingstype) {
            Saksbehandlingstype.ORDINÆR -> throw Feil("Kan ikke utlede automatisk vilkårsvurderingsbegrunnelse for ordinære saker")
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR -> AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_VILKÅRSVURDERING_BEGRUNNELSE
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP -> AUTOMATISK_SAKSBEHANDLING_BEGRUNNELSE
        }

    fun hentAutomatiskVilkårsvurderingAktsomhetBegrunnelse(saksbehandlingstype: Saksbehandlingstype): String =
        when (saksbehandlingstype) {
            Saksbehandlingstype.ORDINÆR -> throw Feil("Kan ikke utlede automatisk aktsomhetsbegrunnelse for ordinære saker")
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR -> AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_VILKÅRSVURDERING_AKTSOMHET_BEGRUNNELSE
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP -> AUTOMATISK_SAKSBEHANDLING_BEGRUNNELSE
        }
}

object PropertyName {
    const val FAGSYSTEM = "fagsystem"
    const val ENHET = "enhet"
    const val BESLUTTER = "beslutter"
}
