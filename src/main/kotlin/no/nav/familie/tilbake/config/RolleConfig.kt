package no.nav.familie.tilbake.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class RolleConfig(
        @Value("\${rolle.barnetrygd.beslutter}")
        val BESLUTTER_ROLLE_BARNETRYGD: String,

        @Value("\${rolle.barnetrygd.saksbehandler}")
        val SAKSBEHANDLER_ROLLE_BARNETRYGD: String,

        @Value("\${rolle.barnetrygd.veileder}")
        val VEILEDER_ROLLE_BARNETRYGD: String,

        @Value("\${rolle.enslig.beslutter}")
        val BESLUTTER_ROLLE_ENSLIG: String,

        @Value("\${rolle.enslig.saksbehandler}")
        val SAKSBEHANDLER_ROLLE_ENSLIG: String,

        @Value("\${rolle.enslig.veileder}")
        val VEILEDER_ROLLE_ENSLIG: String,

        @Value("\${rolle.kontantstøtte.beslutter}")
        val BESLUTTER_ROLLE_KONTANTSTØTTE: String,

        @Value("\${rolle.kontantstøtte.saksbehandler}")
        val SAKSBEHANDLER_ROLLE_KONTANTSTØTTE: String,

        @Value("\${rolle.kontantstøtte.veileder}")
        val VEILEDER_ROLLE_KONTANTSTØTTE: String,

        @Value("\${ENVIRONMENT_NAME}")
        val ENVIRONMENT_NAME: String
)
