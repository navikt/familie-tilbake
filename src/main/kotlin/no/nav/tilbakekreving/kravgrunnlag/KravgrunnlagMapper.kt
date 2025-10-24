package no.nav.tilbakekreving.kravgrunnlag

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagBelopDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.typer.v1.JaNeiDto
import no.nav.tilbakekreving.typer.v1.TypeGjelderDto
import org.springframework.http.HttpStatus
import java.util.UUID

object KravgrunnlagMapper {
    fun tilOpprettTilbakekrevingHendelse(kravgrunnlag: DetaljertKravgrunnlagDto): OpprettTilbakekrevingHendelse {
        return OpprettTilbakekrevingHendelse(
            eksternFagsak = OpprettTilbakekrevingHendelse.EksternFagsak(
                eksternId = kravgrunnlag.fagsystemId,
                ytelse = ytelseFor(kravgrunnlag),
            ),
            opprettelsesvalg = Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
        )
    }

    fun tilKravgrunnlagHendelse(kravgrunnlag: DetaljertKravgrunnlagDto): KravgrunnlagHendelse {
        val kravgrunnlagHendelse = KravgrunnlagHendelse(
            id = UUID.randomUUID(),
            vedtakId = kravgrunnlag.vedtakId,
            kravstatuskode = KravgrunnlagHendelse.Kravstatuskode.forOppdragKode(kravgrunnlag.kodeStatusKrav),
            fagsystemVedtaksdato = kravgrunnlag.datoVedtakFagsystem,
            vedtakGjelder = mapAktør(kravgrunnlag.typeGjelderId, kravgrunnlag.vedtakGjelderId),
            utbetalesTil = mapAktør(kravgrunnlag.typeUtbetId, kravgrunnlag.utbetalesTilId),
            skalBeregneRenter = kravgrunnlag.renterBeregnes == JaNeiDto.J,
            ansvarligEnhet = kravgrunnlag.enhetAnsvarlig,
            kontrollfelt = kravgrunnlag.kontrollfelt,
            kravgrunnlagId = kravgrunnlag.kravgrunnlagId.toString(),
            referanse = kravgrunnlag.referanse,
            perioder = kravgrunnlag.tilbakekrevingsPeriode.map { periode ->
                KravgrunnlagHendelse.Periode(
                    id = UUID.randomUUID(),
                    periode = periode.periode.fom til periode.periode.tom,
                    månedligSkattebeløp = periode.belopSkattMnd,
                    beløp = periode.tilbakekrevingsBelop.tilBeløp(),
                )
            },
        )
        kravgrunnlagHendelse.valider(Sporing(kravgrunnlag.fagsystemId, "Ukjent"))
        return kravgrunnlagHendelse
    }

    private fun mapAktør(
        typeGjelder: TypeGjelderDto,
        gjelderId: String,
    ) = when (typeGjelder) {
        TypeGjelderDto.PERSON -> Aktør.Person(gjelderId)
        TypeGjelderDto.ORGANISASJON -> Aktør.Organisasjon(gjelderId)
        TypeGjelderDto.SAMHANDLER -> Aktør.Samhandler(gjelderId)
        TypeGjelderDto.APPBRUKER -> Aktør.Applikasjonsbruker(gjelderId)
    }

    private fun Iterable<DetaljertKravgrunnlagBelopDto>.tilBeløp() = map {
        KravgrunnlagHendelse.Periode.Beløp(
            id = UUID.randomUUID(),
            it.kodeKlasse,
            it.typeKlasse.name,
            it.belopOpprUtbet,
            it.belopNy,
            it.belopTilbakekreves,
            it.skattProsent,
        )
    }

    fun ytelseFor(kravgrunnlag: DetaljertKravgrunnlagDto) = when (kravgrunnlag.kodeFagomraade) {
        "TILLST" -> Ytelse.Tilleggsstønad
        "AAP" -> Ytelse.Arbeidsavklaringspenger
        else -> throw Feil(
            message = "Kan ikke håndtere saker for ${kravgrunnlag.kodeFagomraade} med ny modell",
            httpStatus = HttpStatus.BAD_REQUEST,
            logContext = SecureLog.Context.utenBehandling(kravgrunnlag.fagsystemId),
        )
    }
}
