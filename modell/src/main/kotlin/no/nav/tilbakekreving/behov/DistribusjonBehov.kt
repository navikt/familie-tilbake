package no.nav.tilbakekreving.behov

import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import java.util.UUID

class DistribusjonBehov(
    val behandlingId: UUID,
    val journalpostId: String,
    val fagsystem: FagsystemDTO,
) : Behov
