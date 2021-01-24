package kuad

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ValuesTests : StringSpec({

    "step with no command is invalid" {
        shouldThrow<IllegalArgumentException> {
            Step.of("invalid", "image", emptyList())
        }.message shouldBe "Failed requirement."
    }
})
