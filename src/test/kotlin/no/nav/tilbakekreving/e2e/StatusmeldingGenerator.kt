package no.nav.tilbakekreving.e2e

import org.intellij.lang.annotations.Language

object StatusmeldingGenerator {
    fun forFagområdet(
        vedtakId: String = KravgrunnlagGenerator.nextPaddedId(6),
        fagsystemId: String = KravgrunnlagGenerator.nextPaddedId(6),
        referanse: String = KravgrunnlagGenerator.nextPaddedId(4),
        fødselsnummer: String = "40026912345",
        kodeStatusKrav: String,
        fagområde: String,
    ): String {
        @Language("XML")
        val xml = """<?xml version="1.0" encoding="utf-8"?>
        <urn:endringKravOgVedtakstatus xmlns:urn="urn:no:nav:tilbakekreving:status:v1">
            <urn:kravOgVedtakstatus>
                <urn:vedtakId>$vedtakId</urn:vedtakId>
                <urn:kodeStatusKrav>$kodeStatusKrav</urn:kodeStatusKrav>
                <urn:kodeFagomraade>$fagområde</urn:kodeFagomraade>
                <urn:fagsystemId>$fagsystemId</urn:fagsystemId>
                <urn:vedtakGjelderId>$fødselsnummer</urn:vedtakGjelderId>
                <urn:typeGjelderId>PERSON</urn:typeGjelderId>
                <urn:referanse>$referanse</urn:referanse>
            </urn:kravOgVedtakstatus>
        </urn:endringKravOgVedtakstatus>
            """
        return xml.replace(Regex("\n *"), "")
    }
}
