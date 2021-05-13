package sanitytests.vcu118

import utest._

/** software dependencies:
  * clang -> bootrom cross compiling
  * vivado -> FPGA toolchain
  * after elaboration, run script: "out/FPGATest/bitstream.sh"
  * then bitstream should be located at "VCU118Shell.bit"
  * you can open snapshot "out/FPGATest/obj/post_synth.dcp", add your own ila configurations.
  * then run "out/FPGATest//rerunFromSynthesis.sh" to regenerated design with ila.
  */
object FPGATest extends TestSuite {
  val outputDirectory = os.pwd / "out" / "FPGATest"
  os.remove.all(outputDirectory)
  os.makeDir(outputDirectory)
  val tests = Tests {
    test("fpga build script") {
      val configs = Seq(classOf[FPGATestConfig], classOf[freechips.rocketchip.system.DefaultConfig])
      TestHarness(configs, Some(outputDirectory)).rerunFromSynthesisScript
      os.write(outputDirectory / "openocd.cfg", os.read(resource("openocd.cfg")))
    }
  }
}
