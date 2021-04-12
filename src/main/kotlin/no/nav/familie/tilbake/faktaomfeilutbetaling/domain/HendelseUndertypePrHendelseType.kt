package no.nav.familie.tilbake.faktaomfeilutbetaling.domain

object HendelsesundertypePerHendelsestype {

    private val HIERARKI = mapOf(Hendelsestype.BA_ANNET to setOf(Hendelsesundertype.ANNET_FRITEKST),
                                 Hendelsestype.EF_ANNET to setOf(Hendelsesundertype.ANNET_FRITEKST),
                                 Hendelsestype.KS_ANNET to setOf(Hendelsesundertype.ANNET_FRITEKST),
                                 Hendelsestype.ENDRING_STØNADSPERIODEN to setOf(Hendelsesundertype.MOTTAKER_DØD,
                                                                                Hendelsesundertype.BARN_DØD,
                                                                                Hendelsesundertype.IKKE_OMSORG),
                                 Hendelsestype.ØKONOMIFEIL to setOf(Hendelsesundertype.DOBBELUTBETALING,
                                                                    Hendelsesundertype.FOR_MYE_UTBETALT,
                                                                    Hendelsesundertype.ØKONOMI_FEIL_TREKK,
                                                                    Hendelsesundertype.ØKONOMI_FEIL_FERIEPENGER),
                                 Hendelsestype.MEDLEMSKAP to setOf(Hendelsesundertype.IKKE_BOSATT,
                                                                   Hendelsesundertype.MEDLEM_I_ANNET_LAND,
                                                                   Hendelsesundertype.IKKE_LOVLIG_OPPHOLD,
                                                                   Hendelsesundertype.UTVANDRET))

    fun getHendelsesundertyper(hendelsestype: Hendelsestype): Set<Hendelsesundertype> {
        return HIERARKI[hendelsestype] ?: error("Ikke-støttet hendelseType: $hendelsestype")
    }
}
