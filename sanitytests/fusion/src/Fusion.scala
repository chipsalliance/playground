package fusion

object Fusion extends TestSuite {
  val outputDirectory = os.pwd / "out" / "Fusion"
  os.remove.all(outputDirectory)
  os.makeDir(outputDirectory)
  val tests = Tests {
    test("build SynthesisHarness") {
      val synthesisHarness = classOf[freechips.rocketchip.system.ExampleRocketSystem]
      val configs = Seq(classOf[SynthesisConfig], classOf[freechips.rocketchip.system.DefaultConfig])
      val emulator = SynthesisHarness(synthesisHarness, configs, Some(outputDirectory)).emulator
    }
  }
}
