package no.nav.tilbakekreving.behov

import no.nav.tilbakekreving.fagsystem.Ytelse
import java.util.UUID

class InfobrevDistribusjonBehov(
    val brevId: UUID,
    val behandlingId: UUID,
    val fagsakId: String,
    val journalpostId: String,
    val dokumentInfoId: String,
    val ytelse: Ytelse,
) : Behov
