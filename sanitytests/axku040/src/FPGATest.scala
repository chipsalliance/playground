package sanitytests.axku040

import chisel3._
import chipsalliance.rocketchip.config.{Config, Parameters}
import firrtl.AnnotationSeq
import firrtl.options.TargetDirAnnotation
import firrtl.stage.{FirrtlStage, RunFirrtlTransformAnnotation}
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink.BootROMLocated
import freechips.rocketchip.stage.{ConfigsAnnotation, OutputBaseNameAnnotation, TopModuleAnnotation}
import freechips.rocketchip.system.RocketChipStage
import logger.LazyLogging
import sanitytests.xcku040.resource
import sifive.blocks.devices.gpio.{GPIOParams, PeripheryGPIOKey}
import sifive.blocks.devices.i2c.{I2CParams, PeripheryI2CKey}
import sifive.blocks.devices.uart.{UARTParams, PeripheryUARTKey}
import sifive.fpgashells.shell.{DesignKey, FPGAFrequencyKey}
import os._
import utest._

class FPGATestConfig extends Config((site, here, up) => {
  case DesignKey => (p: Parameters) => new DesignKeyWrapper()(p)
  case FPGAFrequencyKey => 100.0
  case PeripheryGPIOKey => Seq(
    GPIOParams(address = 0x10000000, width = 4)
  )
  case PeripheryUARTKey => Seq(
    UARTParams(address = 0x10001000)
  )
  case PeripheryI2CKey => Seq(
    I2CParams(address = 0x10002000)
  )
  case ExportDebug => up(ExportDebug, site).copy(protocols = Set(JTAG))
  case DebugModuleKey => up(DebugModuleKey, site).map(_.copy(clockGate = false))
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
        s"${resource("bootrom.c")}",
        "-o", elf
      ).call()
      proc(
        "llvm-objcopy",
        "-O", "binary",
        elf,
        bin
      ).call()
      // format: on
      bin.toString()
    }))
})

case class TestHarness[M <: RawModule](
                                        configs:   Seq[Class[_ <: Config]],
                                        targetDir: Option[os.Path] = None)
  extends LazyLogging {
  lazy val filelist: os.Path = {
    logger.warn(s"start to elaborate fpga designs in $outputDirectory")
    Seq(
      new RocketChipStage,
      new FirrtlStage
    ).foldLeft(
      AnnotationSeq(
        Seq(
          TargetDirAnnotation(outputDirectory.toString),
          new TopModuleAnnotation(testHarness),
          new ConfigsAnnotation(configs.map(_.getName)),
          RunFirrtlTransformAnnotation(new firrtl.passes.InlineInstances),
          new OutputBaseNameAnnotation("TestHarness")
        )
      )
    ) { case (annos, stage) => stage.transform(annos) }
    logger.warn(s"$testHarness with configs: ${configs.mkString("_")} generated.")
    val filelist = outputDirectory / "filelist"
    os.write(filelist, os.walk(outputDirectory).filter(_.ext == "v").map(_.toString).mkString("\n"))
    filelist
  }
  lazy val bitstreamScript: os.Path = {
    val script = outputDirectory / "bitstream.sh"
    os.write(
      script,
      f"""
         |#!/usr/bin/env bash
         |cd $outputDirectory
         |vivado -nojournal -mode batch \\
         |  -source ${os.pwd / "dependencies" / "rocket-chip-fpga-shells" / "xilinx" / "common" / "tcl" / "vivado.tcl"} \\
         |  -tclargs -top-module AlinxAxku040Shell \\
         |    -F $filelist \\
         |    -ip-vivado-tcls "${os.walk(outputDirectory).filter(_.ext == "tcl").mkString(" ")}" \\
         |    -board alinx_axku040
         |""".stripMargin
    )
    os.perms.set(script, "rwx------")
    script
  }
  lazy val rerunFromSynthesisScript: os.Path = {
    // depend on bitstream generation script
    bitstreamScript
    val script = outputDirectory / "rerunFromSynthesis.sh"
    os.write(
      script,
      f"""
         |#!/usr/bin/env bash
         |cd $outputDirectory
         |vivado -nojournal -mode batch \\
         |  -source ${os.pwd / "dependencies" / "chipyard" / "fpga" / "scripts" / "run_impl_bitstream.tcl"} \\
         |  -tclargs \\
         |    ${outputDirectory / "obj" / "post_synth.dcp"} \\
         |    alinx_axku040 \\
         |    ${outputDirectory / "debug_obj"} \\
         |    ${os.pwd / "dependencies" / "rocket-chip-fpga-shells" / "xilinx" / "common" / "tcl"} \\
         |""".stripMargin
    )
    os.perms.set(script, "rwx------")
    script
  }
  val testHarness = classOf[sifive.fpgashells.shell.xilinx.AlinxAxku040Shell]
  val outputDirectory: os.Path = targetDir.getOrElse(os.temp.dir(deleteOnExit = false))
}

object FPGATest extends TestSuite {
  val outputDirectory = os.pwd / "out" / "FPGATest"
  os.remove.all(outputDirectory)
  os.makeDir(outputDirectory)
  val tests = Tests {
    test("FPGA build script") {
      val configs = Seq(classOf[FPGATestConfig], classOf[freechips.rocketchip.system.DefaultConfig])
      TestHarness(configs, Some(outputDirectory)).rerunFromSynthesisScript
    }
  }
}
