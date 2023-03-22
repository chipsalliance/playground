package playground

import org.chipsalliance.cde.config.Config
class TestConfig
    extends Config((site, here, up) => {
      case freechips.rocketchip.util.ClockGateModelFile => Some("./dependencies/rocket-chip/src/vsrc/EICG_wrapper.v")
      case freechips.rocketchip.devices.tilelink.BootROMLocated(x) =>
        up(freechips.rocketchip.devices.tilelink.BootROMLocated(x), site)
          .map(_.copy(contentFileName = "./dependencies/rocket-chip/bootrom/bootrom.img"))
    })

class PlaygroundConfig
    extends Config(
      (new TestConfig)
        .orElse(new freechips.rocketchip.subsystem.WithInclusiveCache)
        .orElse(new freechips.rocketchip.system.DefaultConfig)
    )
