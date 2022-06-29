package sanitytests.rocketchip

import chipsalliance.rocketchip.config.Config
import freechips.rocketchip.devices.debug.{DebugModuleKey, ExportDebug, JTAG}
import freechips.rocketchip.devices.tilelink.BootROMLocated
import freechips.rocketchip.util.ClockGateModelFile
import os._

class SynthesisConfig
    extends Config((site, here, up) => {
      case ClockGateModelFile => Some("/vsrc/EICG_wrapper.v")
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
      case ExportDebug => up(ExportDebug, site).copy(protocols = Set())
      case DebugModuleKey => up(DebugModuleKey, site).map(_.copy(clockGate = false))
    })
