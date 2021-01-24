package kuad

import arrow.core.Either
import arrow.core.Right
import kotlinx.collections.immutable.PersistentMap
import kuad.BuildResult.BuildSucceeded
import kuad.StepResult.StepSucceeded

sealed class Build

class Pipeline private constructor(val steps: NonEmptyList<Step>) {
    companion object {
        fun of(steps: List<Step>) = Pipeline(NonEmptyList(steps))
    }
}

data class ReadyBuild(val pipeline: Pipeline, val readyStep: Step, val completed: PersistentMap<StepName, StepResult>) : Build()
data class RunningBuild internal constructor(val pipeline: Pipeline, val runningStep: Step, val completed: PersistentMap<StepName, StepResult>) : Build()
data class FinishedBuild internal constructor(val pipeline: Pipeline, val buildResult: BuildResult, val completed: PersistentMap<StepName, StepResult>) : Build()

// ---

sealed class BuildResult {
    object BuildSucceeded : BuildResult()
    object BuildFailed : BuildResult()
}

suspend fun ReadyBuild.progress(): Build =
    // TODO side effect: start container
    RunningBuild(pipeline, readyStep, completed)


suspend fun RunningBuild.progress(): Build {

    // TODO side effect: query container-status of runningStep
    val completedNext = completed.put(runningStep.stepName, StepSucceeded)

    fun Pipeline.nextStep(): Either<BuildResult, Step> {
        val step = steps.list.firstOrNull {
            !completedNext.containsKey(it.stepName)
        }
        return if (step == null) Either.Left(BuildSucceeded) else Right(step)
    }

    return pipeline.nextStep().fold({ FinishedBuild(pipeline, it, completedNext) }) { ReadyBuild(pipeline, it, completedNext) }
}

data class Step private constructor(val stepName: StepName, val commands: NonEmptyList<Command>, val image: Image) {
    companion object {
        fun of(stepName: String, imageName: String, commands: List<String>) =
            Step(StepName(stepName), commands.map(::Command).toNel(), Image(imageName))
    }
}

sealed class StepResult {
    object StepSucceeded : StepResult()
    data class StepFailed(val containerExitCode: ContainerExitCode) : StepResult()
}

inline class ContainerExitCode(val code: Int)

fun ContainerExitCode.toStepResult() =
    if (code == 0) StepSucceeded else StepResult.StepFailed(this)

inline class StepName(val name: String)
inline class Command(val command: String)
inline class Image(val image: String)
