package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.entity.ArtifactEntity
import com.example.ui.components.ArtifactCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleArtifact = ArtifactEntity(
      id = 1,
      title = "Kwantowy Algorytm Spójności OtakOS",
      category = "Algorytmy Kwantowe",
      proposingAgent = "Vektor-9",
      status = "NA_STOLE",
      priority = "Wysoki",
      description = "Testowy artefakt na marmurowym stole.",
      digitalPayload = "fun test() = true",
      consensusScore = 85
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        ArtifactCard(
          artifact = sampleArtifact,
          onPreview = {},
          onAgentCollaborate = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

