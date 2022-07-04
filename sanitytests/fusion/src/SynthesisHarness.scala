package sanitytests.fusion

import chipsalliance.rocketchip.config.Config
import freechips.rocketchip.diplomacy.LazyModule
import firrtl.AnnotationSeq
import firrtl.options.TargetDirAnnotation
import firrtl.passes.memlib.{GenVerilogMemBehaviorModelAnno, InferReadWriteAnnotation, ReplSeqMem}
import firrtl.stage.{FirrtlStage, OutputFileAnnotation, RunFirrtlTransformAnnotation}
import freechips.rocketchip.stage._
import freechips.rocketchip.system.RocketChipStage
import logger.LazyLogging
import chisel3.util.log2Ceil
import math._
import os._

case class SynthesisHarness[M <: chisel3.Module](
  synthesisHarness: Class[M],
  configs:     Seq[Class[_ <: Config]],
  targetDir:   Option[Path] = None) extends LazyLogging {
  /** compile [[synthesisHarness]] with correspond [[configs]] to output.
    * return output [[Path]].
    */
  lazy val output: Path = {
    val outputDirectory: Path = targetDir.getOrElse(os.temp.dir(deleteOnExit = false))
    logger.warn(s"start to compile output in $outputDirectory")
    val annotations: AnnotationSeq = Seq(
      new RocketChipStage,
      new FirrtlStage
    ).foldLeft(
      AnnotationSeq(
        Seq(
          TargetDirAnnotation(outputDirectory.toString),
          new TopModuleAnnotation(synthesisHarness),
          new ConfigsAnnotation(configs.map(_.getName)),
          InferReadWriteAnnotation,
          // run process memory replace blackbox
          GenVerilogMemBehaviorModelAnno(false),
          RunFirrtlTransformAnnotation(new ReplSeqMem),
          RunFirrtlTransformAnnotation(new firrtl.passes.InlineInstances),
          new OutputBaseNameAnnotation("SynthesisHarness")
        )
      )
    ) { case (annos, stage) => stage.transform(annos) }
    logger.warn(s"$synthesisHarness with configs: ${configs.mkString("_")} generated.")
    val duts = annotations.collect {
      case OutputFileAnnotation(file) => outputDirectory / s"$file.v"
    }
    val blackbox =
      os.read.lines(outputDirectory / firrtl.transforms.BlackBoxSourceHelper.defaultFileListName).map(Path(_))
    val fusionBuildDir = outputDirectory / "build"
    // TODO: add synthesis script
    val scriptsDir = fusionBuildDir / "scripts"
    logger.warn(s"Scripts location: $scriptsDir")
    scriptsDir
  }
}

/** The configuration of the targeted memory*/
case class MemCfg(
                 name: String = "",
                 depth: BigInt = 0,
                 width: Int = 0,
                 masked: Boolean = false,
                 maskGran: Int = 0,
                 maskSeg: Int = 0
                 ) {
  val addrWidth: Int = log2Ceil(depth)
}

/** The configuration of the instance IP for the memory*/
case class InstCfg(
                    memIPName: String,
                    instName: String,
                    addrPort: String,
                    inputBus: String,
                    outputBus: String
                  )