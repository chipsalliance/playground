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
  /** top IO */
  val topMem: AXI4Bundle = top.outer.mem_axi4.head
  val topUART: UARTPortIO = top.uart.head.asInstanceOf[UARTPortIO]
  val topInterrupts: UInt = top.interrupts
  val topSPI: SPIPortIO = top.qspi.head.asInstanceOf[SPIPortIO]
  val topJtag: SystemJTAGIO = top.debug.head.systemjtag.head

  val fpgaMem = IO(topMem.cloneType)
  fpgaMem <> topMem
  val fpgaUART = IO(topUART.cloneType)
  fpgaUART <> topUART
  val fpgaInterrupts = IO(Input(topInterrupts.cloneType))
  fpgaInterrupts <> topInterrupts

  /** SPI */
  def spiPin(b: SPIDataIO) = {
    val pad = Module(new IOBUF())
    pad.io.I := b.o
    b.i := pad.io.O
    pad.io.T := ~b.oe
    pad.io.IO
  }

  val fpgaSPI = IO(new Bundle {
    val sck = Output(Bool())
    val cs = Output(Bool())
    val dq = Vec(4, Analog(1.W))
  })

  fpgaSPI.sck := topSPI.sck
  fpgaSPI.cs := topSPI.cs(0)
  (fpgaSPI.dq zip topSPI.dq).foreach { case (a, b) => attach(a, spiPin(b)) }

  /** JTAG*/
  val fpgaJtag = IO(new Bundle() {
    val tck = Input(Clock())
    val tms = Input(Bool())
    val tdi = Input(Bool())
    val tdoData = Output(Bool())
    val tdoDriven = Output(Bool())
  })
  topJtag.reset := reset
  topJtag.mfr_id := 0x489.U
  topJtag.part_number := 0.U
  topJtag.version := 2.U
  topJtag.jtag.TCK := fpgaJtag.tck
  topJtag.jtag.TMS := fpgaJtag.tms
  topJtag.jtag.TDI := fpgaJtag.tdi
  fpgaJtag.tdoData := topJtag.jtag.TDO.data
  fpgaJtag.tdoDriven := topJtag.jtag.TDO.driven
}