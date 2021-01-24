import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import arrow.core.traverseEither

fun propertyViaJVM(key: String): Either<String, String> {
    val result = System.getProperty(key)
    return if (result != null) {
        Right(result)
    } else {
        Left("No JVM property: $key")
    }
}

fun main() {
    System.setProperty("foo", "test1.txt")
    System.setProperty("bar", "test2.txt")
    System.setProperty("zed", "test3.txt")

    demoTraverse(listOf("foo", "bar", "zed"))
    demoTraverse(listOf("false", "bar", "zed"))
    demoTraverse(listOf("foo", "false", "zed"))
    demoTraverse(listOf("foo", "bar", "false"))
}

//https://gist.github.com/garthgilmour/1fb98cb7f41cc4164705cdbc5f1ff8a7#file-traverse3-kt
fun demoTraverse(input: List<String>) {
    val failure = { error: String -> println(error) }
    val success = { results: List<String> ->
        println("Results are:")
        results.map { s -> "\t$s" }
            .forEach(::println)
    }

    val result: Either<String, List<String>> = input.traverseEither(::propertyViaJVM)
    result.fold(failure) { success(it) }
}
