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
  def getMemConfig(memBlackBoxFilename: Path): MemCfg = {
    val regex = raw"// name:(\w+) depth:(\d+) width:(\d+) masked:(\w+) maskGran:(\d+) maskSeg:(\d+)".r
    val firstLine = os.read.lines(memBlackBoxFilename).head
    firstLine match {
      case regex(name, depth, width, masked, maskGran, maskSeg) =>
        MemCfg(
          name = name,
          depth = BigInt(depth),
          width = width.toInt,
          masked = masked.toBoolean,
          maskGran = maskGran.toInt,
          maskSeg = maskSeg.toInt
        )
      case _ =>
        println("[Error] No regex matched")
        MemCfg()
    }
  }
  def genSynthMem(memCfg: MemCfg): String = {
    // all the possible width in the mem compiler. Any Integer in Range(4, 512).
    val memWidthSeq = Seq(4, 512)
    val memDepthSeq = Seq(16, 32, 64, 128, 256, 512, 1024)
    val memName = memCfg.name
    val memDepth = memCfg.depth
    val memWidth = memCfg.width
    val memMasked = memCfg.masked
    val memMaskGran = memCfg.maskGran
    val memMaskSeg = memCfg.maskSeg
    val memAddrWidth = memCfg.addrWidth
    assert(memDepth <= memDepthSeq.max, s"[ERROR] The word count should small than ${memDepthSeq.max}.")
    // get the number of memory banks in case the memory width is greater than 512
    val memBankNumber = ceil(memWidth.toDouble / memWidthSeq.max.toDouble).toInt
    val instBit = if (memBankNumber == 1)
      max(memWidth, memWidthSeq.min) else
      memWidth - memWidthSeq.max * (memBankNumber - 1)
    val instDepth = pow(2, log2Ceil(memDepth)).toInt
    var instIPVerilog = ""
    for (instId <- 0 until memBankNumber) {
      val curInstBit = if (instId + 1 == memBankNumber) instBit else memWidthSeq.max
      val curInstDepth = instDepth
      val dataStart = instId * memWidthSeq.max
      val dataEnd = dataStart + curInstBit - 1
      val instName = s"${memName}_inst_$instId"
      val memIPName = s"SRAM1RW${curInstDepth}x$curInstBit"
      val inputBus = s"W0_data[$dataEnd:$dataStart]"
      val outputBus = s"rdata_o_$instId"
      instIPVerilog += s"    wire [${curInstBit-1}:0] $outputBus;\n"
      instIPVerilog += genMemInstVerilog(InstCfg(
        memIPName = memIPName,
        instName = instName,
        addrPort = "R0_addr",
        inputBus = inputBus,
        outputBus = outputBus
      ))
    }
    instIPVerilog += s"    assign R0_data = {${Range(0, memBankNumber).map(id =>
      s"rdata_o_$id").reduce((x, y) => s"$y, $x")}};\n"
    val utilWires =
      s"""    wire chip_select = R0_en | W0_en;
         |    wire wen_n = !W0_en;
         |    wire ren_n = !R0_en;
         |""".stripMargin
    val memVerilogString =
      s"""// name:$memName depth:$memDepth width:$memWidth masked:$memMasked maskGran:$memMaskGran maskSeg:$memMaskSeg
         |module $memName(
         |    input R0_clk,
         |    input [${memAddrWidth-1}:0] R0_addr,
         |    input R0_en,
         |    output [${memWidth-1}:0] R0_data,
         |    input W0_clk,
         |    input [${memAddrWidth-1}:0] W0_addr,
         |    input W0_en,
         |    input [${memWidth-1}:0] W0_data,
         |    input [${memMaskSeg-1}:0] W0_mask
         |);
         |
         |$utilWires
         |$instIPVerilog
         |
         |endmodule
         |""".stripMargin
    memVerilogString
  }
  def genMemInstVerilog(instCfg: InstCfg): String = {
    s"""    ${instCfg.memIPName} ${instCfg.instName} (
       |        .A  (${instCfg.addrPort}),  // Primary Read/Write Address
       |        .CE (R0_clk     ),  // Primary Positive-Edge Clock
       |        .WEN(wen_n      ),  // Primary Write Enable, Active Low
       |        .OEB(ren_n      ),  // Primary Output Enable, Active Low
       |        .CSB(chip_select),  // Primary Chip Select, Active Low
       |        .I  (${instCfg.inputBus}),  // Primary Input data bus
       |        .O  (${instCfg.outputBus})  // Primary Output data bus
       |    );
       |
       |""".stripMargin
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
