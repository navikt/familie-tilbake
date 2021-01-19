package no.nav.familie.tilbake.data

import no.nav.familie.tilbake.domain.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

object Testdata {

    private val bruker = Bruker(ident = "321321321")

    val fagsak = Fagsak(ytelsestype = Ytelsestype.BA,
                        eksternFagsakId = "testverdi",
                        bruker = bruker)

    val behandling = Behandling(fagsakId = fagsak.id,
                                type = Behandlingstype.TILBAKEKREVING,
                                opprettetDato = LocalDate.now(),
                                avsluttetDato = LocalDate.now(),
                                ansvarligSaksbehandler = "testverdi",
                                ansvarligBeslutter = "testverdi",
                                behandlendeEnhet = "testverdi",
                                behandlendeEnhetsNavn = "testverdi",
                                manueltOpprettet = true,
                                eksternId = UUID.randomUUID())

    val behandlingsstegstype = Behandlingsstegstype(kode = "testverdi",
                                                    navn = "testverdi",
                                                    behandlingsstatusDefault = "testverdi",
                                                    beskrivelse = "testverdi")

    val behandlingsårsak = Behandlingsårsak(behandlingId = behandling.id,
                                            type = "testverdi",
                                            originalBehandlingId = null)

    val vurderingspunktsdefinisjon = Vurderingspunktsdefinisjon(kode = "testverdi",
                                                                behandlingsstegstypeId = behandlingsstegstype.id,
                                                                navn = "testverdi",
                                                                beskrivelse = "testverdi")

    val aksjonspunktsdefinisjon = Aksjonspunktsdefinisjon(kode = "testverdi",
                                                          navn = "testverdi",
                                                          vurderingspunktsdefinisjonId = vurderingspunktsdefinisjon.id,
                                                          beskrivelse = "testverdi",
                                                          vilkårstype = "testverdi",
                                                          totrinnsbehandlingDefault = true,
                                                          fristperiode = "testverdi",
                                                          skjermlenketype = "testverdi")

    val aksjonspunkt = Aksjonspunkt(totrinnsbehandling = true,
                                    behandlingsstegstypeId = behandlingsstegstype.id,
                                    aksjonspunktsdefinisjonId = aksjonspunktsdefinisjon.id,
                                    behandlingId = behandling.id,
                                    tidsfrist = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
                                    status = "testverdi")

    val revurderingsårsak = Revurderingsårsak(aksjonspunktId = aksjonspunkt.id,
                                              årsakstype = "testverdi")

    val behandlingsstegstilstand = Behandlingsstegstilstand(behandlingId = behandling.id,
                                                            behandlingsstegstypeId = behandlingsstegstype.id,
                                                            behandlingsstegsstatus = "testverdi")

    val behandlingsstegssekvens = Behandlingsstegssekvens(behandlingstype = "testverdi",
                                                          behandlingsstegstypeId = behandlingsstegstype.id,
                                                          sekvensnummer = 1)

    val behandlingsresultat = Behandlingsresultat(behandlingId = behandling.id)

    val behandlingsvedtak = Behandlingsvedtak(vedtaksdato = LocalDate.now(),
                                              ansvarligSaksbehandler = "testverdi",
                                              behandlingsresultatId = behandlingsresultat.id)

    val totrinnsvurdering = Totrinnsvurdering(behandlingId = behandling.id,
                                              aksjonspunktsdefinisjonId = aksjonspunktsdefinisjon.id,
                                              godkjent = true,
                                              begrunnelse = "testverdi")

    val årsakTotrinnsvurdering = ÅrsakTotrinnsvurdering(årsakstype = "testverdi",
                                                        totrinnsvurderingId = totrinnsvurdering.id)

    val mottakersVarselrespons = MottakersVarselrespons(behandlingId = behandling.id,
                                                        akseptertFaktagrunnlag = true,
                                                        kilde = "testverdi")

    val vurdertForeldelse = VurdertForeldelse(behandlingId = behandling.id)

    val grupperingVurdertForeldelse = GrupperingVurdertForeldelse(vurdertForeldelseId = vurdertForeldelse.id,
                                                                  behandlingId = behandling.id)

    val foreldelsesperiode = Foreldelsesperiode(vurdertForeldelseId = vurdertForeldelse.id,
                                                fom = LocalDate.now(),
                                                tom = LocalDate.now(),
                                                foreldelsesvurderingstype = "testverdi",
                                                begrunnelse = "testverdi",
                                                foreldelsesfrist = LocalDate.now(),
                                                oppdagelsesdato = LocalDate.now())

    val kravgrunnlag431 = Kravgrunnlag431(vedtakId = "testverdi",
                                          kravstatuskode = "testverdi",
                                          fagområdekode = "testverdi",
                                          fagsystem = "testverdi",
                                          fagsystemVedtaksdato = LocalDate.now(),
                                          omgjortVedtakId = "testverdi",
                                          gjelderVedtakId = "testverdi",
                                          gjelderType = "testverdi",
                                          utbetalesTilId = "testverdi",
                                          hjemmelkode = "testverdi",
                                          beregnesRenter = true,
                                          ansvarligEnhet = "testverdi",
                                          bostedsenhet = "testverdi",
                                          behandlingsenhet = "testverdi",
                                          kontrollfelt = "testverdi",
                                          saksbehandlerId = "testverdi",
                                          referanse = "testverdi",
                                          eksternKravgrunnlagId = "testverdi")

    val kravgrunnlagsperiode432 = Kravgrunnlagsperiode432(kravgrunnlag431Id = kravgrunnlag431.id,
                                                          fom = LocalDate.now(),
                                                          tom = LocalDate.now(),
                                                          månedligSkattebeløp = 123.11)

    val kravgrunnlagsbeløp433 = Kravgrunnlagsbeløp433(kravgrunnlagsperiode432Id = kravgrunnlagsperiode432.id,
                                                      klassekode = "testverdi",
                                                      klassetype = "testverdi",
                                                      opprinneligUtbetalingsbeløp = 123.11,
                                                      nyttBeløp = 123.11,
                                                      tilbakekrevesBeløp = 123.11,
                                                      uinnkrevdBeløp = 123.11,
                                                      resultatkode = "testverdi",
                                                      årsakskode = "testverdi",
                                                      skyldkode = "testverdi",
                                                      skatteprosent = 35.11)

    val kravvedtaksstatus437 = Kravvedtaksstatus437(vedtakId = "testverdi",
                                                    kravstatuskode = "testverdi",
                                                    fagområdekode = "testverdi",
                                                    fagsystemId = "testverdi",
                                                    gjelderVedtakId = "testverdi",
                                                    gjelderType = "testverdi",
                                                    referanse = "testverdi")

    val grupperingKravGrunnlag = GrupperingKravGrunnlag(kravgrunnlag431Id = kravgrunnlag431.id,
                                                        behandlingId = behandling.id,
                                                        aktiv = true,
                                                        sperret = true)

    val vilkår = Vilkår(behandlingId = behandling.id)

    val vilkårsperiode = Vilkårsperiode(vilkårId = vilkår.id,
                                        fom = LocalDate.now(),
                                        tom = LocalDate.now(),
                                        fulgtOppNav = "testverdi",
                                        vilkårsresultat = "testverdi",
                                        begrunnelse = "testverdi")

    val vilkårAktsomhet = VilkårAktsomhet(vilkårsperiodeId = vilkårsperiode.id,
                                          aktsomhet = "testverdi",
                                          ileggRenter = true,
                                          andelTilbakekreves = 123.11,
                                          manueltSattBeløp = null,
                                          begrunnelse = "testverdi",
                                          særligeGrunnerTilReduksjon = true,
                                          tilbakekrevSmabeløp = true,
                                          særligeGrunnerBegrunnelse = "testverdi")

    val vilkårSærligGrunn = VilkårSærligGrunn(vilkårAktsomhetId = vilkårAktsomhet.id,
                                              særligGrunn = "testverdi",
                                              begrunnelse = "testverdi")

    val vilkårGodTro = VilkårGodTro(vilkårsperiodeId = vilkårsperiode.id,
                                    beløpErIBehold = true,
                                    beløpTilbakekreves = 32165,
                                    begrunnelse = "testverdi")

    val eksternBehandling = EksternBehandling(behandlingId = behandling.id,
                                              eksternId = UUID.randomUUID(),
                                              henvisning = "testverdi")

    val faktaFeilutbetaling = FaktaFeilutbetaling(begrunnelse = "testverdi")

    val faktaFeilutbetalingsperiode = FaktaFeilutbetalingsperiode(faktaFeilutbetalingId = faktaFeilutbetaling.id,
                                                                  fom = LocalDate.now(),
                                                                  tom = LocalDate.now(),
                                                                  hendelsestype = "testverdi",
                                                                  hendelsesundertype = "testverdi")

    val grupperingFaktaFeilutbetaling = GrupperingFaktaFeilutbetaling(behandlingId = behandling.id,
                                                                      faktaFeilutbetalingId = faktaFeilutbetaling.id)

    val økonomiXmlMottatt = ØkonomiXmlMottatt(melding = "testverdi",
                                              sekvens = 1,
                                              tilkoblet = true,
                                              eksternFagsakId = "testverdi",
                                              henvisning = "testverdi")

    val totrinnsresultatsgrunnlag = Totrinnsresultatsgrunnlag(behandlingId = behandling.id,
                                                              grupperingFaktaFeilutbetalingId = grupperingFaktaFeilutbetaling.id,
                                                              grupperingVurdertForeldelseId = grupperingVurdertForeldelse.id,
                                                              vilkårId = vilkår.id)

    val vedtaksbrevsoppsummering = Vedtaksbrevsoppsummering(behandlingId = behandling.id,
                                                            oppsummeringFritekst = "testverdi",
                                                            fritekst = "testverdi")

    val vedtaksbrevsperiode = Vedtaksbrevsperiode(behandlingId = behandling.id,
                                                  fom = LocalDate.now(),
                                                  tom = LocalDate.now(),
                                                  fritekst = "testverdi",
                                                  fritekststype = "testverdi")

    val økonomiXmlSendt = ØkonomiXmlSendt(behandlingId = behandling.id,
                                          melding = "testverdi",
                                          kvittering = "testverdi",
                                          meldingstype = "testverdi")

    val grupperingKravvedtaksstatus = GrupperingKravvedtaksstatus(kravvedtaksstatus437Id = kravvedtaksstatus437.id,
                                                                  behandlingId = behandling.id)

    val varsel = Varsel(behandlingId = behandling.id,
                        varseltekst = "testverdi",
                        varselbeløp = 123)

    val brevsporing = Brevsporing(behandlingId = behandling.id,
                                  journalpostId = "testverdi",
                                  dokumentId = "testverdi",
                                  brevtype = "testverdi")

    val økonomiXmlMottattArkiv = ØkonomiXmlMottattArkiv(melding = "testverdi")

    val verge = Verge(ident = "testverdi",
                      gyldigFom = LocalDate.now(),
                      gyldigTom = LocalDate.now(),
                      type = "testverdi",
                      orgNr = "testverdi",
                      navn = "testverdi",
                      kilde = "testverdi",
                      begrunnelse = "testverdi")

    val grupperingVerge = GrupperingVerge(behandlingId = behandling.id,
                                          vergeId = verge.id)
}