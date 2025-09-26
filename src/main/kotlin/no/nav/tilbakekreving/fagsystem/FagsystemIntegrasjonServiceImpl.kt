package no.nav.tilbakekreving.fagsystem

import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.api.v2.fagsystem.svar.FagsysteminfoSvarHendelse
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.kontrakter.periode.til
import org.springframework.stereotype.Service

@Service
class FagsystemIntegrasjonServiceImpl(
    private val tilbakekrevingService: TilbakekrevingService,
) : FagsystemIntegrasjonService {
    override fun håndter(ytelse: Ytelse, fagsysteminfo: FagsysteminfoSvarHendelse) {
        tilbakekrevingService.hentTilbakekreving(
            fagsystem = ytelse.tilFagsystemDTO(),
            eksternFagsakId = fagsysteminfo.eksternFagsakId,
        ) { tilbakekreving ->
            tilbakekreving.håndter(
                FagsysteminfoHendelse(
                    aktør = Aktør.Person(""),
                    revurdering = FagsysteminfoHendelse.Revurdering(
                        behandlingId = fagsysteminfo.revurdering.behandlingId,
                        årsak = when (fagsysteminfo.revurdering.årsak) {
                            FagsysteminfoSvarHendelse.RevurderingDto.Årsak.NYE_OPPLYSNINGER -> EksternFagsakRevurdering.Revurderingsårsak.NYE_OPPLYSNINGER
                            FagsysteminfoSvarHendelse.RevurderingDto.Årsak.KORRIGERING -> EksternFagsakRevurdering.Revurderingsårsak.KORRIGERING
                            FagsysteminfoSvarHendelse.RevurderingDto.Årsak.KLAGE -> EksternFagsakRevurdering.Revurderingsårsak.KLAGE
                            FagsysteminfoSvarHendelse.RevurderingDto.Årsak.UKJENT -> EksternFagsakRevurdering.Revurderingsårsak.UKJENT
                        },
                        årsakTilFeilutbetaling = fagsysteminfo.revurdering.årsakTilFeilutbetaling,
                        vedtaksdato = fagsysteminfo.revurdering.vedtaksdato,
                    ),
                    utvidPerioder = fagsysteminfo.utvidPerioder?.map {
                        FagsysteminfoHendelse.UtvidetPeriode(
                            kravgrunnlagPeriode = it.kravgrunnlagPeriode.fom til it.kravgrunnlagPeriode.tom,
                            vedtaksperiode = it.vedtaksperiode.fom til it.vedtaksperiode.tom,
                        )
                    },
                ),
            )
        }
    }
}
