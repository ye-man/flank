package ftl.cli.firebase.test.android

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import ftl.config.FtlConstants
import ftl.test.util.FlankTestRunner
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.SystemOutRule
import org.junit.runner.RunWith
import picocli.CommandLine

@RunWith(FlankTestRunner::class)
class AndroidDoctorCommandTest {
    @Rule
    @JvmField
    val systemOutRule: SystemOutRule = SystemOutRule().enableLog().muteForSuccessfulTests()

    @Test
    fun androidDoctorCommandPrintsHelp() {
        val doctor = AndroidDoctorCommand()
        assertThat(doctor.usageHelpRequested).isFalse()
        CommandLine.run<Runnable>(doctor, System.out, "-h")

        val output = systemOutRule.log
        Truth.assertThat(output).startsWith(
            "Verifies flank firebase is setup correctly\n" +
                "\n" +
                "doctor [-fh] [-c=<configPath>]\n" +
                "\n" +
                "Description:\n" +
                "\n" +
                "Validates Android Flank YAML.\n" +
                "\n" +
                "\n" +
                "Options:\n" +
                "  -c, --config=<configPath>\n" +
                "               YAML config file path\n" +
                "  -h, --help   Prints this help message\n"
        )

        assertThat(doctor.usageHelpRequested).isTrue()
    }

    @Test
    fun androidDoctorCommandRuns() {
        AndroidDoctorCommand().run()
        // When there are no lint errors, output is a newline.
        val output = systemOutRule.log
        Truth.assertThat(output).isEqualTo("\n")
    }

    @Test
    fun androidDoctorCommandOptions() {
        val cmd = AndroidDoctorCommand()
        assertThat(cmd.configPath).isEqualTo(FtlConstants.defaultAndroidConfig)
        cmd.configPath = "tmp"
        assertThat(cmd.configPath).isEqualTo("tmp")

        assertThat(cmd.usageHelpRequested).isFalse()
        cmd.usageHelpRequested = true
        assertThat(cmd.usageHelpRequested).isTrue()

        assertThat(cmd.fix).isFalse()
        cmd.fix = true
        assertThat(cmd.fix).isTrue()
    }
}
