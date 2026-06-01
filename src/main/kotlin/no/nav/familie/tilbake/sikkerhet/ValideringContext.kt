package no.nav.familie.tilbake.sikkerhet

enum class ValideringContext(
    val minimumBehandlerrolle: Behandlerrolle,
    val auditLoggerEvent: AuditLoggerEvent,
    val handling: String,
) {
    HentBehandling(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter tilbakekrevingsbehandling",
    ),
    HentFakta(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter fakta om feilutbetalingen",
    ),
    TrekkTilbakeFraGodkjenning(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.UPDATE,
        handling = "Saksbehandler angrer på send til beslutter og tar behandling tilbake til saksbehandler",
    ),
    OppdaterFakta(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.UPDATE,
        handling = "Oppdaterer fakta om feilutbetalingen",
    ),
    FatteVedtak(
        minimumBehandlerrolle = Behandlerrolle.BESLUTTER,
        auditLoggerEvent = AuditLoggerEvent.UPDATE,
        handling = "Fattet vedtak",
    ),
    HentVedtaksbrevTekster(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter vedtaksbrevtekst",
    ),
    UtførSteg(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.UPDATE,
        handling = "Utfører behandlingens aktiv steg og fortsetter den til neste steg",
    ),
    BeregnFeilutbetaling(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter beregningsresultat",
    ),
    BestillBrev(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.CREATE,
        handling = "Sender brev",
    ),
    ForhåndsvisBrev(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Forhåndsviser brev",
    ),
    HentForhåndsvarselTekster(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter varselbrevtekst",
    ),
    RegistrerUtsattFrist(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Utsette frist på uttalelsen",
    ),
    RegistrerForhåndsvarselUnntak(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Sender ikke forhåndsvarsel",
    ),
    RegistrerBrukeruttalelse(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.CREATE,
        handling = "Lagrer brukers uttalelse",
    ),
    HentFagsak(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter fagsak informasjon med bruker og behandlinger",
    ),
    HentForeldelse(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter foreldelsesinformasjon for en gitt behandling",
    ),
    HentHistorikk(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter historikkinnslag",
    ),
    HentPerioder(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter periodeinformasjon",
    ),
    HentTotrinn(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter totrinnsvurderinger for en gitt behandling",
    ),
    HentVilkårsvurdering(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter vilkårsvurdering for en gitt behandling",
    ),
    HentJournalpost(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter journalpost",
    ),
    HentForvaltningsinfo(
        minimumBehandlerrolle = Behandlerrolle.FORVALTER,
        auditLoggerEvent = AuditLoggerEvent.NONE,
        handling = "Henter forvaltningsinformasjon",
    ),
    ForvaltningFlyttTilFakta(
        minimumBehandlerrolle = Behandlerrolle.FORVALTER,
        auditLoggerEvent = AuditLoggerEvent.UPDATE,
        handling = "Flytter behandling tilbake til Fakta",
    ),
    ForvaltningOppdaterFagsysteminfo(
        minimumBehandlerrolle = Behandlerrolle.FORVALTER,
        auditLoggerEvent = AuditLoggerEvent.UPDATE,
        handling = "Oppdaterer fagsysteminfo",
    ),
    HentVedtaksbrevData(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter informasjon for bruk i brev",
    ),
    HentVedtaksresultat(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter vedtaksresultat",
    ),
    ForeslåVedtak(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.UPDATE,
        handling = "Foreslår vedtak",
    ),
    HentForhåndsvarsel(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter forhåndsvarselinformasjon",
    ),
    UtsettUttalelsesfrist(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.UPDATE,
        handling = "Utsetter uttalelsesfrist",
    ),
    LagreForhåndsvarselUnntak(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.UPDATE,
        handling = "Lagrer unntak for forhåndsvarsel",
    ),
    HentDokumentInfo(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter dokumentinfo",
    ),
    HentFaktaFeilutbetaling(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter fakta om feilutbetalingen",
    ),
    FlyttBehandlingTilFakta(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.UPDATE,
        handling = "Flytter behandling tilbake til Fakta",
    ),
    HentForhåndsvarselinformasjon(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter forhåndsvarselinformasjon",
    ),
    HentJournalposter(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter journalposter",
    ),
    SjekkPerioderSammenslått(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Sjekker om perioder er sammenslått",
    ),
    OppdaterVedtaksbrev(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.UPDATE,
        handling = "Oppdaterer vedtaksbrev",
    ),
    HentDokument(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter dokument",
    ),
    HentBehandlingslogg(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        auditLoggerEvent = AuditLoggerEvent.ACCESS,
        handling = "Henter behandlingslogg",
    ),
    SendVarselbrev(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.CREATE,
        handling = "Sender varselbrev",
    ),
    LagreBrukersuttalelse(
        minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
        auditLoggerEvent = AuditLoggerEvent.CREATE,
        handling = "Lagrer brukers uttalelse",
    ),
}
