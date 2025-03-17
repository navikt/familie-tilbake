package no.nav.tilbakekreving.api.v1.dto

import java.util.UUID
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.bruker.BrukerDto
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype

data class FagsakDto(
    val eksternFagsakId: String,
    val ytelsestype: Ytelsestype,
    val fagsystem: Fagsystem,
    val språkkode: Språkkode,
    val bruker: BrukerDto,
    val behandlinger: List<BehandlingsoppsummeringDto>,
    val institusjon: InstitusjonDto? = null,
)

data class BehandlingsoppsummeringDto(
    val behandlingId: UUID,
    val eksternBrukId: UUID,
    val type: Behandlingstype,
    val status: Behandlingsstatus,
)

data class InstitusjonDto(
    val organisasjonsnummer: String,
    val navn: String,
)
