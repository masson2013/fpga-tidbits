package TidbitsOCM

import Chisel._

// creates a queue either using standard Chisel queues (for smaller queues)
// or with FPGA TDP BRAMs as the storage (for larger queues)

class FPGAQueue[T <: Data](gen: T, val entries: Int) extends Module {
  val thresholdBigQueue = 32 // threshold for deciding big or small queue impl
  val actualEntries = if (entries < thresholdBigQueue) entries else entries + 2
  val io = new QueueIO(gen, actualEntries)
  if(entries < thresholdBigQueue) {
    // create a regular Chisel queue, should be fine to use LUTRAMs as storage
    val theQueue = Module(new Queue(gen, entries)).io
    theQueue <> io
  } else {
    // create a big queue that will use FPGA BRAMs as storage
    // the source code here is mostly copy-pasted from the regular Chisel
    // Queue, but with DualPortBRAM as the data storage
    // some simplifications has been applied, since pipe = false and
    // flow = false (no comb. paths between prod/cons read/valid signals)

    val enq_ptr = Counter(entries)
    val deq_ptr = Counter(entries)
    val maybe_full = Reg(init=Bool(false))

    // due to the 1-cycle read latency of BRAMs, we add a small regular
    // Chisel Queue at the output to correct the interface semantics by
    // "prefetching" the top two elements ("handshaking across latency")
    // TODO support higher BRAM latencies with parametrization here
    val pf = Module(new Queue(gen, 2)).io
    // will be used as the "ready" signal for the prefetch queue
    // the threshold here needs to be (pfQueueCap-BRAM latency)
    val canPrefetch = (pf.count < UInt(1))

    val bram = Module(new DualPortBRAM(log2Up(entries), gen.getWidth())).io
    val writePort = bram.ports(0)
    val readPort = bram.ports(1)
    writePort.req.writeData := io.enq.bits
    writePort.req.writeEn := Bool(false)
    writePort.req.addr := enq_ptr.value

    readPort.req.writeData := UInt(0)
    readPort.req.writeEn := Bool(false)
    readPort.req.addr := deq_ptr.value

    val ptr_match = enq_ptr.value === deq_ptr.value
    val empty = ptr_match && !maybe_full
    val full = ptr_match && maybe_full

    val do_enq = io.enq.ready && io.enq.valid
    val do_deq = canPrefetch && !empty
    when (do_enq) {
      writePort.req.writeEn := Bool(true)
      enq_ptr.inc()
    }
    when (do_deq) {
      deq_ptr.inc()
    }
    when (do_enq != do_deq) {
      maybe_full := do_enq
    }

    io.enq.ready := !full

    pf.enq.valid := Reg(init = Bool(false), next = do_deq)
    pf.enq.bits := readPort.rsp.readData

    pf.deq <> io.deq

    // TODO this count may be off by 1 (elem about to enter the pf queue)
    val ptr_diff = enq_ptr.value - deq_ptr.value
    if (isPow2(entries)) {
      io.count := Cat(maybe_full && ptr_match, ptr_diff) + pf.count
    } else {
      io.count := Mux(ptr_match,
                      Mux(maybe_full,
                        UInt(entries), UInt(0)),
                      Mux(deq_ptr.value > enq_ptr.value,
                        UInt(entries) + ptr_diff, ptr_diff)) + pf.count
    }
  }
}

object FPGAQueue
{
  def apply[T <: Data](enq: DecoupledIO[T], entries: Int = 2): DecoupledIO[T]  = {
    val q = Module(new FPGAQueue(enq.bits.cloneType, entries))
    q.io.enq.valid := enq.valid // not using <> so that override is allowed
    q.io.enq.bits := enq.bits
    enq.ready := q.io.enq.ready
    q.io.deq
  }
}