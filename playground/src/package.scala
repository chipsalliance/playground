import freechips.rocketchip._
import diplomacy._
import config._
import subsystem._

package object playground {
  /** helper to convert [[LazyModule]] to [[chisel3]] */
  def configToLazyModule[L <: LazyModule](lazyModuleClass: Class[L], config: Config): L = {
    lazyModuleClass.getConstructors()(0).newInstance(config.toInstance).asInstanceOf[L]
  }

  def configToRocketModule[L <: RocketSubsystem](rocketLazyModule: Class[L], config: Config): L = {
    configToLazyModule(rocketLazyModule, config)
  }
}
