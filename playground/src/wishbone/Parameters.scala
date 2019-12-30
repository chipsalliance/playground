package wishbone

import chisel3._
import chisel3.util._
import chisel3.internal.sourceinfo.SourceInfo
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import scala.math.max

case class WishboneSlaveParameters(address: Seq[AddressSet],
                                   resources: Seq[Resource] = Nil,
                                   regionType: RegionType.T = RegionType.GET_EFFECTS,
                                   executable: Boolean = false,
                                   nodePath: Seq[BaseNode] = Seq(),
                                   supportsWrite: Boolean = true,
                                   supportsRead: Boolean = true,
                                   device: Option[Device] = None) {
  address.foreach { a => require(a.finite) }
  address.combinations(2).foreach { case Seq(x, y) => require(!x.overlaps(y)) }

  val name: String = nodePath.lastOption.map(_.lazyModule.name).getOrElse("disconnected")
  val maxAddress: BigInt = address.map(_.max).max
  val minAlignment: BigInt = address.map(_.alignment).min

  def toResource: ResourceAddress = ResourceAddress(
    address,
    ResourcePermissions(
      r = supportsRead,
      w = supportsWrite,
      x = executable,
      c = false,
      a = false
    )
  )
}

case class WishboneSlavePortParameters(slaves: Seq[WishboneSlaveParameters],
                                       beatBytes: Int) {
  require(slaves.nonEmpty)
  require(isPow2(beatBytes))

  val maxAddress: BigInt = slaves.map(_.maxAddress).max

  // Require disjoint ranges for addresses
  slaves.combinations(2).foreach { case Seq(x, y) =>
    x.address.foreach { a =>
      y.address.foreach { b =>
        require(!a.overlaps(b))
      }
    }
  }
}

case class WishboneMasterParameters(name: String,
                                    nodePath: Seq[BaseNode] = Seq(),
                                    userBits: Seq[UserBits] = Nil) {
  val userBitsWidth: Int = userBits.map(_.width).sum
}

case class WishboneMasterPortParameters(masters: Seq[WishboneMasterParameters]) {
  val userBitsWidth: Int = masters.map(_.userBitsWidth).max
}

case class WishboneBundleParameters(addrBits: Int,
                                    dataBits: Int,
                                    selectBits: Int,
                                    dataTagBits: Int = 0,
                                    addressTagBits: Int = 0,
                                    clockTagBits: Int = 0,
                                   ) {
  require(dataBits <= 64)
  require(addrBits >= 1)
  require(isPow2(dataBits))
  require(dataBits % selectBits == 0)

  def union(x: WishboneBundleParameters): WishboneBundleParameters = {
    WishboneBundleParameters(
      max(addrBits, x.addrBits),
      max(dataBits, x.dataBits),
      if (addrBits >= x.addrBits) selectBits else x.selectBits,
      max(dataTagBits, x.dataTagBits),
    )
  }
}

object WishboneBundleParameters {
  val emptyBundleParams: WishboneBundleParameters = WishboneBundleParameters(
    addrBits = 1,
    dataBits = 8,
    selectBits = 1)

  def union(x: Seq[WishboneBundleParameters]): WishboneBundleParameters = x.foldLeft(emptyBundleParams)((x, y) => x.union(y))

  def apply(master: WishboneMasterPortParameters, slave: WishboneSlavePortParameters) =
    new WishboneBundleParameters(
      addrBits = log2Up(slave.maxAddress + 1),
      dataBits = slave.beatBytes * 8,
      selectBits = slave.beatBytes
    )
}

case class WishboneEdgeParameters(master: WishboneMasterPortParameters,
                                  slave: WishboneSlavePortParameters,
                                  params: Parameters,
                                  sourceInfo: SourceInfo) {
  val bundle: WishboneBundleParameters = WishboneBundleParameters(master, slave)
}