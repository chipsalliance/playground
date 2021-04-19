package sanitytests.vcu118

import chipsalliance.rocketchip.config.Parameters
import chisel3._
import freechips.rocketchip.devices.debug.{Debug, JtagDTMKey}
import freechips.rocketchip.diplomacy.{LazyModule, LazyRawModuleImp}
import sifive.fpgashells.clocks._
import sifive.fpgashells.ip.xilinx.PowerOnResetFPGAOnly
import sifive.fpgashells.shell.{ClockInputDesignInput, ClockInputOverlayKey, FPGAFrequencyKey, JTAGDebugDesignInput, JTAGDebugOverlayKey}

/** This is the class where [[sifive.fpgashells.shell.DesignKey]] should be pointed to. */
class DesignKeyWrapper()(implicit p: Parameters) extends LazyModule {
  // generate subsystem
  override lazy val module = new LazyRawModuleImp(this) {
    // connect clock and reset
    val clockBundle = coreClock.in.head._1
    childClock := clockBundle.clock
    childReset := clockBundle.reset | subsystem.module.debug.map(_.ndreset).getOrElse(false.B)
    // connect jtag
    val systemJtag = subsystem.module.debug.get.systemjtag.get
    systemJtag.jtag.TCK := jtagOut.TCK
    systemJtag.jtag.TMS := jtagOut.TMS
    systemJtag.jtag.TDI := jtagOut.TDI
    jtagOut.TDO.data := systemJtag.jtag.TDO.data
    jtagOut.TDO.driven := systemJtag.jtag.TDO.driven
    systemJtag.reset := PowerOnResetFPGAOnly(childClock).asAsyncReset
    systemJtag.mfr_id := p(JtagDTMKey).idcodeManufId.U(11.W)
    systemJtag.part_number := p(JtagDTMKey).idcodePartNum.U(16.W)
    systemJtag.version := p(JtagDTMKey).idcodeVersion.U(4.W)
    Debug.connectDebugClockAndReset(subsystem.module.debug, childClock)
    subsystem.module.resetctrl.foreach { rc =>
      rc.hartIsInReset.foreach { _ := childReset.asBool() }
    }
  }
  // generate and connect clock tree.
  val fpgaClockSource = p(ClockInputOverlayKey).head.place(ClockInputDesignInput()).overlayOutput.node
  val pll = p(PLLFactoryKey)()
  val resetWrangler = LazyModule(new ResetWrangler).node
  val coreClock = ClockSinkNode(freqMHz = p(FPGAFrequencyKey))
  coreClock := resetWrangler := ClockGroup() := pll := fpgaClockSource
  val jtagOut = p(JTAGDebugOverlayKey).headOption.map(_.place(JTAGDebugDesignInput())).get.overlayOutput.jtag
  val subsystem = LazyModule(new RocketFPGASubsystem(resetWrangler, pll)(p))
}
