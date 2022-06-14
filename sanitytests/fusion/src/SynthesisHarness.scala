package sanitytests.rocketchip

import chipsalliance.rocketchip.config.Config
import freechips.rocketchip.diplomacy.LazyModule
import firrtl.AnnotationSeq
import firrtl.options.TargetDirAnnotation
import firrtl.passes.memlib.{InferReadWriteAnnotation, GenVerilogMemBehaviorModelAnno}
import firrtl.stage.{FirrtlStage, OutputFileAnnotation, RunFirrtlTransformAnnotation}
import freechips.rocketchip.stage._
import freechips.rocketchip.system.RocketChipStage
import logger.LazyLogging
import os._

case class SynthesisHarness[M <: LazyModule](
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
          RunFirrtlTransformAnnotation(new firrtl.passes.InlineInstances),
          new OutputBaseNameAnnotation("TestHarness")
        )
      )
    ) { case (annos, stage) => stage.transform(annos) }
    logger.warn(s"$synthesisHarness with configs: ${configs.mkString("_")} generated.")
    val duts = annotations.collect {
      case OutputFileAnnotation(file) => outputDirectory / s"$file.v"
    }
    val blackbox =
      os.read.lines(outputDirectory / firrtl.transforms.BlackBoxSourceHelper.defaultFileListName).map(Path(_))
    // TODO: add synthesis script
  }
}
