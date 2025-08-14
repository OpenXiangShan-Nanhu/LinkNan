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

import org.chipsalliance.cde.config.Parameters
import chisel3._
import chisel3.util._
import freechips.rocketchip.amba.axi4.AXI4SlaveNode
import org.chipsalliance.diplomacy.lazymodule._
import freechips.rocketchip.diplomacy.AddressSet
import difftest.common.DifftestMem

class AXI4RAM
(
  address: Seq[AddressSet],
  memByte: Long,
  useBlackBox: Boolean = false,
  executable: Boolean = true,
  beatBytes: Int = 8,
  burstLen: Int = 16,
)(implicit p: Parameters)
  extends AXI4SlaveModule(address, executable, beatBytes, burstLen)
{
  override lazy val module = new AXI4SlaveModuleImp(this) {

    val offsetBits = log2Up(memByte)

    require(address.length >= 1)
    val baseAddress = address(0).base

    def index(addr: UInt) = ((addr - baseAddress.U)(offsetBits - 1, 0) >> log2Ceil(beatBytes)).asUInt

    def inRange(addr: UInt) = addr < (baseAddress + memByte).U

    val wIdx = index(waddr) + writeBeatCnt
    val rIdx = index(raddr) + readBeatCnt
    val wen = in.w.fire && inRange(waddr)
    require(beatBytes >= 8)

    val rdata = if (useBlackBox) {
      val mem = DifftestMem(memByte, beatBytes)
      when(wen) {
        mem.write(
          addr = wIdx,
          data = in.w.bits.data.asTypeOf(Vec(beatBytes, UInt(8.W))),
          mask = in.w.bits.strb.asBools
        )
      }
      val raddr = Mux(in.r.fire && !rLast, rIdx + 1.U, rIdx)
      mem.readAndHold(raddr, in.ar.fire || in.r.fire).asUInt
    } else {
      val mem = Module(new SparseMem(log2Ceil(memByte / beatBytes), beatBytes * 8, beatBytes))
      mem.io.i_ck := clock
      mem.io.i_ra := rIdx
      mem.io.i_wa := wIdx
      mem.io.i_wd := in.w.bits.data
      mem.io.i_wm := in.w.bits.strb
      mem.io.i_we := wen
      mem.io.o_rd
    }
    in.r.bits.data := rdata
  }
}

class SparseMem(aw:Int, dw:Int, mw:Int) extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val i_ck = Input(Clock())
    val i_ra = Input(UInt(aw.W))
    val i_wa = Input(UInt(aw.W))
    val i_wd = Input(UInt(dw.W))
    val i_wm = Input(UInt(mw.W))
    val i_we = Input(Bool())
    val o_rd = Output(UInt(dw.W))
  })
  setInline("SparseMem.sv",
    s"""// VCS coverage exclude_file
       |module SparseMem (
       |  input  logic          i_ck,
       |  input  logic [${aw - 1}: 0] i_ra,
       |  input  logic [${aw - 1}: 0] i_wa,
       |  input  logic [${dw - 1}: 0] i_wd,
       |  input  logic [${mw - 1}: 0] i_wm,
       |  input  logic         i_we,
       |  output logic [${dw - 1}: 0] o_rd
       |);
       |  bit   [${dw - 1}: 0] mem [bit[${aw - 1}: 0]];
       |  logic [${dw - 1}: 0] mask;
       |
       |  generate
       |    for (genvar i = 0; i < $mw; i++) begin
       |      assign mask[i * 8 +: 8] = {8{i_wm[${mw - 1} - i]}};
       |    end
       |  endgenerate
       |
       |  always_ff @(posedge i_ck) begin
       |    if(i_we) begin
       |      if(mem.exists(i_wa)) begin
       |        mem[i_wa] = (i_wd & mask) | (mem[i_wa] & ~mask);
       |      end else begin
       |        mem[i_wa] = (i_wd & mask) | ({$dw{1'b1}} & ~mask);;
       |      end
       |    end
       |  end
       |
       |  always_comb begin
       |    if(mem.exists(i_ra)) begin
       |      o_rd = mem[i_ra];
       |    end else begin
       |      o_rd = {$dw{1'b1}};
       |    end
       |  end
       |endmodule""".stripMargin)
}

class AXI4RAMWrapper (
  slave: AXI4SlaveNode,
  memByte: Long,
  useBlackBox: Boolean = false
 )(implicit p: Parameters) extends AXI4MemorySlave(slave, memByte, useBlackBox) {
  val ram = LazyModule(new AXI4RAM(
    slaveParam.address, memByte, useBlackBox,
    slaveParam.executable, portParam.beatBytes, burstLen
  ))
  ram.node := master
}
