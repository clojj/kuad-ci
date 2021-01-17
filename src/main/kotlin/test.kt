import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

fun main() {
    // val process = ProcessBuilder("rg", "kotlin", "./", "--line-buffered").start()
    val process = ProcessBuilder("ping", "-c 8", "127.0.0.1").start()
    // val process = ProcessBuilder("podman", "pull", "fedora:latest").start()
    val stream = process.gobbleStream()
    Thread.sleep(3000)
    stream.run()
    stream.waitFor()
    println(process.exitValue())

/*
    val process = ProcessBuilder("ping", "127.0.0.1").start()
    process.gobbleStream()
    process.waitFor(3, TimeUnit.SECONDS)
    process.destroyForcibly()
*/
}

class StreamGobbler(private val process: Process, autorun: Boolean = true) : Runnable {
    private val latch = CountDownLatch(2)

    init {
        if (autorun) run()
    }

    override fun run() {
        val handler = Thread.UncaughtExceptionHandler { _, throwable ->
            if (throwable.message == "Stream closed") {
                latch.countDown()
            } else throw throwable
        }
        process.apply {
            thread(isDaemon = true) {
                inputStream.bufferedReader().forEachLine { line ->
                    println(line)
                }
                latch.countDown()
            }.uncaughtExceptionHandler = handler
            thread(isDaemon = true) {
                errorStream.bufferedReader().forEachLine { line ->
                    println("STDERR $line")
                }
                latch.countDown()
            }.uncaughtExceptionHandler = handler
        }
    }

    fun waitFor() = latch.await()
}

fun Process.gobbleStream() = StreamGobbler(this, false)
