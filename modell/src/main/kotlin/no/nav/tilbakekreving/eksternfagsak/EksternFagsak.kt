package no.nav.tilbakekreving.eksternfagsak

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.api.v2.EksternFagsakDto
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.breeeev.standardtekster.HjemmelForTilbakekreving
import no.nav.tilbakekreving.entities.EksternFagsakEntity
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.time.LocalDate
import java.util.UUID

class EksternFagsak(
    private val id: UUID,
    private val eksternId: String?,
    private val eksternIdFraOppdrag: String,
    internal val ytelse: Ytelse,
    val behandlinger: EksternFagsakBehandlingHistorikk,
) {
    fun eksternId(): String = eksternId ?: eksternIdFraOppdrag

    fun tilFrontendDto(): EksternFagsakDto {
        return EksternFagsakDto(
            eksternId = eksternId(),
            ytelsestype = ytelse.tilYtelseDTO(),
            fagsystem = ytelse.tilFagsystemDTO(),
        )
    }

    fun lagre(fagsysteminfo: FagsysteminfoHendelse, klokke: Klokke): HistorikkReferanse<UUID, EksternFagsakRevurdering> {
        return behandlinger.lagre(
            EksternFagsakRevurdering.Revurdering(
                id = UUID.randomUUID(),
                eksternId = fagsysteminfo.revurdering.behandlingId,
                revurderingsårsak = fagsysteminfo.revurdering.årsak,
                årsakTilFeilutbetaling = fagsysteminfo.revurdering.årsakTilFeilutbetaling ?: "Ukjent",
                vedtaksdato = fagsysteminfo.revurdering.vedtaksdato,
                utvidedePerioder = fagsysteminfo.utvidPerioder?.map {
                    EksternFagsakRevurdering.UtvidetPeriode(
                        id = UUID.randomUUID(),
                        kravgrunnlagPeriode = it.kravgrunnlagPeriode,
                        vedtaksperiode = it.vedtaksperiode,
                    )
                } ?: emptyList(),
                url = fagsysteminfo.revurdering.url,
                opprettet = klokke.nå(),
            ),
        )
    }

    fun lagreTomBehandling(
        revurderingsdatoFraKravgrunnlag: LocalDate?,
        kravgrunnlagReferanse: String,
        klokke: Klokke,
    ): HistorikkReferanse<UUID, EksternFagsakRevurdering> {
        return behandlinger.lagre(
            EksternFagsakRevurdering.Ukjent(
                id = UUID.randomUUID(),
                eksternId = kravgrunnlagReferanse,
                revurderingsdatoFraKravgrunnlag = revurderingsdatoFraKravgrunnlag,
                opprettet = klokke.nå(),
            ),
        )
    }

    fun fagsysteminfoBehov(
        eksternBehandlingId: String,
        vedtakGjelderId: String,
    ): FagsysteminfoBehov {
        return FagsysteminfoBehov(
            eksternFagsakId = eksternIdFraOppdrag,
            eksternBehandlingId = eksternBehandlingId,
            vedtakGjelderId = vedtakGjelderId,
            ytelse = ytelse,
        )
    }

    fun tilEntity(tilbakekrevingId: String): EksternFagsakEntity {
        return EksternFagsakEntity(
            id = id,
            tilbakekrevingRef = tilbakekrevingId,
            eksternIdFraOppdrag = eksternIdFraOppdrag,
            eksternId = eksternId,
            ytelseEntity = ytelse.tilEntity(),
            behandlinger = behandlinger.tilEntity(id),
        )
    }

    fun forhåndsvarselHjemlerForTilbakekreving() = buildList {
        addAll(ytelse.hjemlerForTilbakekreving())
        addAll(HjemmelForTilbakekreving.standardForhåndsvarselHjemler(beregnerRenter = true))
    }

    fun hentYtelse() = ytelse

    fun saksbehandlingUrl(): String? {
        if (!behandlinger.harBehandling()) return null
        return behandlinger.nåværende().entry.url
    }
}
