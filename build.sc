// import Mill dependency
import mill._
import mill.define.Sources
import mill.modules.Util
import scalalib._
// support BSP
import mill.bsp._
// input build.sc from each repositories.
import $file.dependencies.chisel3.build
import $file.dependencies.firrtl.build
import $file.dependencies.treadle.build
import $file.dependencies.`chisel-testers2`.build
import $file.dependencies.`api-config-chipsalliance`.build
import $file.dependencies.`berkeley-hardfloat`.build
import $file.dependencies.`rocket-chip`.common

// Global Scala Version
object ivys {
  val sv = "2.12.13"
  val upickle = ivy"com.lihaoyi::upickle:1.3.15"
  val oslib = ivy"com.lihaoyi::os-lib:0.7.8"
  val pprint = ivy"com.lihaoyi::pprint:0.6.6"
  val utest = ivy"com.lihaoyi::utest:0.7.10"
  val macroParadise = ivy"org.scalamacros:::paradise:2.1.1"
  val jline = ivy"org.scala-lang.modules:scala-jline:2.12.1"
  val scalatest = ivy"org.scalatest::scalatest:3.2.2"
  val scalatestplus = ivy"org.scalatestplus::scalacheck-1-14:3.1.1.1"
  val scalacheck = ivy"org.scalacheck::scalacheck:1.14.3"
  val scopt = ivy"com.github.scopt::scopt:3.7.1"
  val playjson =ivy"com.typesafe.play::play-json:2.6.10"
  val spire = ivy"org.typelevel::spire:0.16.2"
  val breeze = ivy"org.scalanlp::breeze:1.1"
}

object helper extends Module {
  def seed = T(s"${os.pwd.baseName}_${System.getProperty("user.name")}")
  def sync(server: String) = T.command {
    os.proc("rsync", "-avP", "--delete" , "--exclude=.git/", "--exclude=out/", s"${os.pwd.toString()}/", s"$server:/tmp/${seed()}").call()
  }
}

// For modules not support mill yet, need to have a ScalaModule depend on our own repositories.
trait CommonModule extends ScalaModule {
  override def scalaVersion = ivys.sv

  override def scalacPluginClasspath = super.scalacPluginClasspath() ++ Agg(
    mychisel3.plugin.jar()
  )

  override def moduleDeps: Seq[ScalaModule] = Seq(mychisel3)

  override def compileIvyDeps = Agg(ivys.macroParadise)

  override def scalacPluginIvyDeps = Agg(ivys.macroParadise)
}


// Chips Alliance

object myfirrtl extends dependencies.firrtl.build.firrtlCrossModule(ivys.sv) {
  override def millSourcePath = os.pwd / "dependencies" / "firrtl"
  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivys.pprint
  )
  override val checkSystemAntlr4Version = false
  override val checkSystemProtocVersion = false
  override val protocVersion = os.proc("protoc", "--version").call().out.text.dropRight(1).split(' ').last
  override val antlr4Version = os.proc("antlr4").call().out.text.split('\n').head.split(' ').last
}

object mychisel3 extends dependencies.chisel3.build.chisel3CrossModule(ivys.sv) {
  override def millSourcePath = os.pwd / "dependencies" / "chisel3"

  def firrtlModule: Option[PublishModule] = Some(myfirrtl)

  def treadleModule: Option[PublishModule] = Some(mytreadle)

  def chiseltestModule: Option[PublishModule] = Some(mychiseltest)
}

object mytreadle extends dependencies.treadle.build.treadleCrossModule(ivys.sv) {
  override def millSourcePath = os.pwd /  "dependencies" / "treadle"

  def firrtlModule: Option[PublishModule] = Some(myfirrtl)
}

object mycde extends dependencies.`api-config-chipsalliance`.build.cde(ivys.sv) with PublishModule {
  override def millSourcePath = os.pwd /  "dependencies" / "api-config-chipsalliance" / "cde"
}

object myrocketchip extends dependencies.`rocket-chip`.common.CommonRocketChip {
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")

  override def scalacPluginClasspath = super.scalacPluginClasspath() ++ Agg(
    mychisel3.plugin.jar()
  )

  override def millSourcePath = os.pwd /  "dependencies" / "rocket-chip"

  override def scalaVersion = ivys.sv

  def chisel3Module: Option[PublishModule] = Some(mychisel3)

  def hardfloatModule: PublishModule = myhardfloat

  def configModule: PublishModule = mycde
}


// SiFive

object inclusivecache extends CommonModule {
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")

  override def millSourcePath = os.pwd / "dependencies" / "block-inclusivecache-sifive" / 'design / 'craft / "inclusivecache"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip)
}

object blocks extends CommonModule with SbtModule {
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")

  override def millSourcePath = os.pwd / "dependencies" / "sifive-blocks"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip)
}

object shells extends CommonModule with SbtModule {
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")

  override def millSourcePath = os.pwd / "dependencies" / "fpga-shells"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, blocks)
}

// UCB

object mychiseltest extends dependencies.`chisel-testers2`.build.chiseltestCrossModule(ivys.sv) {
  override def millSourcePath = os.pwd /  "dependencies" / "chisel-testers2"

  def chisel3Module: Option[PublishModule] = Some(mychisel3)

  def treadleModule: Option[PublishModule] = Some(mytreadle)
}

object myhardfloat extends dependencies.`berkeley-hardfloat`.build.hardfloat {
  override def millSourcePath = os.pwd /  "dependencies" / "berkeley-hardfloat"

  override def scalaVersion = ivys.sv

  def chisel3Module: Option[PublishModule] = Some(mychisel3)

  override def scalacPluginClasspath = super.scalacPluginClasspath() ++ Agg(
    mychisel3.plugin.jar()
  )
}

object testchipip extends CommonModule with SbtModule {
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")

  override def millSourcePath = os.pwd / "dependencies" / "testchipip"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, blocks)
}

object icenet extends CommonModule with SbtModule {
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")

  override def millSourcePath = os.pwd / "dependencies" / "icenet"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, testchipip)
}


object mdf extends CommonModule with SbtModule {

  override def millSourcePath = os.pwd / "dependencies" / "plsi-mdf"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, blocks)

  override def ivyDeps = Agg(
    ivys.playjson
  )
}

object firesim extends CommonModule with SbtModule { fs =>
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")

  override def millSourcePath = os.pwd / "dependencies" / "firesim" / "sim"

  object midas extends CommonModule with SbtModule {
    // TODO: FIX
    override def scalacOptions = Seq("-Xsource:2.11")

    override def millSourcePath = fs.millSourcePath / "midas"

    override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, targetutils, mdf)

    object targetutils extends CommonModule with SbtModule
  }

  object lib extends CommonModule with SbtModule {
    override def millSourcePath = fs.millSourcePath / "firesim-lib"

    override def moduleDeps = super.moduleDeps ++ Seq(midas, testchipip, icenet)
  }

  override def moduleDeps = super.moduleDeps ++ Seq(lib, midas)
}

object boom extends CommonModule with SbtModule {
  override def millSourcePath = os.pwd / "dependencies" / "riscv-boom"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, testchipip)
}

object barstools extends CommonModule with SbtModule { bt =>
  override def millSourcePath = os.pwd / "dependencies" / "barstools"

  object macros extends CommonModule with SbtModule {
    override def millSourcePath = bt.millSourcePath / "macros"
    override def moduleDeps = super.moduleDeps ++ Seq(mdf)
  }

  object iocell extends CommonModule with SbtModule {
    override def millSourcePath = bt.millSourcePath / "iocell"
  }

  object tapeout extends CommonModule with SbtModule {
    override def millSourcePath = bt.millSourcePath / "tapeout"
  }

  override def moduleDeps = super.moduleDeps ++ Seq(macros, iocell, tapeout)
}

object dsptools extends CommonModule with SbtModule { dt =>
  override def millSourcePath = os.pwd / "dependencies" / "dsptools"

  override def ivyDeps = Agg(
    ivys.spire,
    ivys.breeze
  )
}

object rocketdsputils extends CommonModule with SbtModule {
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")
  override def millSourcePath = os.pwd / "dependencies" / "rocket-dsp-utils"
  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, dsptools)
}

object FFTGenerator extends CommonModule with SbtModule {
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")
  override def millSourcePath = os.pwd / "dependencies" / "FFTGenerator"
  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, dsptools, rocketdsputils)
}

object gemmini extends CommonModule with SbtModule {
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")

  override def millSourcePath = os.pwd / "dependencies" / "gemmini"

  override def ivyDeps = Agg(
    ivys.breeze
  )
  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, testchipip, firesim.lib)
}

object nvdla extends CommonModule with SbtModule {
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")

  override def millSourcePath = os.pwd / "dependencies" / "nvdla-wrapper"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip)
}

object cva6 extends CommonModule with SbtModule {
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")

  override def millSourcePath = os.pwd / "dependencies" / "cva6-wrapper"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip)
}

object hwacha extends CommonModule with SbtModule {
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")

  override def millSourcePath = os.pwd / "dependencies" / "hwacha"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip)
}

object sodor extends CommonModule with SbtModule {
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")

  override def millSourcePath = os.pwd / "dependencies" / "riscv-sodor"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip)
}

object sha3 extends CommonModule with SbtModule {
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")

  override def millSourcePath = os.pwd / "dependencies" / "sha3"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, mychiseltest, firesim.midas)
}

object ibex extends CommonModule with SbtModule {
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")

  override def millSourcePath = os.pwd / "dependencies" / "ibex-wrapper"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip)
}

// I know it's quite strange, however UCB messly managed their dependency...
object chipyard extends CommonModule with SbtModule { cy =>
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")
  def basePath = os.pwd / "dependencies" / "chipyard"
  override def millSourcePath = basePath / "generators" / "chipyard"
  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, barstools, testchipip, blocks, icenet, boom, dsptools, rocketdsputils, gemmini, nvdla, hwacha, cva6, tracegen, sodor, sha3, ibex, FFTGenerator)

  object tracegen extends CommonModule with SbtModule {
    // TODO: FIX
    override def scalacOptions = Seq("-Xsource:2.11")
    override def millSourcePath = basePath / "generators" / "tracegen"
    override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, inclusivecache, boom)
  }

  object fpga extends CommonModule with SbtModule {
    // TODO: FIX
    override def scalacOptions = Seq("-Xsource:2.11")
    override def millSourcePath = basePath / "fpga"
    override def moduleDeps = super.moduleDeps ++ Seq(shells, chipyard)
  }

  object utilities extends CommonModule with SbtModule {
    // TODO: FIX
    override def scalacOptions = Seq("-Xsource:2.11")
    override def millSourcePath = basePath / "generators" / "utilities"
    override def moduleDeps = super.moduleDeps ++ Seq(chipyard)
  }
}

// CI Tests
object sanitytests extends CommonModule {
  override def ivyDeps = Agg(
    ivys.oslib
  )
  object rocketchip extends Tests with TestModule.Utest {
    override def ivyDeps = Agg(
      ivys.utest
    )
    override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip)
    def libraryResources = T.sources {
      os.proc("make", s"DESTDIR=${T.ctx.dest}", "install").call(spike.compile())
      T.ctx.dest
    }
    override def resources: Sources = T.sources {
      super.resources() ++ libraryResources()
    }
  }
  object vcu118 extends Tests with TestModule.Utest {
    override def ivyDeps = Agg(
      ivys.utest
    )
    override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, shells)
  }
}

object spike extends Module {
  override def millSourcePath = os.pwd / "dependencies" / "riscv-isa-sim"
  // ask make to cache file.
  def compile = T.persistent {
    os.proc(millSourcePath / "configure", "--prefix", "/usr", "--without-boost", "--without-boost-asio", "--without-boost-regex").call(
      T.ctx.dest, Map("CC" -> "clang", "CXX" -> "clang++", "LD" -> "lld")
    )
    os.proc("make", "-j", Runtime.getRuntime().availableProcessors()).call(T.ctx.dest)
    T.ctx.dest
  }
}

object compilerrt extends Module {
  override def millSourcePath = os.pwd / "dependencies" / "llvm-project" / "compiler-rt"
  // ask make to cache file.
  def compile = T.persistent {
    os.proc("cmake", "-S", millSourcePath,
      "-DCOMPILER_RT_BUILD_LIBFUZZER=OFF",
      "-DCOMPILER_RT_BUILD_SANITIZERS=OFF",
      "-DCOMPILER_RT_BUILD_PROFILE=OFF",
      "-DCOMPILER_RT_BUILD_MEMPROF=OFF",
      "-DCOMPILER_RT_BUILD_ORC=OFF",
      "-DCOMPILER_RT_BUILD_BUILTINS=ON",
      "-DCOMPILER_RT_BAREMETAL_BUILD=ON",
      "-DCOMPILER_RT_INCLUDE_TESTS=OFF",
      "-DCOMPILER_RT_HAS_FPIC_FLAG=OFF",
      "-DCOMPILER_RT_DEFAULT_TARGET_ONLY=On",
      "-DCOMPILER_RT_OS_DIR=riscv64",
      "-DCMAKE_BUILD_TYPE=Release",
      "-DCMAKE_SYSTEM_NAME=Generic",
      "-DCMAKE_SYSTEM_PROCESSOR=riscv64",
      "-DCMAKE_TRY_COMPILE_TARGET_TYPE=STATIC_LIBRARY",
      "-DCMAKE_SIZEOF_VOID_P=8",
      "-DCMAKE_ASM_COMPILER_TARGET=riscv64-none-elf",
      "-DCMAKE_C_COMPILER_TARGET=riscv64-none-elf",
      "-DCMAKE_C_COMPILER_WORKS=ON",
      "-DCMAKE_CXX_COMPILER_WORKS=ON",
      "-DCMAKE_C_COMPILER=clang",
      "-DCMAKE_CXX_COMPILER=clang++",
      "-DCMAKE_C_FLAGS=-nodefaultlibs -fno-exceptions -mno-relax -Wno-macro-redefined -fPIC",
      "-Wno-dev",
    ).call(T.ctx.dest)
    os.proc("make", "-j", Runtime.getRuntime().availableProcessors()).call(T.ctx.dest)
    T.ctx.dest / "lib" / "riscv64"
  }
}

object musl extends Module {
  override def millSourcePath = os.pwd / "dependencies" / "musl"
  // ask make to cache file.
  def compile = T.persistent {
    os.proc(millSourcePath / "configure", "--target", "riscv64-none-elf").call(
      T.ctx.dest,
      Map (
        "CC" -> "clang",
        "CXX" -> "clang++",
        "AR" -> "llvm-ar",
        "RANLIB" -> "llvm-ranlib",
        "LD" -> "lld",
        "LIBCC" -> "-lclang_rt.builtins-riscv64",
        "CFLAGS" -> "--target=riscv64 -mno-relax",
        "LDFLAGS" -> s"-fuse-ld=lld --target=riscv64 -nostartfiles -nodefaultlibs -nolibc -nostdlib -L${compilerrt.compile()}",
      )
    )
    os.proc("make", "-j", Runtime.getRuntime().availableProcessors()).call(T.ctx.dest)
    T.ctx.dest / "lib"
  }
}

object pk extends Module {
  override def millSourcePath = os.pwd / "dependencies" / "riscv-pk"
  // ask make to cache file.
  def compile = T.persistent {
    val env = Map (
      "CC" -> "clang",
      "CXX" -> "clang++",
      "AR" -> "llvm-ar",
      "RANLIB" -> "llvm-ranlib",
      "LD" -> "lld",
      "CFLAGS" -> "--target=riscv64 -mno-relax -Wno-uninitialized -Wno-unknown-pragmas",
      "LDFLAGS" -> "-fuse-ld=lld --target=riscv64 -nostdlib",
    )
    os.proc(millSourcePath / "configure", "--host=riscv64-unknown-elf").call(T.ctx.dest, env)
    os.proc("make", "-j", Runtime.getRuntime().availableProcessors(), "pk").call(T.ctx.dest, env)
    T.ctx.dest / "pk"
  }
}

object hello extends Module {
  override def millSourcePath = os.pwd / "sanitytests" / "rocketchip" / "resources" / "csrc"
  // ask make to cache file.
  def compile = T.persistent {
    os.proc("clang",
      "-o", "hello",
      millSourcePath / "hello.c",
      "--target=riscv64",
      "-mno-relax",
      "-fuse-ld=lld",
      "-nostdlib",
      s"${musl.compile()}/crt1.o",
      s"${musl.compile()}/crti.o",
      s"-L${compilerrt.compile()}",
      "-lclang_rt.builtins-riscv64",
      s"-L${musl.compile()}",
      "-lc",
      s"${musl.compile()}/crtn.o",
      "-static",
    ).call(T.ctx.dest)
    T.ctx.dest / "hello"
  }
}

// Dummy

object playground extends CommonModule {
  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, inclusivecache, blocks, rocketdsputils, shells, firesim, boom, chipyard, chipyard.fpga, chipyard.utilities, mychiseltest)

  // add some scala ivy module you like here.
  override def ivyDeps = Agg(
    ivys.oslib,
    ivys.pprint
  )

  // use scalatest as your test framework
  object tests extends Tests with TestModule.ScalaTest {
    override def ivyDeps = Agg(
      ivys.scalatest
    )
    override def moduleDeps = super.moduleDeps ++ Seq(mychiseltest)
  }
}
