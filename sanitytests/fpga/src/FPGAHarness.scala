package sanitytests.fpga

import chipsalliance.rocketchip.config.Config
import chisel3.RawModule
import firrtl.AnnotationSeq
import firrtl.annotations.MemorySynthInit
import firrtl.options.TargetDirAnnotation
import firrtl.passes.memlib.{
  DefaultReadFirstAnnotation,
  InferReadWrite,
  InferReadWriteAnnotation,
  PassthroughSimpleSyncReadMemsAnnotation,
  SeparateWriteClocks,
  SetDefaultReadUnderWrite
}
import firrtl.stage.{FirrtlStage, RunFirrtlTransformAnnotation}
import firrtl.transforms.SimplifyMems
import freechips.rocketchip.stage._
import freechips.rocketchip.system.RocketChipStage
import logger.LazyLogging
import os._

trait Board
object VCU118 extends Board
object ArtyA7100 extends Board

case class FPGAHarness[M <: RawModule](
  configs:   Seq[Class[_ <: Config]],
  targetDir: Option[Path] = None,
  board:     Board = VCU118)
    extends LazyLogging {

  /** You need to install non-local installed boards to vivado by:
    * {{{
    *   cd $VIVADO_PATH/data/xhub/boards
    *   git clone https://github.com/Xilinx/XilinxBoardStore
    * }}}
    */
  def tclBoard = board match {
    case VCU118    => "vcu118"
    case ArtyA7100 => "arty_a7_100"
  }
  lazy val filelist: Path = {
    logger.warn(s"start to elaborate fpga designs in $outputDirectory")
    Seq(
      new RocketChipStage,
      new FirrtlStage
    ).foldLeft(
      AnnotationSeq(
        Seq(
          TargetDirAnnotation(outputDirectory.toString),
          new TopModuleAnnotation(fpgaHarness),
          new ConfigsAnnotation(configs.map(_.getName)),
          RunFirrtlTransformAnnotation(new firrtl.passes.InlineInstances),
          new OutputBaseNameAnnotation("FPGAHarness"),
          // optimized for FPGA
          InferReadWriteAnnotation,
          RunFirrtlTransformAnnotation(new InferReadWrite),
          RunFirrtlTransformAnnotation(new SeparateWriteClocks),
          DefaultReadFirstAnnotation,
          RunFirrtlTransformAnnotation(new SetDefaultReadUnderWrite),
          RunFirrtlTransformAnnotation(new SimplifyMems),
          PassthroughSimpleSyncReadMemsAnnotation,
          MemorySynthInit
        )
      )
    ) { case (annos, stage) => stage.transform(annos) }
    logger.warn(s"$fpgaHarness with configs: ${configs.mkString("_")} generated.")
    val filelist = outputDirectory / "filelist"
    os.write(filelist, os.walk(outputDirectory).filter(_.ext == "v").map(_.toString).mkString("\n"))
    filelist
  }
  lazy val bitstreamScript: Path = {
    val script: Path = outputDirectory / "bitstream.sh"
    os.write(
      script,
      f"""
         |#!/usr/bin/env bash
         |cd $outputDirectory
         |vivado -nojournal -mode batch \\
         |  -source ${os.pwd / "dependencies" / "fpga-shells" / "xilinx" / "common" / "tcl" / "vivado.tcl"} \\
         |  -tclargs -top-module ${board match {
        case ArtyA7100 => "Arty100TShell"
        case VCU118    => "VCU118Shell"
      }} \\
         |    -F $filelist \\
         |    -ip-vivado-tcls "${os.walk(outputDirectory).filter(_.ext == "tcl").mkString(" ")}" \\
         |    -board $tclBoard
         |""".stripMargin
    )
    os.perms.set(script, "rwx------")
    script
  }
  lazy val rerunFromSynthesisScript: Path = {
    // depend on bitstream generation script
    bitstreamScript
    val script: Path = outputDirectory / "rerunFromSynthesis.sh"
    os.write(
      script,
      f"""
         |#!/usr/bin/env bash
         |cd $outputDirectory
         |vivado -nojournal -mode batch \\
         |  -source ${os.pwd / "dependencies" / "chipyard" / "fpga" / "scripts" / "run_impl_bitstream.tcl"} \\
         |  -tclargs \\
         |    ${outputDirectory / "obj" / "post_synth.dcp"} \\
         |    $tclBoard \\
         |    ${outputDirectory / "debug_obj"} \\
         |    ${os.pwd / "dependencies" / "fpga-shells" / "xilinx" / "common" / "tcl"} \\
         |""".stripMargin
    )
    os.perms.set(script, "rwx------")
    script
  }
  val fpgaHarness = board match {
    case VCU118    => classOf[sifive.fpgashells.shell.xilinx.VCU118Shell]
    case ArtyA7100 => classOf[sifive.fpgashells.shell.xilinx.Arty100TShell]
  }
  val outputDirectory: Path = targetDir.getOrElse(os.temp.dir(deleteOnExit = false))
}
