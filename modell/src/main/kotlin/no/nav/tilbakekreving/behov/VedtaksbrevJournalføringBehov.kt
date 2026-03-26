package no.nav.tilbakekreving.behov

import no.nav.tilbakekreving.aktør.Brukerinfo
import no.nav.tilbakekreving.breeeev.VedtaksbrevInfo
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import java.util.UUID

class VedtaksbrevJournalføringBehov(
    val brevId: UUID,
    val behandlingId: UUID,
    val ytelse: Ytelse,
    val bruker: Brukerinfo,
    val fagsakId: String,
    val journalførendeEnhet: String,
    val vedtaksbrevInfo: VedtaksbrevInfo,
    val vedtaksresultat: Vedtaksresultat,
) : Behov
