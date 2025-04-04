package no.nav.familie.tilbake

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.ApplicationProperties
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeldelseDto
import no.nav.tilbakekreving.api.v2.EksternFagsakDto
import no.nav.tilbakekreving.api.v2.OpprettTilbakekrevingEvent
import no.nav.tilbakekreving.api.v2.Opprettelsevalg
import no.nav.tilbakekreving.behandling.BehandlingHistorikk
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.person.Bruker
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.Random
import java.util.UUID

@Service
class TilbakekrevingService(
    private val applicationProperties: ApplicationProperties,
) {
    private val fnr = "20046912345"
    private val behovObservatør =
        object : BehovObservatør {
            override fun håndter(behov: FagsysteminfoBehov) {}

            override fun håndter(behov: VarselbrevBehov) {}
        }

    private val eksternFagsak =
        EksternFagsak(
            eksternId = "TEST-101010",
            ytelsestype = Ytelsestype.BARNETRYGD,
            fagsystem = Fagsystem.BA,
            behandlinger = EksternFagsakBehandlingHistorikk(mutableListOf()),
            behovObservatør = behovObservatør,
        )
    private val eksempelsaker =
        listOf(
            Tilbakekreving(
                eksternFagsak,
                opprettet = LocalDateTime.of(2025, Month.MARCH, 15, 12, 0),
                behandlingHistorikk =
                    BehandlingHistorikk(mutableListOf()),
                bruker =
                    Bruker(
                        ident = fnr,
                        språkkode = Språkkode.NB,
                        fødselsdato = LocalDate.of(1969, Month.APRIL, 20),
                    ),
                behovObservatør = behovObservatør,
                kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf()),
            ).apply {
                håndter(
                    OpprettTilbakekrevingEvent(
                        EksternFagsakDto(
                            fagsystem = Fagsystem.BA,
                            ytelsestype = Ytelsestype.BARNETRYGD,
                            eksternId = "TEST-101010",
                        ),
                        opprettelsesvalg = Opprettelsevalg.OPPRETT_BEHANDLING_MED_VARSEL,
                    ),
                )
                håndter(
                    KravgrunnlagHendelse(
                        internId = UUID.randomUUID(),
                        vedtakId = BigInteger(128, Random()),
                        kravstatuskode = KravgrunnlagHendelse.Kravstatuskode.NYTT,
                        fagsystemVedtaksdato = LocalDate.now(),
                        vedtakGjelder = KravgrunnlagHendelse.Aktør.Person(fnr),
                        utbetalesTil = KravgrunnlagHendelse.Aktør.Person(fnr),
                        skalBeregneRenter = false,
                        ansvarligEnhet = "0425",
                        kontrollfelt = UUID.randomUUID().toString(),
                        referanse = UUID.randomUUID().toString(),
                        kravgrunnlagId = UUID.randomUUID().toString(),
                        perioder =
                            listOf(
                                KravgrunnlagHendelse.Periode(
                                    periode =
                                        Datoperiode(
                                            fom = LocalDate.of(2018, 1, 1),
                                            tom = LocalDate.of(2018, 2, 28),
                                        ),
                                    månedligSkattebeløp = BigDecimal("0.0"),
                                    beløp =
                                        listOf(
                                            KravgrunnlagHendelse.Periode.Beløp(
                                                klassekode = "",
                                                klassetype = "",
                                                opprinneligUtbetalingsbeløp = BigDecimal("12000.0"),
                                                nyttBeløp = BigDecimal("10000.0"),
                                                tilbakekrevesBeløp = BigDecimal("2000.0"),
                                                skatteprosent = BigDecimal("0.0"),
                                            ),
                                        ),
                                ),
                            ),
                    ),
                )
                håndter(
                    FagsysteminfoHendelse(
                        eksternId = UUID.randomUUID().toString(),
                    ),
                )
                håndter(VarselbrevSendtHendelse)
            },
        )

    fun hentTilbakekreving(
        fagsystem: Fagsystem,
        eksternFagsakId: String,
    ): Tilbakekreving? {
        if (!applicationProperties.toggles.nyModellEnabled) return null

        return eksempelsaker.firstOrNull { it.tilFrontendDto().fagsystem == fagsystem && it.tilFrontendDto().eksternFagsakId == eksternFagsakId }
    }

    fun hentTilbakekreving(behandlingId: UUID): Tilbakekreving? {
        if (!applicationProperties.toggles.nyModellEnabled) return null

        return eksempelsaker.firstOrNull { sak -> sak.tilFrontendDto().behandlinger.any { it.eksternBrukId == behandlingId } }
    }

    fun utførSteg(
        tilbakekreving: Tilbakekreving,
        behandlingsstegDto: BehandlingsstegDto,
    ) {
        val logContext = SecureLog.Context.fra(tilbakekreving)
        return when (behandlingsstegDto) {
            is BehandlingsstegForeldelseDto -> {
                behandlingsstegDto.foreldetPerioder.forEach { periode ->
                    val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
                    behandling.foreldelsesteg.vurderForeldelse(
                        periode.periode,
                        when (periode.foreldelsesvurderingstype) {
                            Foreldelsesvurderingstype.IKKE_VURDERT -> Foreldelsesteg.Vurdering.IkkeVurdert
                            Foreldelsesvurderingstype.FORELDET -> Foreldelsesteg.Vurdering.Foreldet(periode.begrunnelse, periode.oppdagelsesdato!!)
                            Foreldelsesvurderingstype.IKKE_FORELDET -> Foreldelsesteg.Vurdering.IkkeForeldet(periode.begrunnelse)
                            Foreldelsesvurderingstype.TILLEGGSFRIST -> Foreldelsesteg.Vurdering.Tilleggsfrist(periode.foreldelsesfrist!!, periode.oppdagelsesdato!!)
                        },
                    )
                }
            }
            else -> throw Feil("Vurdering for ${behandlingsstegDto.getSteg()} er ikke implementert i ny modell enda.", logContext = logContext)
        }
    }
}
