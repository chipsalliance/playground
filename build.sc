import mill._
import scalalib._

trait CommonModule extends ScalaModule {
  def scalaVersion = "2.12.4"
  def scalacOptions = Seq("-Xsource:2.11")
  def ivyDeps = Agg(
    ivy"edu.berkeley.cs::chisel3:3.2.1",
  )
  private val macroParadise = ivy"org.scalamacros:::paradise:2.1.0"

  def compileIvyDeps = Agg(macroParadise)

  def scalacPluginIvyDeps = Agg(macroParadise)
}

object config extends CommonModule {
  def millSourcePath = super.millSourcePath / 'design / 'craft
}

object rocketchip extends CommonModule with SbtModule {
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"${scalaOrganization()}:scala-reflect:${scalaVersion()}",
    ivy"org.json4s::json4s-jackson:3.6.1"
  )

  object hardfloat extends CommonModule with SbtModule

  object macros extends CommonModule with SbtModule

  def moduleDeps = Seq(config, macros, hardfloat)

  def mainClass = Some("rocketchip.Generator")
}

object inclusivecache extends CommonModule {
  def millSourcePath = super.millSourcePath / 'design / 'craft / 'inclusivecache
  def moduleDeps = Seq(rocketchip)
}

object blocks extends CommonModule with SbtModule {
  def moduleDeps = Seq(rocketchip)
}
