package sanitytests.fpga

import chipsalliance.rocketchip.config.Config
import freechips.rocketchip.subsystem.RocketTilesKey
import utest._

/** software dependencies:
  * clang -> bootrom cross compiling
  * vivado -> FPGA toolchain
  * after elaboration, run script: "out/Arty100T/bitstream.sh"
  * then bitstream should be located at "Arty100TShell.bit"
  * you can open snapshot "out/Arty100T/obj/post_synth.dcp", add your own ila configurations.
  * then run "out/Arty100T/rerunFromSynthesis.sh" to regenerated design with ila.
  */
object Arty100TTest extends TestSuite {

  val tests = Tests {
    test("arty_a7_100 build script") {
      val outputDirectory = os.pwd / "out" / "Arty100T"
      os.remove.all(outputDirectory)
      os.makeDir(outputDirectory)

      val configs = Seq(
        classOf[Arty100TTestConfig],
        classOf[WithNoScratchPad],
        classOf[freechips.rocketchip.subsystem.With1TinyCore],
        classOf[freechips.rocketchip.subsystem.WithCoherentBusTopology],
        classOf[freechips.rocketchip.system.BaseConfig]
      )
      FPGAHarness(configs, Some(outputDirectory), ArtyA7100).rerunFromSynthesisScript
      os.write(outputDirectory / "openocd.cfg", os.read(resource("openocd.cfg")))
    }
  }
}
