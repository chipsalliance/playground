package playground

import chipsalliance.rocketchip.config.{Config, Parameters}
import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, NoRunFirrtlCompilerAnnotation}
import firrtl.AnnotationSeq
import firrtl.options.TargetDirAnnotation
import freechips.rocketchip.devices.debug.{Debug, ExportDebug, JTAG, JtagDTMKey}
import freechips.rocketchip.diplomacy.{
  AddressSet,
  BundleBridgeSource,
  DTSTimebase,
  LazyModule,
  LazyRawModuleImp,
  RationalCrossing
}
import freechips.rocketchip.rocket.{DCacheParams, ICacheParams, MulDivParams, RocketCoreParams}
import freechips.rocketchip.subsystem._
import freechips.rocketchip.system.DefaultConfig
import freechips.rocketchip.tile.RocketTileParams
import freechips.rocketchip.tilelink.{
  TLFIFOFixer,
  TLFragmenter,
  TLInwardNode,
  TLOutwardNode,
  TLRAM,
  TLWidthWidget,
  TLXbar
}
import sifive.fpgashells.clocks._
import sifive.fpgashells.ip.xilinx.PowerOnResetFPGAOnly
import sifive.fpgashells.shell.xilinx.VCU118Shell
import sifive.fpgashells.shell.{
  ClockInputDesignInput,
  ClockInputOverlayKey,
  DDRDesignInput,
  DDROverlayKey,
  DDROverlayOutput,
  DDRShellInput,
  DesignKey,
  DesignPlacer,
  FPGAFrequencyKey,
  JTAGDebugDesignInput,
  JTAGDebugOverlayKey,
  PCIeDesignInput,
  PCIeOverlayKey,
  PCIeOverlayOutput,
  PCIeShellInput
}

/** value of [[sifive.fpgashells.shell.DesignKey]] */
class FireSimDesignWrapper()(implicit p: Parameters) extends LazyModule {
  lazy val module = new LazyRawModuleImp(this) {
    val subsystemJtag = subsystem.module.debug.getOrElse(throw new Exception("need JTAG")).systemjtag.get
    subsystemJtag.jtag.TCK := fpgaJtag.TCK
    subsystemJtag.jtag.TMS := fpgaJtag.TMS
    subsystemJtag.jtag.TDI := fpgaJtag.TDI
    fpgaJtag.TDO.data := subsystemJtag.jtag.TDO.data
    fpgaJtag.TDO.driven := subsystemJtag.jtag.TDO.driven
    val jtagDTM = p(JtagDTMKey)
    subsystemJtag.mfr_id := jtagDTM.idcodeManufId.U
    subsystemJtag.part_number := jtagDTM.idcodePartNum.U
    subsystemJtag.version := jtagDTM.idcodeVersion.U
    subsystemJtag.reset := PowerOnResetFPGAOnly(childClock)
    Debug.connectDebugClockAndReset(subsystem.module.debug, childClock)
    childClock := coreClock.in.head._1.clock
    childReset := coreClock.in.head._1.reset | subsystem.module.debug.map(_.ndreset).getOrElse(false.B)
    subsystem.module.resetctrl.foreach { rc => rc.hartIsInReset.foreach(_ := childReset.asBool()) }
  }
  val inputClockSourceNode: ClockSourceNode = p(ClockInputOverlayKey).head.place(ClockInputDesignInput()).overlayOutput.node
  val pllNode:       PLLNode = p(PLLFactoryKey)()
  val coreGroup:     ClockGroupNode = ClockGroup()
  val resetWrangler: ClockAdapterNode = LazyModule(new ResetWrangler).node
  val coreClock:     ClockSinkNode = ClockSinkNode(freqMHz = p(FPGAFrequencyKey))
  coreClock := resetWrangler := coreGroup := pllNode := inputClockSourceNode
  val fpgaJtag = p(JTAGDebugOverlayKey).headOption.map(_.place(JTAGDebugDesignInput())).get.overlayOutput.jtag
  val subsystem = LazyModule(new FireSimSubsystem(resetWrangler, pllNode)(p))
}

/** @note I think this should be a shim. but I need more information. */
class FireSimSubsystem(resetWranglerNode: ClockAdapterNode, pllNode: PLLNode)(implicit p: Parameters)
    extends BaseSubsystem
    with HasRocketTiles {
  override lazy val module =
    new BaseSubsystemModuleImp(this) with HasTilesModuleImp {
      resetVectorSourceNode.bundle := extMem.master.base.U
    }

  val extMem = p(ExtMem).getOrElse(throw new Exception("ExtMem is not configured, unable to initiate main memory."))
  // Jump to DDR directly
  val resetVectorSourceNode = BundleBridgeSource[UInt]()
  tileResetVectorNexusNode := resetVectorSourceNode
  val ddr = p(DDROverlayKey).headOption
    .map(
      (_: DesignPlacer[DDRDesignInput, DDRShellInput, DDROverlayOutput])
        .place(
          DDRDesignInput(
            extMem.master.base,
            resetWranglerNode,
            pllNode
          )
        )
        .overlayOutput
    )
  // Connect DDR or on-chip RAM to mbus
  if (ddr.isDefined) {
    ddr.get.ddr :=
      mbus.toDRAMController(Some("xilinx-mig-main-memory"))()
  } else {
    LazyModule(new TLRAM(AddressSet(extMem.master.base, extMem.master.size - 1), beatBytes = mbus.beatBytes)).node :=
      TLFragmenter(mbus.beatBytes, mbus.blockBytes) :=
      mbus.toDRAMController(Some("tlsram-main-memory"))()
  }
  // PCIeOverlayKey 0 -> fmc
  // PCIeOverlayKey 0 -> edge
  p(PCIeOverlayKey).tail.zipWithIndex.map {
    case (pciePlacer: DesignPlacer[PCIeDesignInput, PCIeShellInput, PCIeOverlayOutput], index: Int) => {
      val pcieOutput = pciePlacer
        .place(
          PCIeDesignInput(
            wrangler = resetWranglerNode,
            corePLL = pllNode
          )
        )
        .overlayOutput
      locateTLBusWrapper(L2).coupleFrom(s"xilinx-pcie-out$index") {
        (_: TLInwardNode) :*= TLXbar() :*= TLFIFOFixer(TLFIFOFixer.all)
      } :*=
        pcieOutput.pcieNode :*=
        locateTLBusWrapper(CBUS).coupleTo(s"xilinx-pcie-in$index") {
          TLWidthWidget(cbus.beatBytes) :*= (_: TLOutwardNode)
        }
      ibus.fromSync := pcieOutput.intNode
    }
  }
}

object Top extends App {
  Seq(
    new chisel3.stage.ChiselStage
  ).foldLeft(
    AnnotationSeq(
      Seq(
        ChiselGeneratorAnnotation(() => LazyModule(new VCU118Shell()(new ExampleConfig)).module),
        TargetDirAnnotation("/tmp/firesim"),
        NoRunFirrtlCompilerAnnotation
      )
    )
  ) { case (anno, stage) => stage.transform(anno) }
}

class ExampleConfig
    extends Config(
      (new DefaultConfig).alter(new WithInclusiveCache).alter((site, here, up) => {
        case DesignKey => (p: Parameters) => new FireSimDesignWrapper()(p)
        case PeripheryBusKey =>
          up(PeripheryBusKey, site).copy(dtsFrequency =
            Some(BigDecimal(site(FPGAFrequencyKey) * 1000000).setScale(0, BigDecimal.RoundingMode.HALF_UP).toBigInt)
          )
        case ExportDebug       => up(ExportDebug, site).copy(protocols = Set(JTAG))
        case DTSTimebase       => BigInt(1000000)
        case MemoryBusKey      => up(MemoryBusKey).copy(beatBytes = 8)
        case SystemBusKey      => up(SystemBusKey).copy(beatBytes = 8)
        case RocketCrossingKey => List.fill(1)(RocketCrossingParams(crossingType = RationalCrossing()))
        case RocketTilesKey =>
          List.tabulate(2)(i =>
            RocketTileParams(
              core =
                RocketCoreParams(mulDiv = Some(MulDivParams(mulUnroll = 8, mulEarlyOut = true, divEarlyOut = true))),
              dcache = Some(
                DCacheParams(rowBits = site(SystemBusKey).beatBits, nMSHRs = 0, blockBytes = site(CacheBlockBytes))
              ),
              icache = Some(ICacheParams(rowBits = site(SystemBusKey).beatBits, blockBytes = site(CacheBlockBytes)))
            ).copy(hartId = i)
          )

      })
    )
