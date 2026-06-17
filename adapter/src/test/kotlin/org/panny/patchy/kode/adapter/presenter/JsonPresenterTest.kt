package org.panny.patchy.kode.adapter.presenter

import org.panny.patchy.kode.domain.error.NoProjectRootException
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JsonPresenterTest {

    private val presenter = JsonPresenter()

    @Test
    fun `renders error envelope with code message and details`() {
        val json = presenter.renderError(
            ErrorEnvelope.of(NoProjectRootException(details = "no settings file")),
        )

        assertTrue(json.contains("\"code\": \"NO_PROJECT_ROOT\""), json)
        assertTrue(json.contains("\"message\": \"No Gradle project root found\""), json)
        assertTrue(json.contains("\"details\": \"no settings file\""), json)
    }

    @Test
    fun `omits details when absent`() {
        val json = presenter.renderError(ErrorEnvelope.of(NoProjectRootException()))

        assertTrue(!json.contains("\"details\""), json)
    }
}
