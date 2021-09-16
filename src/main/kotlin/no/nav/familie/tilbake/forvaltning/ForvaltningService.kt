package no.nav.familie.tilbake.forvaltning

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.kravgrunnlag.HentKravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.util.UUID

@Service
class ForvaltningService(private val behandlingRepository: BehandlingRepository,
                         private val kravgrunnlagRepository: KravgrunnlagRepository,
                         private val hentKravgrunnlagService: HentKravgrunnlagService,
                         private val økonomiXmlMottattService: ØkonomiXmlMottattService) {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun korrigerKravgrunnlag(behandlingId: UUID,
                             kravgrunnlagId: BigInteger) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erAvsluttet) {
            throw Feil("Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                       frontendFeilmelding = "Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
        val hentetKravgrunnlag = hentKravgrunnlagService.hentKravgrunnlagFraØkonomi(kravgrunnlagId,
                                                                                    KodeAksjon.HENT_KORRIGERT_KRAVGRUNNLAG)

        val kravgrunnlag = kravgrunnlagRepository.findByEksternKravgrunnlagIdAndAktivIsTrue(kravgrunnlagId)
        if (kravgrunnlag != null) {
            kravgrunnlagRepository.update(kravgrunnlag.copy(aktiv = false))
        }
        hentKravgrunnlagService.lagreHentetKravgrunnlag(behandlingId, hentetKravgrunnlag)
    }

    @Transactional
    fun arkiverMottattKravgrunnlag(mottattXmlId: UUID) {
        logger.info("Arkiverer mottattXml for Id=$mottattXmlId")
        val mottattKravgrunnlag = økonomiXmlMottattService.hentMottattKravgrunnlag(mottattXmlId)
        økonomiXmlMottattService.arkiverMottattXml(mottattKravgrunnlag.melding,
                                                   mottattKravgrunnlag.eksternFagsakId,
                                                   mottattKravgrunnlag.ytelsestype)
        økonomiXmlMottattService.slettMottattXml(mottattXmlId)
    }
}