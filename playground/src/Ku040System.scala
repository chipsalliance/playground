package ku040

/** This class is designed for ku040 board */

/** rocketchip dependency */

import chipsalliance.rocketchip.config.Config
import freechips.rocketchip._
import config._
import devices.tilelink._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._
import subsystem._

/** sifive blocks dependency */
import sifive.blocks.devices._
import gpio._
import uart._
import spi._

/** Example Top with periphery devices and ports, and a Rocket subsystem */
class Ku040RocketSystem(implicit p: Parameters) extends RocketSubsystem
  with HasHierarchicalBusTopology
  with HasAsyncExtInterrupts
  with CanHaveMasterAXI4MemPort
  with HasPeripheryBootROM
  with HasPeripherySPIFlash
  with HasPeripheryGPIO
  with HasPeripheryUART {
  override lazy val module = new Ku040RocketSystemModuleImp(this)
}

class Ku040RocketSystemModuleImp[+L <: Ku040RocketSystem](_outer: L) extends RocketSubsystemModuleImp(_outer)
  with HasRTCModuleImp
  with HasExtInterruptsModuleImp
  with HasPeripheryBootROMModuleImp
  with HasPeripherySPIFlashModuleImp
  with HasPeripheryGPIOModuleImp
  with HasPeripheryUARTModuleImp

class WithNKu040Cores(n: Int) extends Config((site, here, up) => {
  case RocketTilesKey => {
    val small = RocketTileParams(
      core = RocketCoreParams(useVM = false, fpu = None),
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

class Ku040Config extends Config(
  new Config((site, here, up) => {
    case BootROMParams => new BootROMParams(contentFileName = "rocketchip/bootrom/bootrom.img")
    case PeripheryGPIOKey => List(GPIOParams(address = 0x10012000, width = 4, includeIOF = true))
    case PeripheryUARTKey => List(
      UARTParams(address = 0x10013000),
      UARTParams(address = 0x10014000)
    )
    case PeripherySPIFlashKey => List(SPIFlashParams(fAddress = 0x20000000, rAddress = 0x10015000, sampleDelayBits = 3))
  }) ++
    new WithNKu040Cores(2) ++
    new WithJtagDTM ++
    new WithDefaultMemPort() ++
    new WithNoMMIOPort() ++
    new WithNoSlavePort() ++
    new WithDTS("freechips,rocketchip-ku040", Nil) ++
    new WithNExtTopInterrupts(2) ++
    new WithTimebase(BigInt(50000000)) ++
    new BaseSubsystemConfig
)