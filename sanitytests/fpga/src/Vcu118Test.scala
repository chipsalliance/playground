package sanitytests.fpga

import chipsalliance.rocketchip.config.Config
import freechips.rocketchip.subsystem.RocketTilesKey
import utest._

/** software dependencies:
  * clang -> bootrom cross compiling
  * vivado -> FPGA toolchain
  * after elaboration, run script: "out/VCU118/bitstream.sh"
  * then bitstream should be located at "xxxShell.bit"
  * you can open snapshot "out/VCU118/obj/post_synth.dcp", add your own ila configurations.
  * then run "out/VCU118/rerunFromSynthesis.sh" to regenerated design with ila.
  */
object Vcu118Test extends TestSuite {

  val tests = Tests {
    test("vcu118 build script") {
      val outputDirectory = os.pwd / "out" / "VCU118"
      os.remove.all(outputDirectory)
      os.makeDir(outputDirectory)
      val configs = Seq(
        classOf[Vcu118TestConfig],
        classOf[freechips.rocketchip.system.DefaultConfig]
      )
      FPGAHarness(configs, Some(outputDirectory), VCU118).rerunFromSynthesisScript
      os.write(outputDirectory / "openocd.cfg", os.read(resource("openocd.cfg")))
   }
  }
}
