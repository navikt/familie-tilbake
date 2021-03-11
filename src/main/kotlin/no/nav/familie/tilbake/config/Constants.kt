package no.nav.familie.tilbake.config

import java.time.Period

object Constants {

    val brukersSvarfrist: Period = Period.ofWeeks(3)

    val kravgrunnlagXmlRootElement: String = "urn:detaljertKravgrunnlagMelding"
    val statusmeldingXmlRootElement: String = "urn:endringKravOgVedtakstatus"
}
