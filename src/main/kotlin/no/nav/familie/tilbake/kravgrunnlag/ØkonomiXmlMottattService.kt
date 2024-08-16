package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottattArkiv
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID

@Service
class ØkonomiXmlMottattService(
    private val mottattXmlRepository: ØkonomiXmlMottattRepository,
    private val mottattXmlArkivRepository: ØkonomiXmlMottattArkivRepository,
) {
    fun lagreMottattXml(
        kravgrunnlagXml: String,
        kravgrunnlag: DetaljertKravgrunnlagDto,
        ytelsestype: Ytelsestype,
    ): ØkonomiXmlMottatt =
        mottattXmlRepository.insert(
            ØkonomiXmlMottatt(
                melding = kravgrunnlagXml,
                kravstatuskode = Kravstatuskode.fraKode(kravgrunnlag.kodeStatusKrav),
                eksternFagsakId = kravgrunnlag.fagsystemId,
                ytelsestype = ytelsestype,
                referanse = kravgrunnlag.referanse,
                eksternKravgrunnlagId = kravgrunnlag.kravgrunnlagId,
                vedtakId = kravgrunnlag.vedtakId,
                kontrollfelt = kravgrunnlag.kontrollfelt,
            ),
        )

    fun hentMottattKravgrunnlag(
        eksternKravgrunnlagId: BigInteger,
        vedtakId: BigInteger,
    ): List<ØkonomiXmlMottatt> = mottattXmlRepository.findByEksternKravgrunnlagIdAndVedtakId(eksternKravgrunnlagId, vedtakId)

    fun arkiverEksisterendeGrunnlag(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val eksisterendeKravgrunnlag: List<ØkonomiXmlMottatt> =
            hentMottattKravgrunnlag(
                eksternKravgrunnlagId = kravgrunnlag.kravgrunnlagId,
                vedtakId = kravgrunnlag.vedtakId,
            )
        eksisterendeKravgrunnlag.forEach {
            arkiverMottattXml(
                mottattXmlId = it.id,
                mottattXml = it.melding,
                fagsystemId = it.eksternFagsakId,
                ytelsestype = it.ytelsestype,
            )
        }
        eksisterendeKravgrunnlag.forEach { slettMottattXml(it.id) }
    }

    fun hentMottattKravgrunnlag(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
        vedtakId: BigInteger,
    ): List<ØkonomiXmlMottatt> {
        val mottattXmlListe =
            mottattXmlRepository
                .findByEksternFagsakIdAndYtelsestypeAndVedtakId(eksternFagsakId, ytelsestype, vedtakId)
        val kravgrunnlagXmlListe = mottattXmlListe.filter { it.melding.contains(Constants.KRAVGRUNNLAG_XML_ROOT_ELEMENT) }
        if (kravgrunnlagXmlListe.isEmpty()) {
            val arkivertMottattXmlListe = mottattXmlArkivRepository.findByEksternFagsakIdAndYtelsestype(eksternFagsakId, ytelsestype)
            if (arkivertMottattXmlListe.isNotEmpty()) {
                return emptyList()
            } else {
                throw Feil(
                    message = "Det finnes ikke noe kravgrunnlag for fagsystemId=$eksternFagsakId og ytelsestype=$ytelsestype",
                )
            }
        }
        return kravgrunnlagXmlListe
    }

    fun hentFrakobletKravgrunnlag(
        barnetrygdBestemtDato: LocalDate,
        barnetilsynBestemtDato: LocalDate,
        overgangsstønadBestemtDato: LocalDate,
        skolePengerBestemtDato: LocalDate,
        kontantStøtteBestemtDato: LocalDate,
    ): List<ØkonomiXmlMottatt> =
        mottattXmlRepository.hentFrakobletKravgrunnlag(
            barnetrygdBestemtDato = barnetrygdBestemtDato,
            barnetilsynBestemtDato = barnetilsynBestemtDato,
            overgangsstonadbestemtdato = overgangsstønadBestemtDato,
            skolePengerBestemtDato = skolePengerBestemtDato,
            kontantstottebestemtdato = kontantStøtteBestemtDato,
        )

    fun hentMottattKravgrunnlag(mottattXmlId: UUID): ØkonomiXmlMottatt = mottattXmlRepository.findByIdOrThrow(mottattXmlId)

    fun hentMottattKravgrunnlagNullable(mottattXmlId: UUID): ØkonomiXmlMottatt? = mottattXmlRepository.findByIdOrNull(mottattXmlId)

    fun oppdaterMottattXml(mottattXml: ØkonomiXmlMottatt) {
        mottattXmlRepository.update(mottattXml)
    }

    fun slettMottattXml(mottattXmlId: UUID) {
        mottattXmlRepository.deleteById(mottattXmlId)
    }

    fun arkiverMottattXml(
        mottattXmlId: UUID?,
        mottattXml: String,
        fagsystemId: String,
        ytelsestype: Ytelsestype,
    ) {
        mottattXmlArkivRepository.insert(
            ØkonomiXmlMottattArkiv(
                gammel_okonomi_xml_mottatt_id = mottattXmlId,
                melding = mottattXml,
                eksternFagsakId = fagsystemId,
                ytelsestype = ytelsestype,
            ),
        )
    }

    fun hentArkiverteMottattXml(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
    ): List<ØkonomiXmlMottattArkiv> = mottattXmlArkivRepository.findByEksternFagsakIdAndYtelsestype(eksternFagsakId, ytelsestype)
}
