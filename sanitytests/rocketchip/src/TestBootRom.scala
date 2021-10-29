package sanitytests.rocketchip

import chipsalliance.rocketchip.config.Config
import freechips.rocketchip.devices.tilelink.BootROMLocated
import os._

class TestBootRom
    extends Config((site, here, up) => {
      case BootROMLocated(x) =>
        up(BootROMLocated(x), site).map(_.copy(contentFileName = {
          val tmp = os.temp.dir()
          val elf = tmp / "bootrom.elf"
          val bin = tmp / "bootrom.bin"
          val img = tmp / "bootrom.img"
          /*
           * Infer clang executable name
           * This is a hack for Nix
           * In Nix, clang --target=riscv64 wont work
           * only riscv64-unknown-linux-gnu-clang would work
           */
          val is_nix = os.proc("riscv64-unknown-linux-gnu-clang", "-v").call(check = false)
          val clang = if (is_nix.exitCode != 0)  "clang" else "riscv64-unknown-linux-gnu-clang"
          // format: off
          proc(
            s"$clang",
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
    })
