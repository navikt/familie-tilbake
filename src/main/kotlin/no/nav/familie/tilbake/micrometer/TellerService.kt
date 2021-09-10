package no.nav.familie.tilbake.micrometer

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tags
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import org.springframework.stereotype.Service

@Service
class TellerService(private val fagsakRepository: FagsakRepository) {

    fun tellKobletKravgrunnlag(ytelsestype: Ytelsestype) {
        Metrics.counter("xmlTeller",
                        Tags.of("ytelse", ytelsestype.kode,
                                "type", "kravgrunnlag",
                                "mottagsstatus", "koblet")).increment()
    }

    fun tellUkobletKravgrunnlag(ytelsestype: Ytelsestype) {
        Metrics.counter("xmlTeller",
                        Tags.of("ytelse", ytelsestype.kode,
                                "type", "kravgrunnlag",
                                "mottagsstatus", "ukoblet")).increment()
    }

    fun tellKobletStatusmelding(ytelsestype: Ytelsestype) {
        Metrics.counter("xmlTeller",
                        Tags.of("ytelse", ytelsestype.kode,
                                "type", "statusmelding",
                                "mottagsstatus", "koblet")).increment()
    }

    fun tellUkobletStatusmelding(ytelsestype: Ytelsestype) {
        Metrics.counter("xmlTeller",
                        Tags.of("ytelse", ytelsestype.kode,
                                "type", "statusmelding",
                                "mottagsstatus", "ukoblet")).increment()
    }

    fun tellBrevSendt(fagsak: Fagsak, brevtype: Brevtype) {
        Metrics.counter("Brevteller",
                        Tags.of("ytelse", fagsak.ytelsestype.kode,
                                "brevtype", brevtype.name)).increment()

    }

    fun tellVedtak(behandlingsresultatstype: Behandlingsresultatstype, behandling: Behandling) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val vedtakstype = if (behandlingsresultatstype in Behandlingsresultat.ALLE_HENLEGGELSESKODER)
            Behandlingsresultatstype.HENLAGT.navn else behandlingsresultatstype.navn

        Metrics.counter("Vedtaksteller",
                        Tags.of("ytelse", fagsak.ytelsestype.kode,
                                "vedtakstype", vedtakstype)).increment()
    }

}