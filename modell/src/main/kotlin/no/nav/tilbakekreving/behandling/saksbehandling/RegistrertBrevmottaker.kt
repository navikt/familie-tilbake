package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerResponsDto
import no.nav.tilbakekreving.kontrakter.brev.Brevmottaker
import no.nav.tilbakekreving.kontrakter.brev.ManuellAdresseInfo
import no.nav.tilbakekreving.kontrakter.brev.MottakerType
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.BRUKER
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.BRUKER_OG_FULLMEKTIG
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.BRUKER_OG_VERGE
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.DØDSBO
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.FULLMEKTIG
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.UTENLANDSK_ADRESSE_OG_FULLMEKTIG
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.UTENLANDSK_ADRESSE_OG_VERGE
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.VERGE
import no.nav.tilbakekreving.kontrakter.verge.Vergetype
import java.util.UUID

sealed interface RegistrertBrevmottaker : FrontendDto<List<ManuellBrevmottakerResponsDto>> {
    val id: UUID
    val type: MottakerType

    class DefaultMottaker(
        override val id: UUID = UUID.randomUUID(),
        override val type: MottakerType = BRUKER,
        val navn: String,
        val personIdent: String? = null,
    ) : RegistrertBrevmottaker {
        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return listOf(
                ManuellBrevmottakerResponsDto(
                    id = id,
                    brevmottaker = Brevmottaker(
                        type = type,
                        vergetype = null,
                        navn = navn,
                        personIdent = personIdent,
                        manuellAdresseInfo = null,
                    ),
                ),
            )
        }
    }

    class UtenlandskAdresseMottaker(
        override val id: UUID,
        override val type: MottakerType = BRUKER_MED_UTENLANDSK_ADRESSE,
        val navn: String,
        val manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) : RegistrertBrevmottaker {
        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return listOf(
                ManuellBrevmottakerResponsDto(
                    id = id,
                    brevmottaker = Brevmottaker(
                        type = type,
                        vergetype = null,
                        navn = navn,
                        personIdent = null,
                        manuellAdresseInfo = manuellAdresseInfo,
                    ),
                ),
            )
        }
    }

    class FullmektigMottaker(
        override val id: UUID,
        override val type: MottakerType = FULLMEKTIG,
        val navn: String,
        val organisasjonsnummer: String? = null,
        val personIdent: String? = null,
        val manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) : RegistrertBrevmottaker {
        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return listOf(
                ManuellBrevmottakerResponsDto(
                    id = id,
                    brevmottaker = Brevmottaker(
                        type = type,
                        vergetype = null,
                        navn = navn,
                        organisasjonsnummer = organisasjonsnummer,
                        personIdent = personIdent,
                        manuellAdresseInfo = manuellAdresseInfo,
                    ),
                ),
            )
        }
    }

    class VergeMottaker(
        override val id: UUID,
        override val type: MottakerType = VERGE,
        val navn: String,
        val vergetype: Vergetype? = null,
        val personIdent: String? = null,
        val manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) : RegistrertBrevmottaker {
        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return listOf(
                ManuellBrevmottakerResponsDto(
                    id = id,
                    brevmottaker = Brevmottaker(
                        type = type,
                        vergetype = vergetype,
                        navn = navn,
                        organisasjonsnummer = null,
                        personIdent = personIdent,
                        manuellAdresseInfo = manuellAdresseInfo,
                    ),
                ),
            )
        }
    }

    class DødsboMottaker(
        override val id: UUID,
        override val type: MottakerType = DØDSBO,
        val navn: String,
        val manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) : RegistrertBrevmottaker {
        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return listOf(
                ManuellBrevmottakerResponsDto(
                    id = id,
                    brevmottaker = Brevmottaker(
                        type = type,
                        vergetype = null,
                        navn = navn,
                        organisasjonsnummer = null,
                        personIdent = null,
                        manuellAdresseInfo = manuellAdresseInfo,
                    ),
                ),
            )
        }
    }

    class UtenlandskAdresseOgVergeMottaker(
        override val id: UUID,
        override val type: MottakerType = UTENLANDSK_ADRESSE_OG_VERGE,
        val utenlandskAdresse: UtenlandskAdresseMottaker,
        val verge: VergeMottaker,
    ) : RegistrertBrevmottaker {
        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return utenlandskAdresse.tilFrontendDto() + verge.tilFrontendDto()
        }
    }

    class UtenlandskAdresseOgFullmektigMottaker(
        override val id: UUID,
        override val type: MottakerType = UTENLANDSK_ADRESSE_OG_FULLMEKTIG,
        val utenlandskAdresse: UtenlandskAdresseMottaker,
        val fullmektig: FullmektigMottaker,
    ) : RegistrertBrevmottaker {
        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return utenlandskAdresse.tilFrontendDto() + fullmektig.tilFrontendDto()
        }
    }

    class DefaultBrukerAdresseOgVergeMottaker(
        override val id: UUID,
        override val type: MottakerType = BRUKER_OG_VERGE,
        val defaultMottaker: DefaultMottaker,
        val verge: VergeMottaker,
    ) : RegistrertBrevmottaker {
        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return defaultMottaker.tilFrontendDto() + verge.tilFrontendDto()
        }
    }

    class DefaultBrukerAdresseOgFullmektigMottaker(
        override val id: UUID,
        override val type: MottakerType = BRUKER_OG_FULLMEKTIG,
        val defaultMottaker: DefaultMottaker,
        val fullmektig: FullmektigMottaker,
    ) : RegistrertBrevmottaker {
        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return defaultMottaker.tilFrontendDto() + fullmektig.tilFrontendDto()
        }
    }
}
