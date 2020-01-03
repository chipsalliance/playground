package arty100t

import chisel3._
import freechips.rocketchip._
import amba.axi4.AXI4Bundle
import chisel3.experimental._
import diplomacy._
import devices.debug.SystemJTAGIO
import sifive.blocks.devices._
import uart._
import spi._
import playground._
import sifive.fpgashells.ip.xilinx._


class FPGATop extends MultiIOModule {
  val top = Module(LazyModule(configToRocketModule(classOf[CustomArty100TRocketSystem], new CustomArty100TConfig)).module)
  val topInterrupts: UInt = top.interrupts
  val fpgaInterrupts = IO(Input(topInterrupts.cloneType))
  fpgaInterrupts <> topInterrupts

  val topMem: AXI4Bundle = top.outer.mem_axi4.head
  val fpgaMem = IO(topMem.cloneType)
  fpgaMem <> topMem

  val topUART: UARTPortIO = top.uart.head.asInstanceOf[UARTPortIO]
  val fpgaUART = IO(topUART.cloneType)
  fpgaUART <> topUART

  val topSPI: SPIPortIO = top.qspi.head.asInstanceOf[SPIPortIO]
  val fpgaSPI = IO(topSPI.cloneType)
  fpgaSPI <> topSPI
  val topJtag: SystemJTAGIO = top.debug.head.systemjtag.head
  val fpgaJtag = IO(new Bundle() {
    val tck = Input(Clock())
    val tms = Input(Bool())
    val tdi = Input(Bool())
    val tdo = Output(Bool())
  })
  topJtag.reset := reset
  topJtag.mfr_id := 0x489.U(11.W)
  topJtag.part_number := 0.U(16.W)
  topJtag.version := 2.U(4.W)
  topJtag.jtag.TCK := fpgaJtag.tck
  topJtag.jtag.TMS := fpgaJtag.tms
  topJtag.jtag.TDI := fpgaJtag.tdi

  fpgaJtag.tdo := topJtag.jtag.TDO.data
}