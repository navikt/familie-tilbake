package no.nav.tilbakekreving.behandlingslogg

import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.historikk.Historikkinnslagstype

enum class Behandlingsloggstype(
    val tittel: String,
    val tekst: String?,
    val type: Historikkinnslagstype = Historikkinnslagstype.HENDELSE,
    val steg: Behandlingssteg?,
) {
    BEHANDLING_OPPRETTET(
        tittel = "Behandling opprettet",
        tekst = null,
        steg = null,
    ),

    FAGSYSTEMINFO_HENT(
        tittel = "Fagsysteminfo innhentet",
        tekst = null,
        steg = null,
    ),

    BRUKERINFO_HENT(
        tittel = "Brukerinfo innhentet",
        tekst = null,
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

    ANGRE_SEND_TIL_BESLUTTER(
        tittel = "Angre send til beslutter",
        tekst = null,
        steg = null,
    ),

    BEHANDLING_SENDT_TILBAKE_TIL_SAKSBEHANDLER(
        tittel = "Vedtak underkjent",
        tekst = null,
        steg = null,
    ),

    VEDTAK_FATTET(
        tittel = "Vedtak fattet",
        tekst = "Resultat: ",
        steg = null,
    ),

    BEHANDLING_AVSLUTTET(
        tittel = "Behandling avsluttet",
        tekst = null,
        steg = null,
    ),

    // Brev type
    VARSELBREV_SENDT(
        tittel = "Varselbrev sendt",
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
    DOKUMENT_JOURNALFØRT(
        tittel = "Dokument journalført",
        tekst = null,
        steg = null,
    ),
    BEHANDLING_NULLSTILLT(
        tittel = "Behandling nullstilt",
        tekst = null,
        steg = null,
    ),
    BEHANDLING_PÅ_VENT(
        tittel = "Behandling er satt på vent",
        tekst = "Årsak: ",
        steg = null,
    ),
}
