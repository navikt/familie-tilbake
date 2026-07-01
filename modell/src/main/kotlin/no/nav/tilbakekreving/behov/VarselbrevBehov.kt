package no.nav.tilbakekreving.behov

import no.nav.tilbakekreving.aktør.Brukerinfo
import no.nav.tilbakekreving.brev.VarselbrevInfo
import no.nav.tilbakekreving.fagsystem.Ytelse
import java.util.UUID

data class VarselbrevJournalføringBehov(
    val brukerinfo: Brukerinfo,
    val behandlingId: UUID,
    val ytelse: Ytelse,
    val info: VarselbrevInfo,
    val gjelderDødsfall: Boolean,
    val tilbakekrevingId: String,
) : Behov

data class VarselbrevDistribusjonBehov(
    val brevId: UUID,
    val behandlingId: UUID,
    val fagsakId: String,
    val journalpostId: String,
    val ytelse: Ytelse,
    val behandlerIdent: String,
    val dokumentInfoId: String,
) : Behov
