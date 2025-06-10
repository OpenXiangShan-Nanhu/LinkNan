package linknan.soc

import chisel3._
import org.chipsalliance.cde.config.Parameters
import zhujiang.ZJModule
import zhujiang.axi.{AxiBuffer, AxiBundle, AxiParams, AxiWidthAdapter}

class PCIeAxiBridge(cfgP:AxiParams, datP:AxiParams)(implicit p: Parameters) extends ZJModule {
  private val cfg2mem = Module(new AxiWidthAdapter(cfgP.copy(dataBits = datP.dataBits), cfgP, 1 << cfgP.idBits))
  private val xbar = Module(new AxiNto1XBar(Seq(cfg2mem.io.slv.params, datP)))

  val io = IO(new Bundle {
    val cfg = Flipped(new AxiBundle(cfgP))
    val dat = Flipped(new AxiBundle(datP))
    val out = new AxiBundle(xbar.io.downstream.head.params.copy(attr = "pcie"))
  })
  cfg2mem.io.mst <> io.cfg
  xbar.io.upstream(0) <> cfg2mem.io.slv
  xbar.io.upstream(1) <> io.dat
  io.out <> AxiBuffer.chain(xbar.io.downstream.head, 2)
}
