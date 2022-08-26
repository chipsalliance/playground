package sanitytests.fpga

import chipsalliance.rocketchip.config.Config
import freechips.rocketchip.subsystem.RocketTilesKey
import utest._

/** software dependencies:
  * clang -> bootrom cross compiling
  * vivado -> FPGA toolchain
  * after elaboration, run script: "out/ArtyA7100/bitstream.sh"
  * then bitstream should be located at "xxxShell.bit"
  * you can open snapshot "out/ArtyA7100/obj/post_synth.dcp", add your own ila configurations.
  * then run "out/ArtyA7100/rerunFromSynthesis.sh" to regenerated design with ila.
  */
object ArtyA7100Test extends TestSuite {

  val tests = Tests {
    test("arty_a7_100 build script") {
      val outputDirectory = os.pwd / "out" / "ArtyA7100"
      os.remove.all(outputDirectory)
      os.makeDir(outputDirectory)

      val configs = Seq(
        classOf[ArtyA7100TestConfig],
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
