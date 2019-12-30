package wishbone

import chisel3.internal.sourceinfo.SourceInfo
import freechips.rocketchip.config.{Parameters, Field}
import freechips.rocketchip.diplomacy._

object WishboneImp extends SimpleNodeImp[WishboneMasterPortParameters, WishboneSlavePortParameters, WishboneEdgeParameters, WishboneBundle] {
  def edge(pd: WishboneMasterPortParameters,
           pu: WishboneSlavePortParameters,
           p: Parameters,
           sourceInfo: SourceInfo): WishboneEdgeParameters = WishboneEdgeParameters(pd, pu, p, sourceInfo)

  def bundle(e: WishboneEdgeParameters): WishboneBundle = WishboneBundle(e.bundle)

  def render(e: WishboneEdgeParameters): RenderedEdge = RenderedEdge(colour = "#00ccff" /* bluish */ , (e.slave.beatBytes * 8).toString)

  override def mixO(pd: WishboneMasterPortParameters,
                    node: OutwardNode[
                      WishboneMasterPortParameters,
                      WishboneSlavePortParameters,
                      WishboneBundle]
                   ): WishboneMasterPortParameters = pd.copy(masters = pd.masters.map { c => c.copy(nodePath = node +: c.nodePath) })

  override def mixI(pu: WishboneSlavePortParameters,
                    node: InwardNode[
                      WishboneMasterPortParameters,
                      WishboneSlavePortParameters,
                      WishboneBundle]
                   ): WishboneSlavePortParameters = pu.copy(slaves = pu.slaves.map { m => m.copy(nodePath = node +: m.nodePath) })
}

case class WishboneMasterNode(portParams: Seq[WishboneMasterPortParameters])(implicit valName: ValName) extends SourceNode(WishboneImp)(portParams)

case class WishboneSlaveNode(portParams: Seq[WishboneSlavePortParameters])(implicit valName: ValName) extends SinkNode(WishboneImp)(portParams)

case class WishboneNexusNode(masterFn: Seq[WishboneMasterPortParameters] => WishboneMasterPortParameters,
                             slaveFn: Seq[WishboneSlavePortParameters] => WishboneSlavePortParameters)(implicit valName: ValName)
  extends NexusNode(WishboneImp)(masterFn, slaveFn)

case class WishboneIdentityNode()(implicit valName: ValName) extends IdentityNode(WishboneImp)()
