package no.nav.pensjon.integrationtest.kafka

import no.nav.pensjon.TestHelper.mapAnyToJson
import no.nav.pensjon.TestHelper.mockLoggMelding
import no.nav.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.pensjon.integrationtest.DataSourceTestConfig
import no.nav.pensjon.integrationtest.KafkaTestConfig
import org.junit.jupiter.api.Assertions.assertEquals
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
class KafkaInnkommendeFlereHendelserTest: KafkaListenerTest() {

    @Test
    fun `Når en flere hendelser er gyldige og ugyldige skal gyldige lagrs i db`() {
        val personIdent = "20903322123"

        val sanitycheck = loggTjeneste.hentAlleLoggInnslagForPerson(personIdent)
        assertEquals(0, sanitycheck.size)

        val loggMelding = mockLoggMelding(personIdent)
        val hendsleJson1 = mapAnyToJson(loggMelding)
        val hendsleJson2 = mapAnyToJson(loggMelding.copy(mottaker = "123456789"))
        val hendsleNotValidert = mapAnyToJson(loggMelding.copy(mottaker = "12345"))
        val hendsleUgydligJson = mockNoValidJson()

        initAndRunContainer().also { runTest ->
            runTest.sendMsgOnDefaultTopic(hendsleJson1)
            runTest.sendMsgOnDefaultTopic(hendsleUgydligJson)
            runTest.sendMsgOnDefaultTopic(hendsleNotValidert)
            runTest.sendMsgOnDefaultTopic(hendsleJson2)
            runTest.waitForlatch(kafkaLoggMeldingConsumer)
        }

        assertTrue(sjekkLoggingFinnes("Lagret melding: ID: 1, person: 209033xxxxx, tema: PEN, mottaker: 938908909"))
        assertTrue(sjekkLoggingFinnes("Lagret melding: ID: 2, person: 209033xxxxx, tema: PEN, mottaker: 123456789"))
        assertTrue(sjekkLoggingFinnes("Mottatt sporingsmelding kan ikke deserialiseres, må evt rettes og sendes inn på nytt Hendelse"))
        assertTrue(sjekkLoggingFinnes("Mottatt sporingsmelding kan ikke valideres, må evt rettes og sendes inn på nytt Hendelse"))
        assertEquals(2, loggTjeneste.hentAlleLoggInnslagForPerson(personIdent).size)
    }

}
