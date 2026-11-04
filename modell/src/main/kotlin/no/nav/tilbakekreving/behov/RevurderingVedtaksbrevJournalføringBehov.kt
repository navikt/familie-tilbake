package no.nav.tilbakekreving.behov

import no.nav.tilbakekreving.aktør.Brukerinfo
import no.nav.tilbakekreving.breeeev.RevurderingVedtaksbrevInfo
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.saksbehandler.Behandler
import java.util.UUID

class RevurderingVedtaksbrevJournalføringBehov(
    val brevId: UUID,
    val behandlingId: UUID,
    val ytelse: Ytelse,
    val bruker: Brukerinfo,
    val fagsakId: String,
    val journalførendeEnhet: String,
    val revurderingVedtaksbrevInfo: RevurderingVedtaksbrevInfo,
    val beslutter: Behandler,
) : Behov
