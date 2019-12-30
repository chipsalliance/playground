package wishbone

import freechips.rocketchip._
import config._
import diplomacy._
import chisel3._

class WishboneDemoMaster(implicit p: Parameters) extends LazyModule {
  val s = LazyModule(new WishboneDemoSlave)
  val node = WishboneMasterNode(Seq(WishboneMasterPortParameters(Seq(WishboneMasterParameters("test")))))
  s.node := node
  lazy val module: LazyModuleImpLike = new LazyModuleImp(this) {
    val out = node.out.head._1
    val reg0 = RegInit(0.U)
    printf("%d", reg0)
    val counter = RegInit(0.U)
    counter := counter +% 1.U
    when(counter === 1.U) {
      out.writeEnable := false.B
      out.address := 0x55.U
      reg0 := out.dataIn
    }.elsewhen(counter === 2.U) {
      out.writeEnable := false.B
      out.address := 0xFF.U
      reg0 := out.dataIn
    }.elsewhen(counter === 3.U) {
      out.writeEnable := true.B
      out.address := 0x55.U
      out.dataOut := 0x22.U
    }.elsewhen(counter === 4.U) {
      out.writeEnable := true.B
      out.address := 0xFF.U
      out.dataOut := 0x33.U
    }
  }
}

class WishboneDemoSlave(implicit p: Parameters) extends LazyModule {
  val node = WishboneSlaveNode(Seq(WishboneSlavePortParameters(Seq(WishboneSlaveParameters(Seq(AddressSet(0, 0)))), 8)))
  lazy val module: LazyModuleImpLike = new LazyModuleImp(this) {
    val in = node.in.head._1
    val reg0 = RegInit(0xff.U)
    val reg1 = RegInit(0x55.U)
    when(!in.writeEnable) {
      when(in.address === 0x55.U) {
        in.dataIn := reg0
      }.elsewhen(in.address === 0xFF.U) {
        in.dataIn := reg1
      }.otherwise {
        in.dataIn := 0.U
      }
    }.otherwise {
      when(in.address === 0.U) {
        reg0 := in.dataOut
      }.elsewhen(in.address === 0xFF.U) {
        reg1 := in.dataOut
      }
    }
  }
}