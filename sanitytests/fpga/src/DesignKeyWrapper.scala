package sanitytests.fpga

import chipsalliance.rocketchip.config.Parameters
import chisel3.Module.clock
import chisel3._
import freechips.rocketchip.devices.debug.{Debug, JtagDTMKey}
import freechips.rocketchip.diplomacy.{LazyModule, LazyRawModuleImp}
import sifive.fpgashells.clocks._
import sifive.fpgashells.ip.xilinx.PowerOnResetFPGAOnly
import sifive.fpgashells.shell.{ClockInputDesignInput, ClockInputOverlayKey, FPGAFrequencyKey, JTAGDebugDesignInput, JTAGDebugOverlayKey}
import sifive.blocks.devices.spi._
import sifive.blocks.devices.uart._
import sifive.blocks.devices.gpio._
import sifive.blocks.devices.pinctrl._

object PinGen {
  def apply(): BasePin =  {
    val pin = new BasePin()
    pin
  }
}

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

    val qspi = new SPIPins(() => PinGen(), p(PeripherySPIFlashKey)(0))
    val gpio = new GPIOPins(() => PinGen(), p(PeripheryGPIOKey)(0))

    val sys_uart = subsystem.module.uart
    val uart_pins = p(PeripheryUARTKey).map { c => Wire(new UARTPins(() => PinGen()))}

    (uart_pins zip  sys_uart) map {case (p, r) => UARTPinsFromPort(p, r, clock = subsystem.module.clock, reset = subsystem.module.reset.asBool, syncStages = 0)}

    for (iof_0 <- subsystem.module.iof(0).get.iof_0) {
      iof_0.default()
    }

    for (iof_1 <- subsystem.module.iof(0).get.iof_1) {
      iof_1.default()
    }

    val iof_0 = subsystem.module.iof(0).get.iof_0
    val iof_1 = subsystem.module.iof(0).get.iof_1

    // UART0
    BasePinToIOF(uart_pins(0).rxd, iof_0(16))
    BasePinToIOF(uart_pins(0).txd, iof_0(17))

    // UART1
    BasePinToIOF(uart_pins(1).rxd, iof_0(24))
    BasePinToIOF(uart_pins(1).txd, iof_0(25))

    // Result of Pin Mux
    GPIOPinsFromPort(gpio, subsystem.module.gpio(0))

    // Dedicated SPI Pads
    SPIPinsFromPort(qspi, subsystem.module.qspi(0), clock = subsystem.module.clock, reset = subsystem.module.reset.asBool, syncStages = 3)
  }
  // generate and connect clock tree.
  val fpgaClockSource = p(ClockInputOverlayKey).head.place(ClockInputDesignInput()).overlayOutput.node
  val pll = p(PLLFactoryKey)()
  val resetWrangler = LazyModule(new ResetWrangler).node
  // default FPGAFrequencyKey is 100.0MHz, max synthesizable clk freq in this version is 80MHz, there is something to do to improve timing frequency
  val coreClock = ClockSinkNode(freqMHz = p(FPGAFrequencyKey))
  coreClock := resetWrangler := ClockGroup() := pll := fpgaClockSource
  val jtagOut = p(JTAGDebugOverlayKey).headOption.map(_.place(JTAGDebugDesignInput())).get.overlayOutput.jtag


  /* User design */
  val subsystem = LazyModule(new RocketFPGASubsystem(resetWrangler, pll)(p))
}
