package TidbitsTestbenches

import Chisel._
import TidbitsAXI._
import TidbitsDMA._
import TidbitsStreams._
import TidbitsOCM._

class WrapperTestOCMController(p: AXIAccelWrapperParams) extends AXIWrappableAccel(p) {
  // plug unused ports / set defaults
  plugRegOuts()
  //plugMemWritePorts()
  //plugMemReadPorts()

  val in = new Bundle {
    val ctl = UInt(width = 2)
    val readBase = UInt(width = 32)
    val writeBase = UInt(width = 32)
  }
  val out = new Bundle {
    val status = UInt(width = 32)
    val readReqC = UInt(width = 32)
    val writeReqC = UInt(width = 32)
    val readRspC = UInt(width = 32)
    val writeDatC = UInt(width = 32)
    val writeRspC = UInt(width = 32)
  }
  manageRegIO(in, out)


  val pMR = p.toMRP()
  val pOCM = new OCMParameters(64*1024, 64, 64, 2, 1)
  pOCM.printParams()

  val ocmInst = Module(new OCMAndController(pOCM, "WrapperBRAM64x1024", true)).io
  // plug user ports (unused for now) -- accessed only via controller
  ocmInst.ocmUser(0).req := NullOCMRequest(pOCM)
  ocmInst.ocmUser(1).req := NullOCMRequest(pOCM)

  // read-write request generators for fills and dumps
  val rrq = Module(new ReadReqGen(pMR, 0, 8)).io
  val wrq = Module(new WriteReqGen(pMR, 0)).io
  rrq.reqs <> io.mp(0).memRdReq
  wrq.reqs <> io.mp(0).memWrReq
  io.mp(0).memWrDat <> Queue(ocmInst.mcif.dumpPort, 16)
  val filterFxn = {x: GenericMemoryResponse => x.readData}
  ocmInst.mcif.fillPort <> StreamFilter(io.mp(0).memRdRsp, UInt(width=64), filterFxn)
  // use a reducer to count the write responses
  val redFxn = {(a: UInt, b: UInt) => a+b}
  val reducer = Module(new StreamReducer(64, 0, redFxn)).io
  ocmInst.mcif.fillPort <> StreamFilter(io.mp(0).memWrRsp, UInt(width=64), filterFxn)
  ocmInst.mcif.fillDumpStart := UInt(0)
  ocmInst.mcif.fillDumpCount := UInt(pOCM.bits/64)
  // wire up control
  val byteCount = UInt(pOCM.bits/8)

  ocmInst.mcif.start := in.ctl(0)
  ocmInst.mcif.mode := in.ctl(1)
  val startFill = in.ctl(0) && !in.ctl(1)
  val startDump = in.ctl(0) && in.ctl(1)
  rrq.ctrl.start := startFill
  wrq.ctrl.start := startDump
  reducer.start := startDump
  rrq.ctrl.baseAddr := in.readBase
  rrq.ctrl.byteCount := byteCount
  wrq.ctrl.baseAddr := in.writeBase
  wrq.ctrl.byteCount := byteCount
  reducer.byteCount := byteCount

  rrq.ctrl.throttle := Bool(false)
  wrq.ctrl.throttle := Bool(false)

  // wire up status
  val statusList = List(   rrq.stat.finished, wrq.stat.finished,
                           reducer.finished, ocmInst.mcif.done)
  out.status := Cat(statusList)

  // memory port operation counters
  // TODO make these an optional part of the infrastructure,
  // can be quite useful for debugging
  val regReadReqCount = Reg(init = UInt(0, 32))
  val regWriteReqCount = Reg(init = UInt(0, 32))
  val regWriteDataCount = Reg(init = UInt(0, 32))
  val regReadRspCount = Reg(init = UInt(0, 32))
  val regWriteRspCount = Reg(init = UInt(0, 32))

  when(io.mp(0).memRdReq.valid & io.mp(0).memRdReq.ready) {
    regReadReqCount := regReadReqCount + UInt(1)
  }

  when(io.mp(0).memWrReq.valid & io.mp(0).memWrReq.ready) {
    regWriteReqCount := regWriteReqCount + UInt(1)
  }

  when(io.mp(0).memWrDat.valid & io.mp(0).memWrDat.ready) {
    regWriteDataCount := regWriteDataCount + UInt(1)
  }

  when(io.mp(0).memRdRsp.valid & io.mp(0).memRdRsp.ready) {
    regReadRspCount := regReadRspCount + UInt(1)
  }

  when(io.mp(0).memWrRsp.valid & io.mp(0).memWrRsp.ready) {
    regWriteRspCount := regWriteRspCount + UInt(1)
  }

  out.readReqC := regReadReqCount
  out.readRspC := regReadRspCount
  out.writeReqC := regWriteReqCount
  out.writeRspC := regWriteRspCount
  out.writeDatC := regWriteDataCount

  // TODO write test
}
