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
          s"${resource("csrc/hello.c")}",
          "--target=riscv64",
          "-mno-relax",
          "-nostdinc",
          s"-I${resource("riscv64/usr/include")}",
          "-fuse-ld=lld",
          "-nostdlib",
          s"${resource("riscv64/usr/lib/crt1.o")}",
          s"${resource("riscv64/usr/lib/crti.o")}",
          s"${resource("riscv64/usr/lib/riscv64/libclang_rt.builtins-riscv64.a")}",
          s"${resource("riscv64/usr/lib/libc.a")}",
          s"${resource("riscv64/usr/lib/crtn.o")}",
          "-static",
        ).call(outputDirectory)
        os.proc("llvm-strip", "hello").call(outputDirectory)
        test("Hello World!") {
          os.proc(
            s"$emulator",
            s"${resource("riscv64/pk")}",
            "hello",
          ).call(outputDirectory)
        }
      }
    }
    test("rocket-chip regression tests") {
      val rocketchipSrcPath = os.pwd / "dependencies" / "rocket-chip"
      val riscvtestsSrcPath = os.pwd / "dependencies" / "riscv-tests"
      val riscvtestsBuildPath = outputDirectory / "riscv-tests"
      os.makeDir.all(riscvtestsBuildPath)
      test("build riscv-tests") {
        os.proc("autoupdate").call(riscvtestsSrcPath)
        os.proc("autoconf").call(riscvtestsSrcPath)
        os.proc(riscvtestsSrcPath / "configure").call(riscvtestsBuildPath)
        os.proc("make", "benchmarks", "-j", Runtime.getRuntime().availableProcessors(),
          "RISCV_GCC=clang --target=riscv64",
          s"RISCV_GCC_OPTS=-mno-relax -nostdinc -I${resource("riscv64/usr/include")} -DPREALLOCATE=1 -mcmodel=medany -static -std=gnu99 -O2 -ffast-math -fno-common -fno-builtin-printf",
          s"RISCV_LINK_OPTS=-static -L${resource("riscv64/usr/lib/riscv64")} -L${resource("riscv64/usr/lib")} -lm -Wl,-T,${riscvtestsSrcPath}/benchmarks/common/test.ld",
          "RISCV_OBJDUMP=llvm-objdump --arch-name=riscv64 --disassemble-all --disassemble-zeroes --section=.text --section=.text.startup --section=.text.init --section=.data"
        ).call(riscvtestsBuildPath)
        os.proc("make", "isa", "-j", Runtime.getRuntime().availableProcessors(),
          "RISCV_GCC=clang --target=riscv64",
          s"RISCV_GCC_OPTS=-mno-relax -nostdinc -I${resource("riscv64/usr/include")} -static -mcmodel=medany -fvisibility=hidden -nostdlib -nostartfiles -L${resource("riscv64/usr/lib/riscv64")} -L${resource("riscv64/usr/lib")} -lm -Wl,-T,${riscvtestsSrcPath}/benchmarks/common/test.ld",
          "RISCV_OBJDUMP=llvm-objdump --arch-name=riscv64 --disassemble-all --disassemble-zeroes --section=.text --section=.text.startup --section=.text.init --section=.data"
        ).call(riscvtestsBuildPath)
        os.proc("make", "install", s"DESTDIR=$riscvtestsBuildPath").call(riscvtestsBuildPath)
        println(s"riscv-tests compilation done at $riscvtestsBuildPath")
      }
      test("run rocket-chip test buckets") {
        val testHarness = classOf[freechips.rocketchip.system.TestHarness]
        val gdbserver = os.pwd / "dependencies" / "riscv-tests" / "debug" / "gdbserver.py"
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
        test("test bucket 2") {
//          Fixme: gdbserver.py: error: unrecognized arguments: .. /playground/dependencies/rocket-chip/scripts/RocketSim32.py MemTest64
          val configs32 = Seq(classOf[TestConfig], classOf[freechips.rocketchip.system.WithJtagDTMSystem], classOf[freechips.rocketchip.system.DefaultRV32Config])
          val emulator32= TestHarness(testHarness, configs32, Some(outputDirectory)).emulator
          for (test <- Seq("MemTest64", "DebugTest")) {
              os.proc(
                s"$gdbserver",
                "--print-failures --print-log-names",
                s"--sim_cmd=$emulator32 +jtag_rbb_enable=1 dummybin",
                "--server_cmd=openocd -s /usr/share/openocd/scripts",
                s"--32",
                s"$rocketchipSrcPath/scripts/RocketSim32.py",
                s"$test"
              ).call(outputDirectory)
          }
          val configs64 = Seq(classOf[TestConfig], classOf[freechips.rocketchip.system.WithJtagDTMSystem], classOf[freechips.rocketchip.system.DefaultConfig])
          val emulator64= TestHarness(testHarness, configs64, Some(outputDirectory)).emulator
          for (test <- Seq("MemTest64", "DebugTest")) {
            os.proc(
              s"$gdbserver",
              "--print-failures --print-log-names",
              s"--sim_cmd=$emulator64 +jtag_rbb_enable=1 dummybin",
              "--server_cmd=openocd -s /usr/share/openocd/scripts",
              s"--64",
              s"$rocketchipSrcPath/scripts/RocketSim64.py",
              s"$test"
            ).call(outputDirectory)
          }
        }
        test("test bucket 3") {
          val configs32 = Seq(classOf[TestConfig], classOf[freechips.rocketchip.system.WithJtagDTMSystem],
            classOf[freechips.rocketchip.system.WithDebugSBASystem], classOf[freechips.rocketchip.system.DefaultRV32Config])
          val emulator32= TestHarness(testHarness, configs32, Some(outputDirectory)).emulator
          for (test <- Seq("MemTest64", "MemTest32", "MemTest8")) {
            os.proc(
              s"$gdbserver",
              "--print-failures --print-log-names",
              s"--sim_cmd=$emulator32 +jtag_rbb_enable=1 dummybin",
              "--server_cmd=openocd -s /usr/share/openocd/scripts",
              s"--32",
              s"$rocketchipSrcPath/scripts/RocketSim32.py",
              s"$test"
            ).call(outputDirectory)
          }
          val configs64 = Seq(classOf[TestConfig], classOf[freechips.rocketchip.system.WithJtagDTMSystem],
            classOf[freechips.rocketchip.system.WithDebugSBASystem], classOf[freechips.rocketchip.system.DefaultConfig])
          val emulator64= TestHarness(testHarness, configs64, Some(outputDirectory)).emulator
          for (test <- Seq("MemTest64", "MemTest32")) {
            os.proc(
              s"$gdbserver",
              "--print-failures --print-log-names",
              s"--sim_cmd=$emulator64 +jtag_rbb_enable=1 dummybin",
              "--server_cmd=openocd -s /usr/share/openocd/scripts",
              s"--64",
              s"$rocketchipSrcPath/scripts/RocketSim64.py",
              s"$test"
            ).call(outputDirectory)
          }
        }
        test("test bucket 4") {
          val configs = Seq(classOf[TestConfig], classOf[freechips.rocketchip.system.DefaultBufferlessConfig])
          val emulator = TestHarness(testHarness, configs, Some(outputDirectory)).emulator
          rv64RegrTests.foreach(test => os.proc(
            s"$emulator",
            "+max-cycles=100000000",
            s"$riscvtestsBuildPath/isa/$test"
          ).call(riscvtestsBuildPath))
        }
        test("test bucket 5") {
          val configs = Seq(classOf[TestConfig], classOf[freechips.rocketchip.system.DefaultConfig])
          val emulator = TestHarness(testHarness, configs, Some(outputDirectory)).emulator
          rv64RegrTests.foreach(test => os.proc(
            s"$emulator",
            "+max-cycles=100000000",
            s"$riscvtestsBuildPath/isa/$test"
          ).call(riscvtestsBuildPath))
        }
        test("test bucket 6") {
          val configs = Seq(classOf[TestConfig], classOf[freechips.rocketchip.system.TinyConfig])
          val emulator = TestHarness(testHarness, configs, Some(outputDirectory)).emulator
          rv32RegrTests.foreach(test => os.proc(
            s"$emulator",
            "+max-cycles=100000000",
            s"$riscvtestsBuildPath/isa/$test"
          ).call(riscvtestsBuildPath))
        }
        test("test bucket 7") {
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
            val configs = Seq(classOf[TestConfig], config)
            val emulator = TestHarness(testHarness, configs, Some(outputDirectory)).emulator
          })
        }
        test("test bucket 8") {
          // TODO: Add StageGeneratorSpec tests
        }
        test("test bucket 9") {
          // TODO: Add scalafix-check
        }
      }
    }
  }
}
