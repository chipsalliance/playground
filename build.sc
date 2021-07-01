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
import $file.dependencies.`api-config-chipsalliance`.`build-rules`.mill.build
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
}

object mychisel3 extends dependencies.chisel3.build.chisel3CrossModule(ivys.sv) {
  override def millSourcePath = os.pwd / "dependencies" / "chisel3"

  def firrtlModule: Option[PublishModule] = Some(myfirrtl)

  def treadleModule: Option[PublishModule] = Some(mytreadle)
}

object mytreadle extends dependencies.treadle.build.treadleCrossModule(ivys.sv) {
  override def millSourcePath = os.pwd /  "dependencies" / "treadle"

  def firrtlModule: Option[PublishModule] = Some(myfirrtl)
}

object myconfig extends dependencies.`api-config-chipsalliance`.`build-rules`.mill.build.config with PublishModule {
  override def millSourcePath = os.pwd /  "dependencies" / "api-config-chipsalliance" / "design" / "craft"

  override def scalaVersion = ivys.sv

  override def pomSettings = myrocketchip.pomSettings()

  override def publishVersion = myrocketchip.publishVersion()
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

  def configModule: PublishModule = myconfig
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

object `firrtl-interpreter` extends CommonModule with SbtModule {
  override def millSourcePath = os.pwd /  "dependencies" / "firrtl-interpreter"
  override def ivyDeps = Agg(
    ivys.jline
  )
}

object iotesters extends CommonModule with SbtModule {
  override def millSourcePath = os.pwd /  "dependencies" / "chisel-testers"

  override def ivyDeps = Agg(
    ivys.scalatest,
    ivys.scalatestplus,
    ivys.scalacheck,
    ivys.scopt
 )

  override def moduleDeps = super.moduleDeps ++ Seq(mytreadle, `firrtl-interpreter`)
}

object myhardfloat extends dependencies.`berkeley-hardfloat`.build.hardfloat {
  override def millSourcePath = os.pwd /  "dependencies" / "berkeley-hardfloat"

  override def scalaVersion = ivys.sv

  def chisel3Module: Option[PublishModule] = Some(mychisel3)
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

  override def moduleDeps = super.moduleDeps ++ Seq(iotesters)

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

object gemmini extends CommonModule with SbtModule {
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")

  override def millSourcePath = os.pwd / "dependencies" / "gemmini"

  override def ivyDeps = Agg(
    ivys.breeze
  )
  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, testchipip)
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

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, iotesters, firesim.midas)
}

// I know it's quite strange, however UCB messly managed their dependency...
object chipyard extends CommonModule with SbtModule { cy =>
  // TODO: FIX
  override def scalacOptions = Seq("-Xsource:2.11")
  def basePath = os.pwd / "dependencies" / "chipyard"
  override def millSourcePath = basePath / "generators" / "chipyard"
  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, barstools, testchipip, blocks, icenet, boom, dsptools, rocketdsputils, gemmini, nvdla, hwacha, cva6, tracegen, sodor, sha3)

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
    os.proc(millSourcePath / "configure", "--prefix", "/usr").call(
      T.ctx.dest, Map("CC" -> "clang", "CXX" -> "clang++")
    )
    os.proc("make", "-j", Runtime.getRuntime().availableProcessors()).call(T.ctx.dest)
    T.ctx.dest
  }
}

// Dummy

object playground extends CommonModule {
  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, inclusivecache, blocks, rocketdsputils, shells, firesim, boom, chipyard, chipyard.fpga, chipyard.utilities)

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
