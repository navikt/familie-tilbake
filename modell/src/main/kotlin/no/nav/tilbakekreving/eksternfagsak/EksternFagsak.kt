package no.nav.tilbakekreving.eksternfagsak

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v2.EksternFagsakDto
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.entities.EksternFagsakEntity
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.time.LocalDate
import java.util.UUID

class EksternFagsak(
    val eksternId: String,
    internal val ytelse: Ytelse,
    val behandlinger: EksternFagsakBehandlingHistorikk,
    private val behovObservatør: BehovObservatør,
) : FrontendDto<EksternFagsakDto> {
    override fun tilFrontendDto(): EksternFagsakDto {
        return EksternFagsakDto(
            eksternId = eksternId,
            ytelsestype = ytelse.tilYtelseDTO(),
            fagsystem = ytelse.tilFagsystemDTO(),
        )
    }

    fun lagre(fagsysteminfo: FagsysteminfoHendelse): HistorikkReferanse<UUID, EksternFagsakRevurdering> {
        return behandlinger.lagre(
            EksternFagsakRevurdering.Revurdering(
                internId = UUID.randomUUID(),
                eksternId = fagsysteminfo.revurdering.behandlingId,
                revurderingsårsak = fagsysteminfo.revurdering.årsak,
                årsakTilFeilutbetaling = fagsysteminfo.revurdering.årsakTilFeilutbetaling ?: "Ukjent",
                vedtaksdato = fagsysteminfo.revurdering.vedtaksdato,
                utvidedePerioder = fagsysteminfo.revurdering.utvidPerioder?.map {
                    EksternFagsakRevurdering.UtvidetPeriode(
                        kravgrunnlagPeriode = it.kravgrunnlagPeriode,
                        vedtaksperiode = it.vedtaksperiode,
                    )
                } ?: emptyList(),
            ),
        )
    }

    fun lagreTomBehandling(revurderingsdatoFraKravgrunnlag: LocalDate?): HistorikkReferanse<UUID, EksternFagsakRevurdering> {
        return behandlinger.lagre(
            EksternFagsakRevurdering.Ukjent(
                internId = UUID.randomUUID(),
                revurderingsdatoFraKravgrunnlag = revurderingsdatoFraKravgrunnlag,
            ),
        )
    }

    fun trengerFagsysteminfo(
        eksternBehandlingId: String,
        vedtakGjelderId: String,
    ) {
        behovObservatør.håndter(
            FagsysteminfoBehov(
                eksternFagsakId = eksternId,
                eksternBehandlingId = eksternBehandlingId,
                vedtakGjelderId = vedtakGjelderId,
                ytelse = ytelse,
            ),
        )
    }

    fun tilEntity(): EksternFagsakEntity {
        return EksternFagsakEntity(
            eksternId = eksternId,
            ytelseEntity = ytelse.tilEntity(),
            behandlinger = behandlinger.tilEntity(),
        )
    }
}
