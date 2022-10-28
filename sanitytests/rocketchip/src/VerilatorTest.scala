package sanitytests.rocketchip

import utest._

/** software dependencies:
  * clang -> bootrom cross compiling / veriltor C compiling
  * verilator -> emulator generation
  * cmake -> simulation
  * ninja -> fast verilator build tool
  * spike -> isa behavior model linking in emulator
  */
object VerilatorTest extends TestSuite {
  val outputDirectory = os.pwd / "out" / "VerilatorTest"
  os.remove.all(outputDirectory)
  os.makeDir(outputDirectory)
  val tests = Tests {
    test("build TestHarness emulator") {
      val testHarness = classOf[freechips.rocketchip.system.TestHarness]
      val configs = Seq(classOf[TestConfig], classOf[freechips.rocketchip.system.DefaultConfig])
      val emulator = TestHarness(testHarness, configs, Some(outputDirectory)).emulator
      test("build hello") {
        os.proc(
          "clang",
          "-o", "hello",
          s"${sanitytests.utils.resource("entry.S")}",
          s"${sanitytests.utils.resource("csrc/hello.c")}",
          "--target=riscv64",
          "-mcmodel=medany",
          "-mno-relax",
          "-nostdinc",
          "-fuse-ld=lld",
          s"-T${sanitytests.utils.resource("hello.ld")}",
          "-nostdlib",
          "-static",
        ).call(outputDirectory)
        test("Hello World!") {
          os.proc(
            s"$emulator",
            "hello",
          ).call(outputDirectory)
        }
      }
    }
  }
}
