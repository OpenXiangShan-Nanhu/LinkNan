package linknan.soc

import chisel3._
import chisel3.util._
import linknan.generator.AddrConfig
import org.chipsalliance.cde.config.Parameters
import zhujiang.ZJModule
import zhujiang.axi.{AxiBufferChain, AxiBundle, AxiParams, AxiWidthAdapter}

class MstAxiFabric(cfgP:AxiParams, datP:Option[AxiParams])(implicit p: Parameters) extends ZJModule {
  val s_axi = IO(new Bundle {
    val cfg = Flipped(new AxiBundle(cfgP))
    val uc = datP.map(d => Flipped(new AxiBundle(d)))
  })
  private def ucMatcher(addr:UInt, uc:Boolean):Bool = {
    val ucSetHitVec = AddrConfig.mem_nc.map(m => (m._2.U & addr) === m._1.U)
    val hit = Cat(ucSetHitVec).orR
    if(uc) hit else !hit
  }
  private val cfgXbar = datP.map(d => {
    val xbar = Module(new Axi1toNXBar(cfgP, Seq(ucMatcher(_, uc = true), ucMatcher(_, uc = false))))
    xbar.io.upstream.head <> s_axi.cfg
    xbar
  })

  private val cfg2mem = datP.map(d => {
    val adpt = Module(new AxiWidthAdapter(cfgP.copy(dataBits = d.dataBits), cfgP, 1 << cfgP.idBits))
    adpt.io.mst <> cfgXbar.get.io.downstream.head
    adpt
  })

  private val datXbar = datP.map(d => {
    val xbar = Module(new AxiNto1XBar(Seq(cfg2mem.get.io.slv.params, d)))
    xbar.io.upstream.head <> cfg2mem.get.io.slv
    xbar.io.upstream.last <> s_axi.uc.get
    xbar
  })

  private val ucBufChain = datXbar.map(xbar => {
    val buf = Module(new AxiBufferChain(xbar.io.downstream.head.params, 3))
    buf.io.in <> xbar.io.downstream.head
    buf
  })

  private val cfgOut = cfgXbar.map(_.io.downstream.last).getOrElse(s_axi.cfg)
  private val cfgBufChain = Module(new AxiBufferChain(cfgOut.params, 4))
  cfgBufChain.io.in <> cfgOut

  val m_axi = IO(new Bundle {
    val cfg = new AxiBundle(cfgBufChain.io.out.params.copy(attr = "cfg"))
    val uc = ucBufChain.map(b => new AxiBundle(b.io.out.params.copy(attr = "hs")))
  })
  m_axi.cfg <> cfgBufChain.io.out
  m_axi.uc.foreach(_ <> ucBufChain.get.io.out)

  dontTouch(s_axi)
  dontTouch(m_axi)
}
