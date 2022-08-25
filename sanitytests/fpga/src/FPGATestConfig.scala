package sanitytests.fpga

import chipsalliance.rocketchip.config.{Config, Parameters}
import freechips.rocketchip.devices.debug.{DebugModuleKey, ExportDebug, JTAG}
import freechips.rocketchip.devices.tilelink.BootROMLocated
import freechips.rocketchip.subsystem.RocketTilesKey
import os._
import sifive.fpgashells.shell.{DesignKey,FPGAFrequencyKey}

class FPGATestConfig
    extends Config((site, here, up) => {
      case BootROMLocated(x) =>
        up(BootROMLocated(x), site).map(_.copy(contentFileName = {
          val tmp = os.temp.dir()
          val elf = tmp / "bootrom.elf"
          val bin = tmp / "bootrom.bin"
          val img = tmp / "bootrom.img"
          // format: off
          proc(
            "clang",
            "--target=riscv64", "-march=rv64gc",
            "-mno-relax",
            "-static",
            "-nostdlib",
            "-Wl,--no-gc-sections",
            "-fuse-ld=lld", s"-T${resource("linker.ld")}",
            s"${resource("bootrom.S")}",
            "-o", elf
          ).call()
          proc(
            "llvm-objcopy",
            "-O", "binary",
            elf,
            bin
          ).call()
          proc(
            "dd",
            s"if=$bin",
            s"of=$img",
            "bs=128",
            "count=1"
          ).call()
          // format: on
          img.toString()
        }))
      case DesignKey   => { (p: Parameters) => new DesignKeyWrapper()(p) }
      case ExportDebug => up(ExportDebug, site).copy(protocols = Set(JTAG))
      case DebugModuleKey => up(DebugModuleKey, site).map(_.copy(clockGate = false))
      // default FPGAFrequencyKey is 100.0MHz, max synthesizable clk freq in this version is 80MHz, there is something to do to improve timing frequency
      case FPGAFrequencyKey => (50.0)
    })

class WithNoScratchPad extends Config((site, here, up) => {
  case RocketTilesKey => up(RocketTilesKey).map(p => p.copy(dcache = p.dcache.map(p => p.copy(scratch = None))))
})