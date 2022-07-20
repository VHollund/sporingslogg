package no.nav.pensjon.integrationtest.kafka

import no.nav.pensjon.TestHelper.mockLoggMeldingAsJson
import no.nav.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.pensjon.integrationtest.DataSourceTestConfig
import no.nav.pensjon.integrationtest.KafkaTestConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles


@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest( classes = [DataSourceTestConfig::class, KafkaTestConfig::class, UnsecuredWebMvcTestLauncher::class], value = ["SPRING_PROFILES_ACTIVE", "unsecured-webmvctest"])
@ActiveProfiles("unsecured-webmvctest")
@DirtiesContext
@EmbeddedKafka(topics = [TOPIC])
class KafkaInnkommendeGydligHendelseTest: KafkaListenerTest() {

    @Test
    fun `Når en hendsle av LoggMelding er gyldig SÅ skal det lagres til db`() {
        val personIdent = "20903322123"

        Assertions.assertEquals(0, loggTjeneste.hentAlleLoggInnslagForPerson(personIdent).size)

        val hendsleJson = mockLoggMeldingAsJson(personIdent)

        initAndRunContainer().also {
            it.sendMsgOnDefaultTopic(hendsleJson)
            it.waitForlatch(kafkaLoggMeldingConsumer)
        }

        Assertions.assertEquals(1, loggTjeneste.hentAlleLoggInnslagForPerson(personIdent).size)
        assertTrue(sjekkLoggingFinnes("Lagret melding: ID: 1, person: 209033xxxxx, tema: PEN, mottaker: 938908909"))

    }


}
