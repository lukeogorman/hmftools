package com.hartwig.hmftools.extensions.cli.options.filesystem

import com.google.common.io.Resources
import com.hartwig.hmftools.extensions.cli.createCommandLine
import com.hartwig.hmftools.extensions.cli.options.HmfOptions
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import java.io.IOException

class RequiredInputFileOptionTest : StringSpec() {

    private val TEST_FILE_PATH = Resources.getResource("cli/testFile.txt").path
    private val TEST_FILE_OPTION = "test_file"

    init {
        "works for valid required input file" {
            val optionsWrapper = HmfOptions()
            optionsWrapper.add(RequiredInputFileOption(TEST_FILE_OPTION, "Path to test file."))
            val cmd = optionsWrapper.options.createCommandLine("test", arrayOf("-$TEST_FILE_OPTION", TEST_FILE_PATH))
            optionsWrapper.validate(cmd)
            cmd.hasOption(TEST_FILE_OPTION) shouldBe true
            cmd.getOptionValue(TEST_FILE_OPTION) shouldBe TEST_FILE_PATH
        }

        "throws on missing required input file" {
            val optionsWrapper = HmfOptions()
            optionsWrapper.add(RequiredInputFileOption(TEST_FILE_OPTION, "Path to test file."))
            val cmd = optionsWrapper.options.createCommandLine("test", arrayOf("-$TEST_FILE_OPTION", "missing_file.txt"))
            shouldThrow<IOException> { optionsWrapper.validate(cmd) }
        }

        "throws on folder instead of required input file" {
            val optionsWrapper = HmfOptions()
            optionsWrapper.add(RequiredInputFileOption(TEST_FILE_OPTION, "Path to test file."))
            val cmd = optionsWrapper.options.createCommandLine("test", arrayOf("-$TEST_FILE_OPTION", "."))
            shouldThrow<IOException> { optionsWrapper.validate(cmd) }
        }
    }
}
