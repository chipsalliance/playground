// import Mill dependency
import mill._
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
val sv = "2.12.12"

// Init local repositories from repositories if exist a build.sc
object myfirrtl extends dependencies.firrtl.build.firrtlCrossModule(sv) {
  override def millSourcePath = os.pwd / "dependencies" / "firrtl"
}

object mychisel3 extends dependencies.chisel3.build.chisel3CrossModule(sv) {
  override def millSourcePath = os.pwd / "dependencies" / "chisel3"

  def firrtlModule: Option[PublishModule] = Some(myfirrtl)

  def treadleModule: Option[PublishModule] = Some(mytreadle)
}

object mytreadle extends dependencies.treadle.build.treadleCrossModule(sv) {
  override def millSourcePath = os.pwd /  "dependencies" / "treadle"

  def firrtlModule: Option[PublishModule] = Some(myfirrtl)
}

object mychiseltest extends dependencies.`chisel-testers2`.build.chiseltestCrossModule(sv) {
  override def millSourcePath = os.pwd /  "dependencies" / "chisel-testers2"

  def chisel3Module: Option[PublishModule] = Some(mychisel3)

  def treadleModule: Option[PublishModule] = Some(mytreadle)
}

object myhardfloat extends dependencies.`berkeley-hardfloat`.build.hardfloat {
  override def millSourcePath = os.pwd /  "dependencies" / "berkeley-hardfloat"

  override def scalaVersion = sv

  def chisel3Module: Option[PublishModule] = Some(mychisel3)
}

object myconfig extends dependencies.`api-config-chipsalliance`.`build-rules`.mill.build.config with PublishModule {
  override def millSourcePath = os.pwd /  "dependencies" / "api-config-chipsalliance" / "design" / "craft"
  override def scalaVersion = sv

  override def pomSettings = myrocketchip.pomSettings()
  
  override def publishVersion = myrocketchip.publishVersion()
}

object myrocketchip extends dependencies.`rocket-chip`.common.CommonRocketChip {
  override def millSourcePath = os.pwd /  "dependencies" / "rocket-chip"

  override def scalaVersion = sv

  def chisel3Module: Option[PublishModule] = Some(mychisel3)

  def hardfloatModule: PublishModule = myhardfloat

  def configModule: PublishModule = myconfig
}


// For modules not support mill yet, need to have a ScalaModule depend on our own repositories.
trait CommonModule extends ScalaModule {
  override def scalaVersion = sv

  override def scalacOptions = Seq("-Xsource:2.11")

  override def moduleDeps: Seq[ScalaModule] = Seq(mychisel3)

  private val macroParadise = ivy"org.scalamacros:::paradise:2.1.1"

  override def compileIvyDeps = Agg(macroParadise)

  override def scalacPluginIvyDeps = Agg(macroParadise)
}

object inclusivecache extends CommonModule {
  override def millSourcePath = os.pwd / "dependencies" / "block-inclusivecache-sifive" / 'design / 'craft / "inclusivecache"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip)
}

object blocks extends CommonModule with SbtModule {
  override def millSourcePath = os.pwd / "dependencies" / "sifive-blocks"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip)
}

object shells extends CommonModule with SbtModule {
  override def millSourcePath = os.pwd / "dependencies" / "fpga-shells"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, blocks)
}

object playground extends CommonModule {
  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, inclusivecache, blocks, shells)

  override def scalacPluginClasspath = super.scalacPluginClasspath() ++ Agg(
    mychisel3.plugin.jar()
  )

  // add some scala ivy module you like here.
  override def ivyDeps = Agg(
    ivy"com.lihaoyi::upickle:latest.integration",
    ivy"com.lihaoyi::os-lib:latest.integration",
    ivy"com.lihaoyi::pprint:latest.integration",
    ivy"org.scala-lang.modules::scala-xml:latest.integration"
  )

  // use scalatest as your test framework
  object tests extends Tests {
    override def ivyDeps = Agg(ivy"org.scalatest::scalatest:latest.integration")

    override def moduleDeps = super.moduleDeps ++ Seq(mychiseltest)

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }
}
