package kuad

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BuildTests {

    private val testPipeline = Pipeline.of(
        listOf(
            Step.of("First step", "ubuntu", listOf("date")),
            Step.of("Second step", "ubuntu", listOf("uname -r"))
        )
    )

    private val testBuild = ReadyBuild(testPipeline, testPipeline.steps.list.first())

    @Test
    fun `ReadyBuild is progressed RunningBuild`() {
        runBlocking {
            val build = testBuild.progress()
            assertThat(build).isEqualTo(RunningBuild(testPipeline, testBuild.stepNext))
        }
    }
}

