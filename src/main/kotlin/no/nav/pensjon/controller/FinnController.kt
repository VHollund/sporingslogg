package no.nav.pensjon.controller

import no.nav.pensjon.domain.LoggMelding
import no.nav.pensjon.tjeneste.LoggTjeneste
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException


@RestController
@Profile("!prod")
class FinnController( private val loggTjeneste: LoggTjeneste ) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/sporingslogg/test/finn/{ident}", produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ProtectedWithClaims(issuer = "difi", claimMap = [ "acr=Level4" ])
    fun finnLoggMelding(@PathVariable(value = "ident", required = true) ident: String) : List<String> {
        log.debug("søker innslag på : $ident")

        val result = loggTjeneste.finnAllePersonerStarterMed(ident)
        log.debug("resultat = $result")

        return result
    }

    @GetMapping("/sporingslogg/test/hent/{ident}", produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ProtectedWithClaims(issuer = "difi", claimMap = [ "acr=Level4" ])
    fun hentLoggMelding(@PathVariable(value = "ident", required = true) ident: String) : List<LoggMelding> {
        if (ident.length != 11) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ugyldig ident")
        log.debug("Henter ut pid : $ident")

        val result = loggTjeneste.hentAlleLoggInnslagForPerson(ident)
        val loggmeldinger = result.map { logginnslag ->
            LoggMelding.fromLoggInnslag(logginnslag)
        }

        return loggmeldinger
    }

}