package no.nav.familie.tilbake.kontrakter.oppgave

import no.nav.familie.tilbake.kontrakter.Tema
import java.time.LocalDate

data class OpprettOppgaveRequest(
    val ident: OppgaveIdentV2?,
    val enhetsnummer: String?,
    val saksId: String?,
    val journalpostId: String? = null,
    val tema: Tema,
    val oppgavetype: Oppgavetype,
    val behandlingstema: String?,
    val tilordnetRessurs: String? = null,
    val fristFerdigstillelse: LocalDate,
    val aktivFra: LocalDate = LocalDate.now(),
    val beskrivelse: String,
    val prioritet: OppgavePrioritet = OppgavePrioritet.NORM,
    val behandlingstype: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val mappeId: Long? = null,
)

enum class Oppgavetype(
    val value: String,
) {
    BehandleSak("BEH_SAK"),
    Journalføring("JFR"),
    GodkjenneVedtak("GOD_VED"),
    BehandleUnderkjentVedtak("BEH_UND_VED"),
    Fordeling("FDR"),
    BehandleReturpost("RETUR"),
    BehandleSED("BEH_SED"),
    BehandleUderkjentVedtak("BEH_UND_VED"),
    FordelingSED("FDR_SED"),
    Fremlegg("FREM"),
    Generell("GEN"),
    InnhentDokumentasjon("INNH_DOK"),
    JournalføringUtgående("JFR_UT"),
    KontaktBruker("KONT_BRUK"),
    KontrollerUtgåendeSkannetDokument("KON_UTG_SCA_DOK"),
    SvarIkkeMottatt("SVAR_IK_MOT"),
    VurderDokument("VUR"),
    VurderHenvendelse("VURD_HENV"),
    VurderInntekt("VURD_INNT"),
    VurderKonsekvensForYtelse("VUR_KONS_YTE"),
    VurderLivshendelse("VURD_LIVS"),
    VurderSvar("VUR_SVAR"),
}

enum class Behandlingstype(
    val value: String,
) {
    Utland("ae0106"),
    NASJONAL("ae0118"),
    EØS("ae0120"),
    Tilbakekreving("ae0161"),
}

enum class OppgavePrioritet {
    HOY,
    NORM,
    LAV,
}

@Deprecated("Misforstått bruk - ulike identer på tvers av miljøer")
enum class Enhetsmappe(
    val value: Long,
) {
    SøknaderKlarTilBehandling(100000035),
    AvventerDokumentasjon(100000036),
    KlagerKlarTilBehandling(100000037),
    RevurderingKlarTilBehandling(100000038),
    Revurdering(100000039),
    OppfølgingAvSkolesaker(100024196),
    TilbakekrevingKlarTilBehandling(100000266),
    Flyttesaker(100024195),
    EØSMedlemskap(100025358),
    Corona(100025133),
}
