package linknan.soc

import chisel3._
import chisel3.util._
import linknan.cluster.hub.peripheral.AclintAddrRemapper
import org.chipsalliance.cde.config.Parameters
import zhujiang.ZJModule
import zhujiang.axi.{AxiBuffer, AxiBufferChain, AxiBundle, AxiParams, AxiWidthAdapter}

class SlvAxiFabric(dmaP:AxiParams)(implicit p: Parameters) extends ZJModule {
  private val sbaP = dmaP.copy(attr = "sba", idBits = dmaP.idBits - 2)
  private val cfgP = dmaP.copy(attr = "cfg", idBits = dmaP.idBits - 2, dataBits = 64)
  private val datP = dmaP.copy(attr = "hs",  idBits = dmaP.idBits - 2)

  val s_axi = IO(new Bundle {
    val sba = Flipped(new AxiBundle(sbaP))
    val cfg = Flipped(new AxiBundle(cfgP))
    val dat = Flipped(new AxiBundle(datP))
  })
  val m_axi = IO(new Bundle {
    val dma = new AxiBundle(dmaP)
  })
  private val cfgBuf = Module(new AxiBufferChain(cfgP, 4))
  private val datBuf = Module(new AxiBufferChain(datP, 4))
  private val adpt = Module(new AxiWidthAdapter(cfgP.copy(dataBits = dmaP.dataBits), cfgP, 8))
  private val xbar = Module(new AxiNto1XBar(Seq(sbaP, adpt.io.slv.params, datP)))

  cfgBuf.io.in <> s_axi.cfg
  adpt.io.mst <> cfgBuf.io.out
  datBuf.io.in <> s_axi.dat
  xbar.io.upstream(0) <> s_axi.sba
  xbar.io.upstream(1) <> adpt.io.slv
  xbar.io.upstream(2) <> datBuf.io.out
  m_axi.dma <> xbar.io.downstream.head

  adpt.io.mst.aw.bits.addr := AclintAddrRemapper(cfgBuf.io.out.aw.bits.addr)
  adpt.io.mst.ar.bits.addr := AclintAddrRemapper(cfgBuf.io.out.ar.bits.addr)
  dontTouch(s_axi)
  dontTouch(m_axi)
}
