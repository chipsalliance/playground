package playground

import org.chipsalliance.cde.config.Config
import freechips.rocketchip.devices.tilelink.BootROMLocated
import freechips.rocketchip.util.ClockGateModelFile
import os._
class TestConfig
    extends Config((site, here, up) => {
      case ClockGateModelFile => Some("./dependencies/rocket-chip/src/vsrc/EICG_wrapper.v")
      case BootROMLocated(x) =>
        up(BootROMLocated(x), site).map(_.copy(contentFileName = "./dependencies/rocket-chip/bootrom/bootrom.img"))
    })
