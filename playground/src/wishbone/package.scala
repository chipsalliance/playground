import freechips.rocketchip.diplomacy._

package object wishbone {
  type WishboneOutwardNode = OutwardNodeHandle[
    WishboneMasterPortParameters,
    WishboneSlavePortParameters,
    WishboneEdgeParameters,
    WishboneBundle]
  type WishboneInwardNode = InwardNodeHandle[
    WishboneMasterPortParameters,
    WishboneSlavePortParameters,
    WishboneEdgeParameters,
    WishboneBundle]
  type WishboneNode = SimpleNodeHandle[
    WishboneMasterPortParameters,
    WishboneSlavePortParameters,
    WishboneEdgeParameters,
    WishboneBundle]
}
