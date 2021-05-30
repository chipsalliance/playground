package sanitytests.vcu118
import chipsalliance.rocketchip.config.Config
import chisel3.RawModule
import firrtl.AnnotationSeq
import firrtl.options.TargetDirAnnotation
import firrtl.passes.memlib.{InferReadWrite, InferReadWriteAnnotation, ReplSeqMem, ReplSeqMemAnnotation}
import firrtl.stage.{FirrtlStage, RunFirrtlTransformAnnotation}
import freechips.rocketchip.stage._
import freechips.rocketchip.system.RocketChipStage
import logger.LazyLogging
import os._

case class TestHarness[M <: RawModule](
  configs:   Seq[Class[_ <: Config]],
  targetDir: Option[Path] = None)
    extends LazyLogging {
  lazy val filelist: Path = {
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
  lazy val bitstreamScript: Path = {
    val script: Path = outputDirectory / "bitstream.sh"
    os.write(
      script,
      f"""
         |#!/usr/bin/env bash
         |cd $outputDirectory
         |vivado -nojournal -mode batch \\
         |  -source ${os.pwd / "dependencies" / "fpga-shells" / "xilinx" / "common" / "tcl" / "vivado.tcl"} \\
         |  -tclargs -top-module VCU118Shell \\
         |    -F $filelist \\
         |    -ip-vivado-tcls "${os.walk(outputDirectory).filter(_.ext == "tcl").mkString(" ")}" \\
         |    -board vcu118
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
         |    vcu118 \\
         |    ${outputDirectory / "debug_obj"} \\
         |    ${os.pwd / "dependencies" / "fpga-shells" / "xilinx" / "common" / "tcl"} \\
         |""".stripMargin
    )
    os.perms.set(script, "rwx------")
    script
  }
  val testHarness = classOf[sifive.fpgashells.shell.xilinx.VCU118Shell]
  val outputDirectory: Path = targetDir.getOrElse(os.temp.dir(deleteOnExit = false))
}
