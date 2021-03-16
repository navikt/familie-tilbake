package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.common.exceptionhandler.UgyldigKravgrunnlagFeil
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagMelding
import no.nav.tilbakekreving.status.v1.EndringKravOgVedtakstatus
import no.nav.tilbakekreving.status.v1.KravOgVedtakstatus
import java.io.StringReader
import java.math.BigDecimal
import java.util.Comparator
import java.util.SortedMap
import javax.xml.XMLConstants
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.Unmarshaller
import javax.xml.validation.SchemaFactory

object KravgrunnlagUtil {

    private val jaxbContext: JAXBContext = JAXBContext.newInstance(DetaljertKravgrunnlagMelding::class.java)
    private val statusmeldingJaxbContext: JAXBContext = JAXBContext.newInstance(EndringKravOgVedtakstatus::class.java)

    fun finnFeilutbetalingPrPeriode(kravgrunnlag: Kravgrunnlag431): SortedMap<Periode, BigDecimal> {
        val feilutbetalingPrPeriode = mutableMapOf<Periode, BigDecimal>()
        for (kravgrunnlagPeriode432 in kravgrunnlag.perioder) {
            val feilutbetaltBeløp = kravgrunnlagPeriode432.beløp
                    .filter { Klassetype.FEIL === it.klassetype }
                    .sumOf(Kravgrunnlagsbeløp433::nyttBeløp)
            if (feilutbetaltBeløp.compareTo(BigDecimal.ZERO) != 0) {
                feilutbetalingPrPeriode[kravgrunnlagPeriode432.periode] = feilutbetaltBeløp
            }
        }
        return feilutbetalingPrPeriode.toSortedMap(Comparator.comparing(Periode::fom).thenComparing(Periode::tom))
    }

    fun unmarshalKravgrunnlag(kravgrunnlagXML: String): DetaljertKravgrunnlagDto {
        return try {
            val jaxbUnmarshaller: Unmarshaller = jaxbContext.createUnmarshaller()

            //satt xsd for å validere mottatt xml
            val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
            val kravgrunnlagSchema = schemaFactory.newSchema(
                    this.javaClass.classLoader.getResource("xsd/kravgrunnlag_detalj.xsd"))
            jaxbUnmarshaller.schema = kravgrunnlagSchema

            (jaxbUnmarshaller.unmarshal(StringReader(kravgrunnlagXML)) as DetaljertKravgrunnlagMelding).detaljertKravgrunnlag
        } catch (e: JAXBException) {
            throw UgyldigKravgrunnlagFeil(melding = "Mottatt kravgrunnlagXML er ugyldig! Den feiler med $e")
        }
    }

    fun unmarshalStatusmelding(statusmeldingXml: String): KravOgVedtakstatus {
        return try {
            val jaxbUnmarshaller: Unmarshaller = statusmeldingJaxbContext.createUnmarshaller()

            //satt xsd for å validere mottatt xml
            val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
            val statusmeldingSchema = schemaFactory.newSchema(
                    this.javaClass.classLoader.getResource("xsd/krav_og_vedtakstatus.xsd"))
            jaxbUnmarshaller.schema = statusmeldingSchema

            (jaxbUnmarshaller.unmarshal(StringReader(statusmeldingXml)) as EndringKravOgVedtakstatus).kravOgVedtakstatus
        } catch (e: JAXBException) {
            throw UgyldigKravgrunnlagFeil(melding = "Mottatt statusmeldingXML er ugyldig! Den feiler med $e")
        }
    }

    fun tilYtelsestype(fagområdekode: String): Ytelsestype {
        return when (fagområdekode) {
            "BA" -> Ytelsestype.BARNETRYGD
            "KS" -> Ytelsestype.KONTANTSTØTTE
            "EFOG" -> Ytelsestype.OVERGANGSSTØNAD
            "EFBT" -> Ytelsestype.BARNETILSYN
            "EFSP" -> Ytelsestype.SKOLEPENGER
            else -> throw IllegalArgumentException("Ukjent Ytelsestype for $fagområdekode")
        }
    }
}
