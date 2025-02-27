package no.nav.familie.tilbake.leader

object Environment {
    @JvmStatic
    fun hentLeaderSystemEnv(): String? {
        return System.getenv("ELECTOR_GET_URL") ?: return null
    }
}
