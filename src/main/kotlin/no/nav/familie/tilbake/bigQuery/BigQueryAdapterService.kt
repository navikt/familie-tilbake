package no.nav.familie.tilbake.bigQuery

import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.tilbakekreving.api.v1.dto.BigQueryBehandlingDataDto
import no.nav.tilbakekreving.bigquery.BigQueryService
import org.springframework.stereotype.Service

@Service
class BigQueryAdapterService(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val fagsakService: FagsakService,
    private val bigQueryService: BigQueryService,
) {
    fun oppdaterBigQuery(behandling: Behandling) {
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingId(behandling.id).lastOrNull()
        val ytelsestype = fagsakService.finnFagsystemForBehandlingId(behandling.id).navn
        bigQueryService.oppdaterBehandling(
            BigQueryBehandlingDataDto(
                behandlingId = behandling.id.toString(),
                opprettetDato = behandling.opprettetTidspunkt,
                periode = kravgrunnlag?.samletPeriode(),
                behandlingstype = behandling.type.name,
                ytelse = ytelsestype,
                beløp = kravgrunnlag?.sumFeilutbetaling()?.toLong(),
                enhetNavn = behandling.behandlendeEnhetsNavn,
                enhetKode = behandling.behandlendeEnhet,
                status = behandling.status.name,
                resultat = behandling.sisteResultat?.type?.name,
            ),
        )
    }
}
