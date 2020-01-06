package arty100t

/** This class is designed for arty100t board */

/** rocketchip dependency */

import chipsalliance.rocketchip.config.Config
import freechips.rocketchip._
import config._
import devices.tilelink._
import freechips.rocketchip.devices.debug.HasPeripheryDebugModuleImp
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._
import subsystem._

/** sifive blocks dependency */
import sifive.blocks.devices._
import uart._
import spi._

/** Example Top with periphery devices and ports, and a Rocket subsystem */
class CustomArty100TRocketSystem(implicit p: Parameters) extends RocketSubsystem
  with HasHierarchicalBusTopology
  with HasAsyncExtInterrupts
  with CanHaveMasterAXI4MemPort
  with HasPeripheryBootROM
  with HasPeripherySPIFlash
  with HasPeripheryUART {

  override lazy val module = new CustomArty100TRocketSystemModuleImp(this)
}

class CustomArty100TRocketSystemModuleImp[+L <: CustomArty100TRocketSystem](_outer: L) extends RocketSubsystemModuleImp(_outer)
  with HasRTCModuleImp
  with HasExtInterruptsModuleImp
  with HasPeripheryBootROMModuleImp
  with HasPeripherySPIFlashModuleImp
  with HasPeripheryUARTModuleImp
  with HasPeripheryDebugModuleImp

class WithNCustomArty100TCores(n: Int) extends Config((site, here, up) => {
  case RocketTilesKey => {
    val small = RocketTileParams(
      core = RocketCoreParams(useVM = true, fpu = None, mulDiv = None),
      btb = None,
      dcache = Some(DCacheParams(
        rowBits = site(SystemBusKey).beatBits,
        nSets = 32,
        nWays = 1,
        nTLBEntries = 4,
        nMSHRs = 0,
        blockBytes = site(CacheBlockBytes))),
      icache = Some(ICacheParams(
        rowBits = site(SystemBusKey).beatBits,
        nSets = 2,
        nWays = 1,
        nTLBEntries = 4,
        blockBytes = site(CacheBlockBytes))))
    List.tabulate(n)(i => small.copy(hartId = i))
  }
})

class CustomArty100TConfig extends Config(
  new Config((site, here, up) => {
    case BootROMParams => new BootROMParams(contentFileName = "rocketchip/bootrom/bootrom.img")
    case PeripheryUARTKey => List(
      UARTParams(address = 0x10012000),
    )
    case PeripherySPIFlashKey => List(SPIFlashParams(fAddress = 0x20000000, rAddress = 0x10013000, sampleDelayBits = 3))
  }) ++
    new WithNCustomArty100TCores(2) ++
    new WithJtagDTM ++
    new WithDefaultMemPort() ++
    new WithNoMMIOPort() ++
    new WithNoSlavePort() ++
    new WithDTS("freechips,rocketchip-arty100t", Nil) ++
    new WithNExtTopInterrupts(0) ++
    new WithoutTLMonitors ++
    new WithTimebase(BigInt(50000000)) ++
    new BaseSubsystemConfig
)