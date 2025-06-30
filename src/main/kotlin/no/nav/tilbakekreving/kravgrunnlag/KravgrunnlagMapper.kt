package no.nav.tilbakekreving.kravgrunnlag

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagBelopDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.typer.v1.JaNeiDto
import no.nav.tilbakekreving.typer.v1.TypeGjelderDto
import no.nav.tilbakekreving.typer.v1.TypeKlasseDto
import org.springframework.http.HttpStatus
import java.util.UUID

object KravgrunnlagMapper {
    fun tilOpprettTilbakekrevingHendelse(kravgrunnlag: DetaljertKravgrunnlagDto): OpprettTilbakekrevingHendelse {
        return OpprettTilbakekrevingHendelse(
            eksternFagsak = OpprettTilbakekrevingHendelse.EksternFagsak(
                eksternId = kravgrunnlag.fagsystemId,
                ytelse = ytelseFor(kravgrunnlag),
            ),
            opprettelsesvalg = Opprettelsesvalg.OPPRETT_BEHANDLING_MED_VARSEL,
        )
    }

    fun tilKravgrunnlagHendelse(kravgrunnlag: DetaljertKravgrunnlagDto): KravgrunnlagHendelse {
        return KravgrunnlagHendelse(
            UUID.randomUUID(),
            kravgrunnlag.vedtakId,
            KravgrunnlagHendelse.Kravstatuskode.valueOf(kravgrunnlag.kodeStatusKrav),
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
                    periode = periode.periode.fom til periode.periode.tom,
                    månedligSkattebeløp = periode.belopSkattMnd,
                    feilutbetaltBeløp = periode.tilbakekrevingsBelop.filter { it.typeKlasse == TypeKlasseDto.FEIL }.tilBeløp(),
                    ytelsesbeløp = periode.tilbakekrevingsBelop.filter { it.typeKlasse == TypeKlasseDto.YTEL }.tilBeløp(),
                )
            },
        )
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
            it.kodeKlasse,
            it.typeKlasse.name,
            it.belopOpprUtbet,
            it.belopNy,
            it.belopTilbakekreves,
            it.skattProsent,
        )
    }

    private fun ytelseFor(kravgrunnlag: DetaljertKravgrunnlagDto) = when (kravgrunnlag.kodeFagomraade) {
        "TILLST" -> Ytelse.Tilleggsstønad
        else -> throw Feil(
            message = "Kan ikke håndtere saker for ${kravgrunnlag.kodeFagomraade} med ny modell",
            httpStatus = HttpStatus.BAD_REQUEST,
            logContext = SecureLog.Context.utenBehandling(kravgrunnlag.fagsystemId),
        )
    }
}
