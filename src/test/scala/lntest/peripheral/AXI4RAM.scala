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
  override lazy val module = new AXI4SlaveModuleImp(this){

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
      when (wen) {
        mem.write(
          addr = wIdx,
          data = in.w.bits.data.asTypeOf(Vec(beatBytes, UInt(8.W))),
          mask = in.w.bits.strb.asBools
        )
      }
      val raddr = Mux(in.r.fire && !rLast, rIdx + 1.U, rIdx)
      mem.readAndHold(raddr, in.ar.fire || in.r.fire).asUInt
    } else {
      val mem = Mem(memByte / beatBytes, Vec(beatBytes, UInt(8.W)))

      val wdata = VecInit.tabulate(beatBytes) { i => in.w.bits.data(8 * (i + 1) - 1, 8 * i) }
      when(wen) {
        mem.write(wIdx, wdata, in.w.bits.strb.asBools)
      }

      Cat(mem.read(rIdx).reverse)
    }
    in.r.bits.data := rdata
  }
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
