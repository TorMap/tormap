package org.tormap.api

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import java.nio.file.Files
import java.nio.file.Path

/**
 * Keeps the committed API v1 contract (api/openapi-v1.json in the repository root) in sync with the code.
 * Clients such as the TorMap CLI are built and tested against that file, so any change to it shows up in review.
 *
 * After an intended change, update the file with:
 * `UPDATE_OPENAPI_SPEC=true ./gradlew test --tests 'org.tormap.api.ApiV1OpenApiSpecTest'`
 * Within v1 only additions are allowed; renaming or removing a path or field needs /v2/.
 */
@SpringBootTest(
    properties = [
        "springdoc.api-docs.enabled=true",
        // Swagger UI is never served by this test; enabling it only lets the security rules permit /openapi
        "springdoc.swagger-ui.enabled=true",
        "springdoc.paths-to-match=/v1/**",
        "springdoc.writer-with-order-by-keys=true",
    ]
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiV1OpenApiSpecTest(
    private val mockMvc: MockMvc,
) : StringSpec({
    val specFile = Path.of("..", "api", "openapi-v1.json")
    val mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build()

    fun generatedSpec(): String {
        val body = mockMvc.perform(get("/openapi")).andReturn().response.contentAsString
        val spec = mapper.readTree(body) as ObjectNode
        // The server URL depends on the request, and the title should not be springdoc's default
        spec.remove("servers")
        spec.putObject("info").put("title", "TorMap API").put("version", "v1")
        return mapper.writeValueAsString(spec) + "\n"
    }

    "the spec only contains API v1 paths" {
        val paths = mapper.readTree(generatedSpec()).get("paths").propertyNames().toList()
        paths.shouldNotBeEmpty()
        paths.filterNot { it.startsWith("/v1/") } shouldBe emptyList()
    }

    "the committed API v1 spec matches the code" {
        val generated = generatedSpec()
        if (System.getenv("UPDATE_OPENAPI_SPEC") == "true") {
            Files.createDirectories(specFile.parent)
            Files.writeString(specFile, generated)
        }
        val committed = if (Files.exists(specFile)) Files.readString(specFile) else ""
        withClue(
            "api/openapi-v1.json is out of date. If the API change is intended (only additions within v1), run " +
                    "UPDATE_OPENAPI_SPEC=true ./gradlew test --tests 'org.tormap.api.ApiV1OpenApiSpecTest' and commit the file."
        ) {
            committed shouldBe generated
        }
    }
})
