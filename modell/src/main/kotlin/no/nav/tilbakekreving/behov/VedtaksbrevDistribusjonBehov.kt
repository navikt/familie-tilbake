package no.nav.tilbakekreving.behov

import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import java.util.UUID

class VedtaksbrevDistribusjonBehov(
    val behandlingId: UUID,
    val brevId: UUID,
    val fagsakId: String,
    val journalpostId: String,
    val fagsystem: FagsystemDTO,
    val dokumentInfoId: String,
) : Behov
