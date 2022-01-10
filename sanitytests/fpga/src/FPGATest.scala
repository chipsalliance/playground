package sanitytests.fpga

import chipsalliance.rocketchip.config.Config
import freechips.rocketchip.subsystem.RocketTilesKey
import utest._

/** software dependencies:
  * clang -> bootrom cross compiling
  * vivado -> FPGA toolchain
  * after elaboration, run script: "out/FPGATest/bitstream.sh"
  * then bitstream should be located at "xxxShell.bit"
  * you can open snapshot "out/FPGATest/obj/post_synth.dcp", add your own ila configurations.
  * then run "out/FPGATest//rerunFromSynthesis.sh" to regenerated design with ila.
  */
object FPGATest extends TestSuite {

  val tests = Tests {
// TODO: Diplomacy is not
//    test("vcu118 build script") {
//      val outputDirectory = os.pwd / "out" / "VCU118"
//      os.remove.all(outputDirectory)
//      os.makeDir(outputDirectory)
//      val configs = Seq(classOf[FPGATestConfig], classOf[freechips.rocketchip.system.DefaultConfig])
//      FPGAHarness(configs, Some(outputDirectory), VCU118).rerunFromSynthesisScript
//      os.write(outputDirectory / "openocd.cfg", os.read(resource("openocd.cfg")))
//    }
    test("arty_a7_100 build script") {
      val outputDirectory = os.pwd / "out" / "ArtyA7100"
      os.remove.all(outputDirectory)
      os.makeDir(outputDirectory)

      val configs = Seq(
        classOf[FPGATestConfig],
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
