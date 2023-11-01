package no.nav.familie.tilbake.config

import java.io.BufferedReader
import java.io.InputStreamReader

class TestLauncherConfig {
    fun settClientIdOgSecretForLokalKjøring() {
        val cmd = "src/test/resources/hentMiljøvariabler-lokal.sh"

        hentOgSettMiljøVariabler(cmd)
    }

    fun settClientIdOgSecretForLokalKjøringMotPreprod() {
        val cmd = "src/test/resources/hentMiljøvariabler-preprod.sh"

        hentOgSettMiljøVariabler(cmd)
    }

    private fun hentOgSettMiljøVariabler(cmd: String) {
        val process = ProcessBuilder(cmd).start()

        if (process.waitFor() == 1) {
            error("Klarte ikke hente variabler fra Nais. Er du logget på Naisdevice og gcloud?")
        }

        val inputStream = BufferedReader(InputStreamReader(process.inputStream))
        inputStream.readLine() // "Switched to context dev-gcp"
        val split = inputStream.readLine().split(";")
        split
            .map { it.split("=") }
            .map { System.setProperty(it[0], it[1]) }
        inputStream.close()
    }
}
