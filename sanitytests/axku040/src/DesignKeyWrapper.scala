package sanitytests.axku040

import chisel3._
import chipsalliance.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink.TLInwardNode
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink.{BootROM, BootROMLocated}
import sifive.blocks.devices.gpio._
import sifive.blocks.devices.i2c._
import sifive.blocks.devices.spi._
import sifive.blocks.devices.uart._
import sifive.fpgashells.clocks._
import sifive.fpgashells.ip.xilinx.PowerOnResetFPGAOnly
import sifive.fpgashells.shell._
import sifive.fpgashells.shell.xilinx.DDRAlinxAxku040PlacedOverlay

class RocketFPGASubsystem(resetWrangler: ClockAdapterNode, pll: PLLNode, ddr: TLInwardNode)(implicit p: Parameters)
extends RocketSubsystem
  with HasPeripheryDebug with HasPeripheryGPIO with HasPeripheryUART with HasPeripheryI2C
 {
  override lazy val module = new RocketFPGASubsystemImp(this)

  p(BootROMLocated(location)).map { BootROM.attach(_, this, CBUS) }

  ddr := mbus.toDRAMController(Some("xilinx_mig"))()
}
class RocketFPGASubsystemImp(val out: RocketFPGASubsystem)
extends RocketSubsystemModuleImp(out)
  with HasPeripheryDebugModuleImp
  with HasPeripheryGPIOModuleImp
  with HasPeripheryUARTModuleImp
  with HasPeripheryI2CModuleImp

class DesignKeyWrapper()(implicit p: Parameters) extends LazyModule {
  private val fpgaClockSource = p(ClockInputOverlayKey).head.place(ClockInputDesignInput()).overlayOutput.node
  private val pll = p(PLLFactoryKey)()
  private val coreClock = ClockSinkNode(freqMHz = p(FPGAFrequencyKey), jitterPS = 500.0)
  private val resetWrangler = LazyModule(new ResetWrangler()).node
  private val led1 = p(LEDOverlayKey).head.place(LEDDesignInput()).overlayOutput.led
  private val led2 = p(LEDOverlayKey).head.place(LEDDesignInput()).overlayOutput.led
  private val led3 = p(LEDOverlayKey).head.place(LEDDesignInput()).overlayOutput.led
  private val led4 = p(LEDOverlayKey).head.place(LEDDesignInput()).overlayOutput.led
  private val jtag = p(JTAGDebugOverlayKey).head.place(JTAGDebugDesignInput()).overlayOutput.jtag
  private val ddr = p(DDROverlayKey).head
    .place(DDRDesignInput(0x80000000L, resetWrangler, pll))
    .asInstanceOf[DDRAlinxAxku040PlacedOverlay]
  private val rocket = LazyModule(new RocketFPGASubsystem(resetWrangler, pll, ddr.overlayOutput.ddr))
  private val uartBundleBridgeSrcNode = BundleBridgeSource(() => new UARTPortIO(p(PeripheryUARTKey).head))
  p(UARTOverlayKey).head.place(UARTDesignInput(uartBundleBridgeSrcNode))

  private val i2cBundleBridgeSrcNode = BundleBridgeSource(() => new I2CPort())
  p(I2COverlayKey).head.place(I2CDesignInput(i2cBundleBridgeSrcNode))

  override lazy val module = new LazyRawModuleImp(this) {
    val clockBundle = coreClock.in.head._1
    childClock := clockBundle.clock
    childReset := clockBundle.reset | rocket.module.debug.map(_.ndreset).getOrElse(false.B)

    uartBundleBridgeSrcNode.bundle <> rocket.module.uart.head
    i2cBundleBridgeSrcNode.bundle <> rocket.module.i2c.head

    val systemJtag = rocket.module.debug.head.systemjtag.head
    systemJtag.jtag.TCK := jtag.TCK
    systemJtag.jtag.TDI := jtag.TDI
    jtag.TDO.data := systemJtag.jtag.TDO.data
    jtag.TDO.driven := 1.B
    systemJtag.jtag.TMS := jtag.TMS
    systemJtag.reset := PowerOnResetFPGAOnly(childClock).asAsyncReset
    systemJtag.mfr_id := 0x233.U(11.W)
    systemJtag.part_number := 0xde.U(12.W)
    systemJtag.version := 0x1.U(4.W)
    Debug.connectDebugClockAndReset(rocket.module.debug, childClock)
    rocket.module.resetctrl.foreach { rc =>
      rc.hartIsInReset.foreach { _ := childReset.asBool }
    }

    withClockAndReset(childClock, childReset) {
      led1 := rocket.module.gpio.head.asInstanceOf[GPIOPortIO].pins(0).o.oval
      led2 := rocket.module.gpio.head.asInstanceOf[GPIOPortIO].pins(1).o.oval
      led3 := rocket.module.gpio.head.asInstanceOf[GPIOPortIO].pins(2).o.oval
      led4 := ddr.placedAuxIO.MIGCalibComplete
    }

  }
  coreClock := resetWrangler := ClockGroup() := pll := fpgaClockSource
}
