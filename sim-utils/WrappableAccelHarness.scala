package TidbitsSimUtils

import Chisel._
import TidbitsAXI._
import TidbitsRegFile._

// testing infrastructure for wrappable accelerators
// provides "main memory" simulation and a convenient way of setting up the
// control/status registers for setting up the accelerator --
// just like how a CPU would in an SoC-like setting

// TODO how should the implementation here be partitioned?

class WrappableAccelHarness(
  val p: AXIAccelWrapperParams,
  fxn: AXIAccelWrapperParams => AXIWrappableAccel) extends Module {
  val rfAddrBits = log2Up(p.numRegs)
  val io = new Bundle {
    val regFileIF = new RegFileSlaveIF(rfAddrBits, p.csrDataWidth)
  }
  val accel = Module(fxn(p))

  // instantiate regfile
  val regFile = Module(new RegFile(p.numRegs, rfAddrBits, p.csrDataWidth)).io

  // connect regfile to accel ports
  for(i <- 0 until p.numRegs) {
    regFile.regIn(i) <> accel.io.regOut(i)
    accel.io.regIn(i) := regFile.regOut(i)
  }
  // expose regfile interface
  io.regFileIF <> regFile.extIF

  // TODO add memory simulation support
}

class WrappableAccelTester(c: WrappableAccelHarness) extends Tester(c) {
  // TODO add functions for initializing memory
  val regFile = c.io.regFileIF
  def nameToRegInd(regName: String): Int = {
    return c.accel.regMap(regName).toInt
  }

  def readReg(regName: String): BigInt = {
    val ind = nameToRegInd(regName)
    poke(regFile.cmd.bits.regID, ind)
    poke(regFile.cmd.bits.read, 1)
    poke(regFile.cmd.bits.write, 0)
    poke(regFile.cmd.bits.writeData, 0)
    poke(regFile.cmd.valid, 1)
    step(1)
    poke(regFile.cmd.valid, 0)
    return peek(regFile.readData.bits)
  }

  def expectReg(regName: String, value: BigInt): Boolean = {
    return expect(readReg(regName)==value, regName)
  }

  def writeReg(regName: String, value: BigInt) = {
    val ind = nameToRegInd(regName)
    poke(regFile.cmd.bits.regID, ind)
    poke(regFile.cmd.bits.read, 0)
    poke(regFile.cmd.bits.write, 1)
    poke(regFile.cmd.bits.writeData, value)
    poke(regFile.cmd.valid, 1)
    step(1)
    poke(regFile.cmd.valid, 0)
  }

  // let the accelerator do internal init (such as writing to the regfile)
  step(10)
  // launch the default test, as defined by the accelerator
  c.accel.defaultTest(this)
}
