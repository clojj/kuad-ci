package kuad

import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.runBlocking
import kuad.BuildResult.BuildSucceeded
import kuad.StepResult.StepSucceeded
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BuildTests {

    private val pipeline = Pipeline.of(
        listOf(
            Step.of("First step", "ubuntu", listOf("date")),
            Step.of("Second step", "fedora", listOf("uname -r"))
        )
    )

    private fun Pipeline.step(i: Int): Step = steps.list[i]

    private val readyBuild = ReadyBuild(pipeline, pipeline.step(0), persistentMapOf())
    private val runningBuild = RunningBuild(pipeline, pipeline.step(0), persistentMapOf())

    @Test
    fun `ReadyBuild progresses to RunningBuild`() {
        runBlocking {
            val build = readyBuild.progress()
            assertThat(build).isEqualTo(RunningBuild(pipeline, readyBuild.readyStep, readyBuild.completed))
        }
    }

    @Test
    fun `successful RunningBuild progresses to ReadyBuild`() {
        runBlocking {
            val build = runningBuild.progress()
            assertThat(build).isEqualTo(ReadyBuild(pipeline, pipeline.step(1), runningBuild.completed.put(pipeline.step(0).stepName, StepSucceeded)))
        }
    }

    @Test
    fun `RunningBuild progresses to FinishedBuild`() {
        runBlocking {
            val running = RunningBuild(pipeline, pipeline.step(1), persistentMapOf(pipeline.step(0).stepName to StepSucceeded))
            val result = running.progress()
            assertThat(result).isEqualTo(FinishedBuild(pipeline, BuildSucceeded, persistentMapOf(pipeline.step(0).stepName to StepSucceeded, pipeline.step(1).stepName to StepSucceeded)))
            with(result as FinishedBuild) {
                assertThat(result.completed.keys).containsExactly(pipeline.step(0).stepName, pipeline.step(1).stepName)
                assertThat(result.completed.values).containsExactly(StepSucceeded, StepSucceeded)
            }
        }
    }
}

