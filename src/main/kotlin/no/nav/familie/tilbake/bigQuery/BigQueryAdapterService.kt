package no.nav.familie.tilbake.bigQuery

import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.tilbakekreving.api.v1.dto.BigQueryBehandlingDataDto
import no.nav.tilbakekreving.bigquery.BigQueryService
import no.nav.tilbakekreving.kontrakter.Varsel
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.repository.NyFaktavurderingRepository
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import no.nav.tilbakekreving.repository.TilbakekrevingRepository
import org.springframework.stereotype.Service

@Service
class BigQueryAdapterService(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val fagsakService: FagsakService,
    private val bigQueryService: BigQueryService,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val nyFaktavurderingRepository: NyFaktavurderingRepository,
) {
    fun oppdaterBigQuery(
        behandling: Behandling,
        harTilleggsfrist: Boolean?,
        varsel: Varsel?,
    ) {
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingId(behandling.id).lastOrNull()
        val ytelsestype = fagsakService.finnFagsystemForBehandlingId(behandling.id).navn
        val tilbakekreving = tilbakekrevingRepository.hentTilbakekreving(TilbakekrevingFilter.behandling(behandling.id))
        val faktasteg = tilbakekreving?.let { nyFaktavurderingRepository.hentFaktavurdering(behandling.id) }
        bigQueryService.oppdaterBehandling(
            BigQueryBehandlingDataDto(
                behandlingId = behandling.id.toString(),
                opprettetDato = behandling.opprettetTidspunkt,
                periode = kravgrunnlag?.samletPeriode() ?: varsel?.perioder?.let { periode -> periode.minOf { it.fom } til periode.maxOf { it.tom } },
                behandlingstype = behandling.type.name,
                ytelse = ytelsestype,
                beløp = kravgrunnlag?.sumFeilutbetaling()?.toLong() ?: varsel?.sumFeilutbetaling?.toLong(),
                enhetNavn = behandling.behandlendeEnhetsNavn,
                enhetKode = behandling.behandlendeEnhet,
                status = behandling.status.name,
                resultat = behandling.sisteResultat?.type?.name,
                harTilleggsfrist = harTilleggsfrist,
                tilbakekrevingId = tilbakekreving?.id,
                oppdagetAv = faktasteg?.oppdaget?.av?.name,
            ),
        )
    }
}
