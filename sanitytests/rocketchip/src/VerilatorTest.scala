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
      test("build riscv-tests") {
        val riscvtestsSrcPath = os.pwd / "dependencies" / "riscv-tests"
        val riscvtestsBuildPath = outputDirectory / "riscv-tests"
        os.remove.all(riscvtestsBuildPath)
        os.makeDir(riscvtestsBuildPath)
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
      }
    }
  }
}
