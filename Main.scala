import Chisel._
import TidbitsTestbenches._
import TidbitsOCM._
import TidbitsStreams._
import TidbitsSimUtils._
import TidbitsAXI._
import TidbitsDMA._

object MainObj {
  val testOutputDir = "testOutput/"
  val verilogOutputDir = "verilogOutput/"
  def makeTestArgs(cmpName: String): Array[String] = {
    return Array( "--targetDir", testOutputDir+cmpName,
                  "--compile", "--test", "--genHarness")
  }

  def makeVerilogBuildArgs(cmpName: String): Array[String] = {
    return Array( "--targetDir", verilogOutputDir+cmpName, "--v")
  }

  def main(args: Array[String]): Unit = {
    //runTest_OCMAndController()
    //runTest_HazardGuard()
    //runTest_AXIStreamUpsizer()
    //runTest_HLM_Simple()
    //runTest_ReadReqGen()
    //runTest_StreamDelta()
    //runTest_StreamRepeatElem()
    //runTest_WrapperTestSum()
    //runTest_WrapperTestSeqRead()
    //runTest_WrapperTestSeqWrite()
    //runVerilog_WrapperTestSum()
    //runVerilog_WrapperTestMultiChanSum()
    //runVerilog_WrapperTestSeqWrite()
    //runVerilog_WrapperTestOCMController()
  }

  def runVerilog_WrapperTestOCMController() {
    val p = new AXIAccelWrapperParams(32,32,64,6,16,1)
    val fxn = { () => new WrapperTestOCMController(p)}
    val instModule = {() => Module(new AXIAccelWrapper(fxn))}
    def aV = makeVerilogBuildArgs("WrapperTest")

    chiselMain(aV, instModule)
  }

  def runVerilog_WrapperTestSeqWrite() {
    val p = new AXIAccelWrapperParams(32,32,64,6,8,1)
    val fxn = { () => new WrapperTestSeqWrite(p)}
    val instModule = {() => Module(new AXIAccelWrapper(fxn))}
    def aV = makeVerilogBuildArgs("WrapperTest")

    chiselMain(aV, instModule)
  }

  def runVerilog_WrapperTestSum() {
    val p = new AXIAccelWrapperParams(32,32,64,6,16,1)
    val fxn = { () => new WrapperTestSum(p)}
    val instModule = {() => Module(new AXIAccelWrapper(fxn))}
    def aV = makeVerilogBuildArgs("WrapperTest")

    chiselMain(aV, instModule)
  }

  def runTest_ReadReqGen() {
    val instModule = {() => Module(new TestReadReqGenWrapper())}
    val instTest = {c => new TestReadReqGen(c)}
    val aT = makeTestArgs("ReadReqGen")
    chiselMainTest(aT, instModule)(instTest)
  }

  def runTest_WrapperTestSum() {
    val p = new AXIAccelWrapperParams(32,32,64,6,16,1)
    val fxn = { () => new WrapperTestSum(p)}
    val instModule = {() => Module(new WrappableAccelHarness(fxn, 1024))}
    val instTest = {c => new WrappableAccelTester(c)}
    val aT = makeTestArgs("WrapperTestSum")
    chiselMainTest(aT, instModule)(instTest)
  }

  def runTest_WrapperTestSeqRead() {
    val p = new AXIAccelWrapperParams(32,32,64,6,16,1)
    val fxn = { () => new WrapperTestSeqRead(p)}
    val instModule = {() => Module(new WrappableAccelHarness(fxn, 1024))}
    val instTest = {c => new WrappableAccelTester(c)}
    val aT = makeTestArgs("WrapperTestSeqRead")
    chiselMainTest(aT, instModule)(instTest)
  }

  def runTest_WrapperTestSeqWrite() {
    val p = new AXIAccelWrapperParams(32,32,64,6,16,1)
    val fxn = { () => new WrapperTestSeqWrite(p)}
    val instModule = {() => Module(new WrappableAccelHarness(fxn, 1024))}
    val instTest = {c => new WrappableAccelTester(c)}
    val aT = makeTestArgs("WrapperTestSeqWrite")
    chiselMainTest(aT, instModule)(instTest)
  }

  def runVerilog_WrapperTestMultiChanSum() {
    val p = new AXIAccelWrapperParams(32,32,64,6,16,1)
    val fxn = { () => new WrapperTestMultiChanSum(3,p)}
    val instModule = {() => Module(new AXIAccelWrapper(fxn))}
    def aV = makeVerilogBuildArgs("WrapperTest")

    chiselMain(aV, instModule)
  }

  def runTest_StreamRepeatElem() {
    val instModule = {() => Module(new StreamRepeatElem(32,32))}
    val instTest = {c => new StreamRepeatElemTester(c)}
    val aT = makeTestArgs("StreamRepeatElem")
    chiselMainTest(aT, instModule)(instTest)
  }

  def runTest_StreamDelta() {
    val instModule = {() => Module(new StreamDeltaTestBed())}
    val instTest = {c => new StreamDeltaTester(c)}
    val aT = makeTestArgs("StreamDelta")
    chiselMainTest(aT, instModule)(instTest)
  }

  def runTest_HLM_Simple() {
    val instModule = {() => Module(new SimpleHLMHarness())}
    val instTest = {c => new HLMSimpleTester(c)}
    val aT = makeTestArgs("HLMSimple")
    def aV = makeVerilogBuildArgs("HLMSimple")

    chiselMain(aV, instModule)
    chiselMainTest(aT, instModule)(instTest)
  }

  def runTest_HazardGuard() {
    val p = new HazardGuardTestParams(4, 2, 2, 16, 32)

    val instModule = {() => Module(new HazardGuardTestHarness(p))}
    val instTest = {c => new HazardGuardTestHarnessTester(c)}
    val aT = makeTestArgs("HazardGuard")
    def aV = makeVerilogBuildArgs("HazardGuard")

    chiselMain(aV, instModule)
    chiselMainTest(aT, instModule)(instTest)
  }

  def runTest_AXIStreamUpsizer() {
    val moduleName: String = "AXIStreamUpsizer"
    val instModule = {() => Module(new  AXIStreamUpsizer(8,32) )}
    val instTest = {c => new AXIStreamUpsizerTester(c)}
    val aT = makeTestArgs(moduleName)
    def aV = makeVerilogBuildArgs(moduleName)

    chiselMain(aV, instModule)
    chiselMainTest(aT, instModule)(instTest)
  }

  def runTest_OCMAndController() {
    val args = makeTestArgs("OCMAndController")
    val p = new OCMParameters(1024, 32, 1, 2, 3)
    p.printParams()
    chiselMainTest(args, () => Module(new OCMFillDump(p))) { c => new TestOCMAndController(c)}
  }
}
