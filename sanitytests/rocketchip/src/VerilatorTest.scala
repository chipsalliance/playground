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
    }
    test("rocket-chip regression tests") {
      val rocketchipSrcPath = os.pwd / "dependencies" / "rocket-chip"
      val riscvtestsPath = resource("usr/local/share/riscv-tests/isa")
      test("run rocket-chip test buckets") {
        val testHarness = classOf[freechips.rocketchip.system.TestHarness]
        val rv64RegrTests = Seq(
          "rv64ud-v-fcvt",
          "rv64ud-p-fdiv",
          "rv64ud-v-fadd",
          "rv64uf-v-fadd",
          "rv64um-v-mul",
          "rv64mi-p-breakpoint",
          "rv64uc-v-rvc",
          "rv64ud-v-structural",
          "rv64si-p-wfi",
          "rv64um-v-divw",
          "rv64ua-v-lrsc",
          "rv64ui-v-fence_i",
          "rv64ud-v-fcvt_w",
          "rv64uf-v-fmin",
          "rv64ui-v-sb",
          "rv64ua-v-amomax_d",
          "rv64ud-v-move",
          "rv64ud-v-fclass",
          "rv64ua-v-amoand_d",
          "rv64ua-v-amoxor_d",
          "rv64si-p-sbreak",
          "rv64ud-v-fmadd",
          "rv64uf-v-ldst",
          "rv64um-v-mulh",
          "rv64si-p-dirty")
        val rv32RegrTests = Seq(
          "rv32mi-p-ma_addr",
          "rv32mi-p-csr",
          "rv32ui-p-sh",
          "rv32ui-p-lh",
          "rv32uc-p-rvc",
          "rv32mi-p-sbreak",
          "rv32ui-p-sll")
        test("test bucket 1") {
          // TODO: Add rocket-chip unit tests
        }
        def runTest(test: String)(target: (os.Path, String)) = {
          val (emulator, xLen) = target
          // vendor in
          val gdbserver = os.pwd / "dependencies" / "riscv-tests" / "debug" / "gdbserver.py"
          os.proc(
            s"$gdbserver",
            "--print-failures", "--print-log-names",
            s"--sim_cmd=$emulator +jtag_rbb_enable=1 dummybin",
            "--server_cmd=openocd",
            "--gdb=riscv64-none-elf-gdb",
            s"--$xLen",
            s"$rocketchipSrcPath/scripts/RocketSim$xLen.py",
            s"$test")
          .call(os.pwd,
            Map(
              "JTAG_DTM_ENABLE_SBA" -> "off",
              "TERM" -> "dumb" // gdb changed behavior of its output, see https://lists.gnu.org/archive/html/bug-readline/2020-11/msg00010.html
              ))
        }
        // TODO: vendor in riscv-tests submodule
        //test("test bucket 2") {
        //  val target = Seq("32", "64").zip(
        //      Seq(classOf[freechips.rocketchip.system.DefaultRV32Config],
        //        classOf[freechips.rocketchip.system.DefaultConfig]))
        //          .map({ case (xLen, config) =>
        //      val testOutputDirectory: os.Path = outputDirectory / "test_bucket_2" / xLen
        //      os.remove.all(testOutputDirectory)
        //      os.makeDir.all(testOutputDirectory)

        //      val configs = Seq(classOf[TestConfig], classOf[freechips.rocketchip.system.WithJtagDTMSystem], config)
        //      val emulator= TestHarness(testHarness, configs, Some(testOutputDirectory)).emulator
        //      (emulator, xLen)
        //    })
        //  test("MemTest64_32") { runTest("MemTest64")(target(0)) }
        //  test("DebugTest_32") { runTest("DebugTest")(target(0)) }
        //  test("MemTest64_64") { runTest("MemTest64")(target(1)) }
        //  test("DebugTest_64") { runTest("DebugTest")(target(1)) }
        //}
        //test("test bucket 3") {
        //  val target = Seq("32", "64").zip(
        //      Seq(classOf[freechips.rocketchip.system.DefaultRV32Config],
        //        classOf[freechips.rocketchip.system.DefaultConfig]))
        //          .map({ case (xLen, config) =>
        //      val testOutputDirectory: os.Path = outputDirectory / "test_bucket_3" / xLen
        //      os.remove.all(testOutputDirectory)
        //      os.makeDir.all(testOutputDirectory)

        //      val configs = Seq(classOf[TestConfig], classOf[freechips.rocketchip.system.WithJtagDTMSystem],
        //        classOf[freechips.rocketchip.system.WithDebugSBASystem], config)
        //      val emulator= TestHarness(testHarness, configs, Some(testOutputDirectory)).emulator
        //      (emulator, xLen)
        //    })
        //  test("MemTest64_32") { runTest("MemTest64")(target(0)) }
        //  test("MemTest32_32") { runTest("MemTest32")(target(0)) }
        //  test("MemTest8_32") { runTest("MemTest8")(target(0)) }
        //  test("MemTest64_64") { runTest("MemTest64")(target(1)) }
        //  test("MemTest32_64") { runTest("MemTest32")(target(1)) }
        //}
        test("test bucket 4") {
          val testOutputDirectory: os.Path = outputDirectory / "test_bucket_4"
          os.remove.all(testOutputDirectory)
          os.makeDir.all(testOutputDirectory)
          val configs = Seq(classOf[TestConfig], classOf[freechips.rocketchip.system.DefaultBufferlessConfig])
          val emulator = TestHarness(testHarness, configs, Some(testOutputDirectory)).emulator
          rv64RegrTests.foreach({ case test =>
            println(s"Testing $test")
            os.proc(
              s"$emulator",
              "+max-cycles=100000000",
              s"$riscvtestsPath/$test"
            ).call()
          })
        }
        test("test bucket 5") {
          val testOutputDirectory: os.Path = outputDirectory / "test_bucket_5"
          os.remove.all(testOutputDirectory)
          os.makeDir.all(testOutputDirectory)
          val configs = Seq(classOf[TestConfig], classOf[freechips.rocketchip.system.DefaultConfig])
          val emulator = TestHarness(testHarness, configs, Some(testOutputDirectory)).emulator
          rv64RegrTests.foreach(test => os.proc(
            s"$emulator",
            "+max-cycles=100000000",
            s"$riscvtestsPath/$test"
          ).call())
        }
        test("test bucket 6") {
          val testOutputDirectory: os.Path = outputDirectory / "test_bucket_6"
          os.remove.all(testOutputDirectory)
          os.makeDir.all(testOutputDirectory)
          val configs = Seq(classOf[TestConfig], classOf[freechips.rocketchip.system.TinyConfig])
          val emulator = TestHarness(testHarness, configs, Some(testOutputDirectory)).emulator
          rv32RegrTests.foreach(test => os.proc(
            s"$emulator",
            "+max-cycles=100000000",
            s"$riscvtestsPath/$test"
          ).call())
        }
        test("test bucket 7") {
          // Miscellaneous configs cover any remaining configurations not tested
          // above, but are included in the freechips.rocketchip.system package.
          // These are here to prevent regressions at the compilation level and
          // expected to be built, but not executed.
          val configList = Seq(
            classOf[freechips.rocketchip.system.DefaultSmallConfig],
            classOf[freechips.rocketchip.system.DualBankConfig],
            classOf[freechips.rocketchip.system.DualChannelConfig],
            classOf[freechips.rocketchip.system.DualChannelDualBankConfig],
            classOf[freechips.rocketchip.system.RoccExampleConfig],
            classOf[freechips.rocketchip.system.Edge128BitConfig],
            classOf[freechips.rocketchip.system.Edge32BitConfig],
            classOf[freechips.rocketchip.system.QuadChannelBenchmarkConfig],
            classOf[freechips.rocketchip.system.EightChannelConfig],
            classOf[freechips.rocketchip.system.DualCoreConfig],
            classOf[freechips.rocketchip.system.MemPortOnlyConfig],
            classOf[freechips.rocketchip.system.MMIOPortOnlyConfig]
          )
          configList.foreach(config => {
            val testOutputDirectory: os.Path = outputDirectory / "test_bucket_7"
            os.remove.all(testOutputDirectory)
            os.makeDir.all(testOutputDirectory)
            val configs = Seq(classOf[TestConfig], config)
            val emulator = TestHarness(testHarness, configs, Some(testOutputDirectory)).emulator
          })
        }
        //test("test bucket 8") {
        //  // TODO: Add StageGeneratorSpec tests
        //}
        //test("test bucket 9") {
        //  // TODO: Add scalafix-check
        //}
      }
    }
  }
}
