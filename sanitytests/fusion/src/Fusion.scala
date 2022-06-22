package sanitytests.fusion

import os._
import utest._

object Fusion extends TestSuite {
  val outputDirectory: Path = os.pwd / "out" / "Fusion"
  os.remove.all(outputDirectory)
  os.makeDir(outputDirectory)
  val tests = Tests {
    test("build SynthesisHarness") {
      val synthesisHarness = classOf[RocketSynthesisModule]
      val configs = Seq(classOf[SynthesisConfig], classOf[freechips.rocketchip.system.DefaultConfig])
      val output = SynthesisHarness(synthesisHarness, configs, Some(outputDirectory)).output
    }
  }
}
