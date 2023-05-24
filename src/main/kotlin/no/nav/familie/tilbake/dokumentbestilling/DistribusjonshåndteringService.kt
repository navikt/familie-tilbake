package no.nav.familie.tilbake.dokumentbestilling

import no.nav.familie.kontrakter.felles.dokdist.AdresseType
import no.nav.familie.kontrakter.felles.dokdist.ManuellAdresse
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType.DØDSBO
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.FeatureToggleConfig
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmetadataUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevgunnlagService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DistribusjonshåndteringService(
    private val brevmetadataUtil: BrevmetadataUtil,
    private val fagsakRepository: FagsakRepository,
    private val manuelleBrevmottakerRepository: ManuellBrevmottakerRepository,
    private val pdfBrevService: PdfBrevService,
    private val vedtaksbrevgrunnlagService: VedtaksbrevgunnlagService,
    private val featureToggleService: FeatureToggleService
) {

    fun sendBrev(
        behandling: Behandling,
        brevtype: Brevtype,
        varsletBeløp: Long? = null,
        fritekst: String? = null,
        brevdata: (Brevmottager, Brevmetadata?) -> Brevdata
    ) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val vedtaksbrevgrunnlag = when (brevtype) {
            Brevtype.VEDTAK -> vedtaksbrevgrunnlagService.hentVedtaksbrevgrunnlag(behandling.id)
            else -> null
        }
        val støtterManuelleBrevmottakere: Boolean = BehandlingService.sjekkOmManuelleBrevmottakereErStøttet(
            behandling = behandling,
            fagsak = fagsak,
            featureToggleEnabled = featureToggleService.isEnabled(FeatureToggleConfig.DISTRIBUER_TIL_MANUELLE_BREVMOTTAKERE)
        )
        val brevmottakere = utledMottakere(
            behandling = behandling,
            fagsak = fagsak,
            erManuelleMottakereStøttet = støtterManuelleBrevmottakere,
            manueltRegistrerteMottakere = manuelleBrevmottakerRepository.findByBehandlingId(behandling.id).toSet()
        ).toList()

        brevmottakere.forEachIndexed { index, brevmottaker ->
            pdfBrevService.sendBrev(
                behandling = behandling,
                fagsak = fagsak,
                brevtype = brevtype,
                data = brevdata(
                    brevmottaker.somBrevmottager,
                    brevmetadataUtil.genererMetadataForBrev(
                        behandling.id,
                        vedtaksbrevgrunnlag,
                        brevmottager = brevmottaker.somBrevmottager,
                        manuellAdresseinfo = brevmottaker.manuellAdresse,
                        annenMottakersNavn = brevmottakere[brevmottakere.lastIndex - index].navn
                    )
                ),
                varsletBeløp = varsletBeløp,
                fritekst = fritekst
            )
        }
    }
    companion object {
        fun utledMottakere(
            behandling: Behandling,
            fagsak: Fagsak,
            erManuelleMottakereStøttet: Boolean,
            manueltRegistrerteMottakere: Set<ManuellBrevmottaker>
        ): Pair<Any, Any?> {
            return if (erManuelleMottakereStøttet) {
                require(manueltRegistrerteMottakere.all { it.behandlingId == behandling.id })

                val (manuellBrukeradresse, manuellTilleggsmottaker) = manueltRegistrerteMottakere
                    .partition { it.type == BRUKER_MED_UTENLANDSK_ADRESSE || it.type == DØDSBO }
                Pair(manuellBrukeradresse.singleOrNull() ?: Brevmottager.BRUKER, manuellTilleggsmottaker.singleOrNull())
            } else {
                val defaultMottaker = if (fagsak.institusjon != null) Brevmottager.INSTITUSJON else Brevmottager.BRUKER
                val tilleggsmottaker = if (behandling.harVerge) Brevmottager.VERGE else null
                Pair(defaultMottaker, tilleggsmottaker)
            }
        }
    }
}

val Any?.navn: String?
    get() = if (this is ManuellBrevmottaker) navn else null
val Any?.somBrevmottager: Brevmottager
    get() = this as? Brevmottager ?: Brevmottager.MANUELL
val Any?.manuellAdresse: Adresseinfo?
    get() = if (this is ManuellBrevmottaker)
        Adresseinfo(
            ident = ident.orEmpty(),
            mottagernavn = navn,
            manuellAdresse = if (hasManuellAdresse())
                ManuellAdresse(
                    adresseType = when (landkode) {
                        "NO" -> AdresseType.norskPostadresse
                        else -> AdresseType.utenlandskPostadresse
                    },
                    adresselinje1 = adresselinje1,
                    adresselinje2 = adresselinje2,
                    postnummer = postnummer,
                    poststed = poststed,
                    land = landkode!!,
                ) else null,
        ) else null

