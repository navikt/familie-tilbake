package no.nav.tilbakekreving.behandlingslogg

import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.historikk.Historikkinnslagstype
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class Behandlingsloggstype(
    val tittel: String,
    val tekst: String?,
    val type: Historikkinnslagstype,
    val steg: Behandlingssteg?,
) {
    KRAVGRUNNLAG_MOTTATT(
        tittel = "Kravgrunnlag mottatt",
        tekst = null,
        type = Historikkinnslagstype.HENDELSE,
        steg = null,
    ),
    TILBAKEKREVING_OPPRETTET(
        tittel = "Tilbakekreving opprettet",
        tekst = null,
        type = Historikkinnslagstype.HENDELSE,
        steg = null,
    ),

    BEHANDLING_OPPRETTET(
        tittel = "Behandling opprettet",
        tekst = null,
        type = Historikkinnslagstype.HENDELSE,
        steg = null,
    ),

    FAGSYSTEMINFO_OPPDATERT(
        tittel = "Fagsysteminfo oppdatert",
        tekst = null,
        type = Historikkinnslagstype.HENDELSE,
        steg = null,
    ),

    BRUKERINFO_OPPDATERT(
        tittel = "Brukerinfo oppdatert",
        tekst = null,
        type = Historikkinnslagstype.HENDELSE,
        steg = null,
    ),

    // Skjermlenke type
    FAKTA_VURDERT(
        tittel = "Fakta vurdert",
        tekst = null,
        type = Historikkinnslagstype.SKJERMLENKE,
        steg = Behandlingssteg.FAKTA,
    ),
    FORELDELSE_VURDERT(
        tittel = "Foreldelse vurdert",
        tekst = null,
        type = Historikkinnslagstype.SKJERMLENKE,
        steg = Behandlingssteg.FORELDELSE,
    ),
    VILKÅRSVURDERING_VURDERT(
        tittel = "Vilkår vurdert",
        tekst = null,
        type = Historikkinnslagstype.SKJERMLENKE,
        steg = Behandlingssteg.VILKÅRSVURDERING,
    ),
    FORESLÅ_VEDTAK_VURDERT(
        tittel = "Vedtak foreslått og sendt til beslutter",
        tekst = null,
        type = Historikkinnslagstype.SKJERMLENKE,
        steg = Behandlingssteg.FORESLÅ_VEDTAK,
    ),

    TREKK_TILBAKE_FRA_GODKJENNING(
        tittel = "Trekk tilbake fra godkjenning",
        tekst = null,
        type = Historikkinnslagstype.HENDELSE,
        steg = null,
    ),

    BEHANDLING_SENDT_TILBAKE_TIL_SAKSBEHANDLER(
        tittel = "Vedtak underkjent",
        tekst = null,
        type = Historikkinnslagstype.HENDELSE,
        steg = null,
    ),

    VEDTAK_FATTET(
        tittel = "Vedtak fattet",
        tekst = null,
        type = Historikkinnslagstype.HENDELSE,
        steg = null,
    ),

    BEHANDLING_AVSLUTTET(
        tittel = "Behandling avsluttet",
        tekst = null,
        type = Historikkinnslagstype.HENDELSE,
        steg = null,
    ),

    // Brev type
    @Deprecated("Deprecated ettersom FORHÅNDSVARSEL_SENDT er laget")
    VARSELBREV_SENDT(
        tittel = "Varselbrev sendt",
        tekst = "Varselbrev tilbakekreving",
        type = Historikkinnslagstype.BREV,
        steg = null,
    ),
    FORHÅNDSVARSEL_SENDT(
        tittel = "Forhåndsvarsel sendt",
        tekst = "Varselbrev tilbakekreving",
        type = Historikkinnslagstype.BREV,
        steg = null,
    ),
    VEDTAKSBREV_SENDT(
        tittel = "Vedtaksbrev sendt",
        tekst = "Vedtak om tilbakekreving",
        type = Historikkinnslagstype.BREV,
        steg = null,
    ),
    VARSELBREV_JOURNALFØRT(
        tittel = "Varselbrev journalført",
        tekst = null,
        type = Historikkinnslagstype.HENDELSE,
        steg = null,
    ),
    VEDTAKSBREV_JOURNALFØRT(
        tittel = "Vedtksbrev journalført",
        tekst = null,
        type = Historikkinnslagstype.HENDELSE,
        steg = null,
    ),
    BEHANDLING_NULLSTILLT(
        tittel = "Behandling nullstilt",
        tekst = null,
        type = Historikkinnslagstype.HENDELSE,
        steg = null,
    ),
    BEHANDLING_PÅ_VENT(
        tittel = "Behandling er satt på vent",
        tekst = null,
        type = Historikkinnslagstype.HENDELSE,
        steg = null,
    ),
    UTSETT_UTTALELSESFRIST(
        tittel = "Ny frist for uttalelse",
        tekst = null,
        type = Historikkinnslagstype.HENDELSE,
        steg = null,
    ) {
        override fun hentTittel(ekstraInfo: Map<EkstraInfo, Any>): String {
            val nyFrist = (ekstraInfo[EkstraInfo.NY_FRIST_FOR_UTTALELSE] as LocalDate).toString()
            return "$tittel: ${nyFrist.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}"
        }

        override fun hentTekst(ekstraInfo: Map<EkstraInfo, Any>): String {
            return ekstraInfo[EkstraInfo.BEGRUNNELSE_FOR_UTSATT_FRIST] as String
        }
    },
    BRUKER_UTTALELSE(
        tittel = "Bruker uttalelse er registrert",
        tekst = null,
        type = Historikkinnslagstype.HENDELSE,
        steg = null,
    ),
    UNNTAK_FOR_UTTALELSE(
        tittel = "Unntak for uttalelse er registrert",
        tekst = null,
        type = Historikkinnslagstype.HENDELSE,
        steg = null,
    ),
    ;

    open fun hentTittel(ekstraInfo: Map<EkstraInfo, Any>): String {
        return tittel
    }

    open fun hentTekst(ekstraInfo: Map<EkstraInfo, Any>): String? {
        return tekst
    }
}

enum class EkstraInfo() {
    BREV_REF,
    NY_FRIST_FOR_UTTALELSE,
    BEGRUNNELSE_FOR_UTSATT_FRIST,
    JOURNALPOST_ID,
    DOKUMENTINFO_ID,
}
