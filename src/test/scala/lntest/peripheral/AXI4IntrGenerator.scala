/***************************************************************************************
* Copyright (c) 2020-2021 Institute of Computing Technology, Chinese Academy of Sciences
* Copyright (c) 2020-2021 Peng Cheng Laboratory
*
* XiangShan is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

package lntest.peripheral

import chisel3._
import chisel3.util._
import freechips.rocketchip.amba.axi4._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy.{AddressSet, RegionType, TransferSizes}
import org.chipsalliance.diplomacy.lazymodule.{LazyModule, LazyModuleImp}

class IntrGenIO(nrIntr: Int) extends Bundle {
  val intrVec = Output(UInt(nrIntr.W))
}

class AXI4IntrGenerator(
  address: Seq[AddressSet],
  nrIntr: Int
)(implicit p: Parameters) extends LazyModule
{
  val node = AXI4SlaveNode(Seq(AXI4SlavePortParameters(
    Seq(AXI4SlaveParameters(
      address,
      regionType = RegionType.UNCACHED,
      executable = false,
      supportsWrite = TransferSizes(1, 8),
      supportsRead = TransferSizes(1, 8),
      interleavedId = Some(0)
    )),
    beatBytes = 8
  )))
  lazy val module = new Impl

  class Impl extends LazyModuleImp(this) {
    val io = IO(new Bundle {
      val intr = Output(UInt(nrIntr.W))
    })
    private val (in, edge) = node.in.head
    private val axiP = edge.bundle
    private val regBits = in.r.bits.data.getWidth
    private val nrRegs = (nrIntr + regBits  - 1) / regBits
    private val intrRegVec = RegInit(VecInit(Seq.fill(nrRegs)(0.U(regBits.W))))
    io.intr := intrRegVec.asUInt

    private val awq = Module(new Queue(new AXI4BundleAW(axiP), entries = 8))
    private val wq = Module(new Queue(new AXI4BundleW(axiP), entries = 8))
    private val rq = Module(new Queue(new AXI4BundleR(axiP), entries = 2))
    private val bq = Module(new Queue(new AXI4BundleB(axiP), entries = 2))
    awq.io.enq <> in.aw
    wq.io.enq <> in.w
    in.r <> rq.io.deq
    in.b <> bq.io.deq
    when(in.aw.fire) {
      assert(in.aw.bits.cache === "b0000".U)
      assert(in.aw.bits.size === log2Ceil(regBits / 8).U)
      assert(in.aw.bits.len === 0.U)
    }
    when(in.ar.fire) {
      assert(in.ar.bits.cache === "b0000".U)
      assert(in.ar.bits.size === log2Ceil(regBits / 8).U)
      assert(in.ar.bits.len === 0.U)
    }
    when(in.w.fire){
      assert(in.w.bits.last)
    }

    rq.io.enq.valid := in.ar.valid
    rq.io.enq.bits.id := in.ar.bits.id
    if(nrRegs == 1) {
      rq.io.enq.bits.data := intrRegVec.head
    } else {
      rq.io.enq.bits.data := intrRegVec(in.ar.bits.addr(log2Ceil(nrIntr / 8) - 1, log2Ceil(regBits / 8)))
    }
    rq.io.enq.bits.resp := 0.U
    rq.io.enq.bits.user := in.ar.bits.user
    rq.io.enq.bits.echo := in.ar.bits.echo
    rq.io.enq.bits.last := true.B
    in.ar.ready := rq.io.enq.ready

    bq.io.enq.valid := awq.io.deq.valid & wq.io.deq.valid
    bq.io.enq.bits.id := awq.io.deq.bits.id
    bq.io.enq.bits.resp := 0.U
    bq.io.enq.bits.user := awq.io.deq.bits.user
    bq.io.enq.bits.echo := awq.io.deq.bits.echo

    awq.io.deq.ready := bq.io.enq.ready & wq.io.deq.valid
    wq.io.deq.ready := bq.io.enq.ready & awq.io.deq.valid

    private val waddr = awq.io.deq.bits.addr(log2Ceil(nrIntr / 8) - 1, log2Ceil(regBits / 8))
    private val wdata = wq.io.deq.bits.data
    private val wmask = VecInit(Seq.tabulate(regBits / 8)(i => Fill(8, wq.io.deq.bits.strb(i)))).asUInt
    private val wen = bq.io.enq.valid
    for(i <- 0 until nrRegs) {
      when(wen & waddr === i.U) {
        intrRegVec(i) := (wmask & wdata) | ((~wmask).asUInt & intrRegVec(i))
      }
    }
  }
}
