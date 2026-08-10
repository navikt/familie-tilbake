package no.nav.familie.tilbake.bigQuery

import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.tilbakekreving.api.v1.dto.BigQueryBehandlingDataDto
import no.nav.tilbakekreving.bigquery.BigQueryService
import no.nav.tilbakekreving.kontrakter.Varsel
import no.nav.tilbakekreving.kontrakter.periode.til
import org.springframework.stereotype.Service

@Service
class BigQueryAdapterService(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val bigQueryService: BigQueryService,
    private val fagsakRepository: FagsakRepository,
) {
    fun oppdaterBigQuery(
        behandling: Behandling,
        harTilleggsfrist: Boolean?,
        varsel: Varsel?,
    ) {
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingId(behandling.id).lastOrNull()
        val tilbakekreving = fagsakRepository.finnFagsakForBehandlingId(behandling.id)
        bigQueryService.oppdaterBehandling(
            BigQueryBehandlingDataDto(
                behandlingId = behandling.id.toString(),
                opprettetDato = behandling.opprettetTidspunkt,
                periode = kravgrunnlag?.samletPeriode() ?: varsel?.perioder?.let { periode -> periode.minOf { it.fom } til periode.maxOf { it.tom } },
                behandlingstype = behandling.type.name,
                ytelse = tilbakekreving.fagsystem.name,
                beløp = kravgrunnlag?.sumFeilutbetaling()?.toLong() ?: varsel?.sumFeilutbetaling?.toLong(),
                enhetNavn = behandling.behandlendeEnhetsNavn,
                enhetKode = behandling.behandlendeEnhet,
                status = behandling.status.name,
                resultat = behandling.sisteResultat?.type?.name,
                harTilleggsfrist = harTilleggsfrist,
                tilbakekrevingId = tilbakekreving.id.toString(),
                oppdagetAv = null,
            ),
        )
    }
}
