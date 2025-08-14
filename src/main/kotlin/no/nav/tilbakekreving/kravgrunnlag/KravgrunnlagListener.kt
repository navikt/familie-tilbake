package no.nav.tilbakekreving.kravgrunnlag

import jakarta.jms.Message
import jakarta.jms.MessageListener
import jakarta.jms.TextMessage
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import org.springframework.stereotype.Service

@Service
class KravgrunnlagListener(private val kravgrunnlagBufferRepository: KravgrunnlagBufferRepository) : MessageListener {
    override fun onMessage(message: Message) {
        require(message is TextMessage) { "Mottok melding som ikke er TextMessage" }
        val kravgrunnlagXML = message.text
        val kravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagXML)
        kravgrunnlagBufferRepository.lagre(
            KravgrunnlagBufferRepository.Entity(
                kravgrunnlag = kravgrunnlagXML,
                kravgrunnlagId = kravgrunnlag.kravgrunnlagId.toString(),
                fagsystemId = kravgrunnlag.fagsystemId,
            ),
        )
    }
}
