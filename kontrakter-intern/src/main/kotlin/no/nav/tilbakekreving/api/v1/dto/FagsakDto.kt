package no.nav.tilbakekreving.api.v1.dto

import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.bruker.FrontendBrukerDto
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import java.util.UUID

data class FagsakDto(
    val eksternFagsakId: String,
    val ytelsestype: YtelsestypeDTO,
    val fagsystem: FagsystemDTO,
    val språkkode: Språkkode,
    val bruker: FrontendBrukerDto,
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
