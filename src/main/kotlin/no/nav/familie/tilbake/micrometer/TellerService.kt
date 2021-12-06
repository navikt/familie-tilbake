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
import no.nav.familie.tilbake.micrometer.domain.Meldingstelling
import no.nav.familie.tilbake.micrometer.domain.MeldingstellingRepository
import no.nav.familie.tilbake.micrometer.domain.Meldingstype
import no.nav.familie.tilbake.micrometer.domain.Mottaksstatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class TellerService(private val fagsakRepository: FagsakRepository,
                    private val meldingstellingRepository: MeldingstellingRepository) {

    fun tellKobletKravgrunnlag(ytelsestype: Ytelsestype) =
            tellMelding(ytelsestype, Meldingstype.KRAVGRUNNLAG, Mottaksstatus.KOBLET)

    fun tellUkobletKravgrunnlag(ytelsestype: Ytelsestype) =
            tellMelding(ytelsestype, Meldingstype.KRAVGRUNNLAG, Mottaksstatus.UKOBLET)

    fun tellKobletStatusmelding(ytelsestype: Ytelsestype) =
            tellMelding(ytelsestype, Meldingstype.STATUSMELDING, Mottaksstatus.KOBLET)

    fun tellUkobletStatusmelding(ytelsestype: Ytelsestype) =
            tellMelding(ytelsestype, Meldingstype.STATUSMELDING, Mottaksstatus.UKOBLET)

    fun tellMelding(ytelsestype: Ytelsestype, type: Meldingstype, status: Mottaksstatus) {

        val meldingstelling = meldingstellingRepository.findByYtelsestypeAndAndTypeAndStatusAndDato(ytelsestype,
                                                                                                    type,
                                                                                                    status,
                                                                                                    LocalDate.now())
        if (meldingstelling == null) {
            meldingstellingRepository.insert(Meldingstelling(ytelsestype = ytelsestype,
                                                             type = type,
                                                             status = status))
        } else {
            meldingstellingRepository.oppdaterTeller(ytelsestype, type, status)
        }
    }

    fun tellBrevSendt(fagsak: Fagsak, brevtype: Brevtype) {
        Metrics.counter("Brevteller",
                        Tags.of("ytelse", fagsak.ytelsestype.kode,
                                "brevtype", brevtype.name)).increment()
    }

    fun tellVedtak(behandlingsresultatstype: Behandlingsresultatstype, behandling: Behandling) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val vedtakstype = if (behandlingsresultatstype in Behandlingsresultat.ALLE_HENLEGGELSESKODER)
            Behandlingsresultatstype.HENLAGT.name else behandlingsresultatstype.name

        Metrics.counter("Vedtaksteller",
                        Tags.of("ytelse", fagsak.ytelsestype.kode,
                                "vedtakstype", vedtakstype)).increment()
    }

}