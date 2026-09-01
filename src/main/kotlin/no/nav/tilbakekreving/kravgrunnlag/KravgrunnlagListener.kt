package no.nav.tilbakekreving.kravgrunnlag

import jakarta.jms.Message
import jakarta.jms.MessageListener
import jakarta.jms.TextMessage
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.stereotype.Service

@Service
class KravgrunnlagListener(
    private val kravgrunnlagBufferRepository: KravgrunnlagBufferRepository,
    private val statusmeldingBufferRepository: StatusmeldingBufferRepository,
) : MessageListener {
    private val log = TracedLogger.getLogger<KravgrunnlagListener>()

    override fun onMessage(message: Message) {
        require(message is TextMessage) { "Mottok melding som ikke er TextMessage" }
        val meldingXML = message.text
        if (meldingXML.contains(Constants.STATUSMELDING_XML_ROOT_ELEMENT)) {
            val statusmelding = KravgrunnlagUtil.unmarshalStatusmelding(meldingXML)
            log.medContext(SecureLog.Context.utenBehandling(statusmelding.fagsystemId)) {
                info(
                    "Mottok statusmelding for {} med status {}",
                    keyValue("vedtakId", statusmelding.vedtakId),
                    statusmelding.kodeStatusKrav,
                )
            }
            statusmeldingBufferRepository.lagre(
                StatusmeldingBufferRepository.Entity(
                    statusmelding = meldingXML,
                    fagsystemId = statusmelding.fagsystemId,
                    vedtakId = statusmelding.vedtakId.toString(),
                    status = statusmelding.kodeStatusKrav,
                ),
            )
        } else {
            val kravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(meldingXML)
            kravgrunnlagBufferRepository.lagre(
                KravgrunnlagBufferRepository.Entity(
                    kravgrunnlag = meldingXML,
                    kravgrunnlagId = kravgrunnlag.kravgrunnlagId.toString(),
                    fagsystemId = kravgrunnlag.fagsystemId,
                ),
            )
        }
    }
}
