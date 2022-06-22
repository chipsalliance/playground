package sanitytests.fusion

import chisel3._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.devices.debug.Debug
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.system.ExampleRocketSystem
import freechips.rocketchip.util.AsyncResetReg

class RocketSynthesisModule()(implicit p: Parameters) extends Module {
  val ldut = LazyModule(new ExampleRocketSystem)
  val dut = Module(ldut.module)

  // Allow the debug ndreset to reset the dut, but not until the initial reset has completed
  dut.reset := (reset.asBool | dut.debug.map { debug => AsyncResetReg(debug.ndreset) }.getOrElse(false.B)).asBool

  dut.dontTouchPorts()
  dut.tieOffInterrupts()

  ldut.mem_axi4.foreach(_.tieoff())
  ldut.mmio_axi4.foreach(_.tieoff())
  ldut.l2_frontend_bus_axi4.foreach(_.tieoff())
  Debug.tieoffDebug(dut.debug, dut.resetctrl)
  dut.debug.foreach(io => io.dmactiveAck := false.B)
}
