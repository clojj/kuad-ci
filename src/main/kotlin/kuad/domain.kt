package kuad

import arrow.core.Either
import arrow.core.Right
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

sealed class Build(val pipeline: Pipeline)
sealed class ActiveBuild(pipeline: Pipeline) : Build(pipeline)
class ReadyBuild(pipeline: Pipeline, val stepNext: Step) : ActiveBuild(pipeline)
class RunningBuild(pipeline: Pipeline, val step: Step) : ActiveBuild(pipeline)
class FinishedBuild(pipeline: Pipeline, val buildResult: BuildResult) : Build(pipeline)

sealed class BuildResult {
    object BuildSucceeded : BuildResult()
    object BuildFailed : BuildResult()
}

suspend fun ActiveBuild.progress(): Build =
    when (this) {
        is ReadyBuild -> {
            // TODO side effect: start container
            RunningBuild(pipeline, stepNext)
        }
        is RunningBuild -> {
            // TODO side effect: query status of container
            pipeline.nextStep().fold({ FinishedBuild(pipeline, it) }) { ReadyBuild(pipeline, it) }
            ReadyBuild(pipeline, step)
        }
    }

class Pipeline private constructor(
    val steps: NonEmptyList<Step>,
    val completedSteps: PersistentMap<StepName, StepResult> = persistentMapOf()
) {
    companion object {
        fun of(steps: List<Step>) = Pipeline(NonEmptyList(steps))
    }
}

suspend fun Pipeline.nextStep(): Either<BuildResult, Step> {
    val step = steps.list.firstOrNull {
        // TODO check current step's status
        !completedSteps.containsKey(it.stepName)
    }
    return if (step == null) Either.Left(BuildResult.BuildSucceeded) else Right(step)
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
    if (code == 0) StepResult.StepSucceeded else StepResult.StepFailed(this)

inline class StepName(val name: String)
inline class Command(val command: String)
inline class Image(val image: String)
