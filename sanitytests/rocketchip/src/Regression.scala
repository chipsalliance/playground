package sanitytests.regression

import sanitytests.rocketchip.{TestBootRom, TestHarness}
import utest._

/** software dependencies:
  * clang -> bootrom cross compiling / veriltor C compiling
  * verilator -> emulator generation
  * cmake -> simulation
  * ninja -> fast verilator build tool
  * spike -> isa behavior model linking in emulator
  */
object Regression extends TestSuite {
  val outputDirectory = os.pwd / "out" / "Regression"
  os.remove.all(outputDirectory)
  os.makeDir(outputDirectory)
  val testHarness = classOf[freechips.rocketchip.system.TestHarness]

  val RocketSuiteA = Seq(classOf[TestBootRom], classOf[freechips.rocketchip.system.DefaultConfig])
  val RocketSuiteB = Seq(classOf[TestBootRom], classOf[freechips.rocketchip.system.DefaultBufferlessConfig])
  val RocketSuiteC = Seq(classOf[TestBootRom], classOf[freechips.rocketchip.system.TinyConfig])

  val tests = Tests {
    test("RocketSuiteA") {
      val configs = RocketSuiteA
      val emulator = TestHarness(testHarness, configs, Some(outputDirectory)).emulator
    }
    test("RocketSuiteB") {
      val configs = RocketSuiteB
      val emulator = TestHarness(testHarness, configs, Some(outputDirectory)).emulator
    }
    test("RocketSuiteC") {
      val configs = RocketSuiteC
      val emulator = TestHarness(testHarness, configs, Some(outputDirectory)).emulator
    }
  }
}
