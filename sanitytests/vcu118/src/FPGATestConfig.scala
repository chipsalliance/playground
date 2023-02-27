package sanitytests.vcu118

import org.chipsalliance.cde.config.{Config, Parameters}
import freechips.rocketchip.devices.debug.{DebugModuleKey, ExportDebug, JTAG}
import freechips.rocketchip.devices.tilelink.BootROMLocated
import os._
import sifive.fpgashells.shell.DesignKey

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
            "-fuse-ld=lld", s"-T${sanitytests.utils.resource("linker.ld")}",
            s"${sanitytests.utils.resource("bootrom.S")}",
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
    })
