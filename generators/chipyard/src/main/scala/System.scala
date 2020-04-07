//******************************************************************************
// Copyright (c) 2019 - 2019, The Regents of the University of California (Regents).
// All Rights Reserved. See LICENSE and LICENSE.SiFive for license details.
//------------------------------------------------------------------------------

package chipyard

import chisel3._

import freechips.rocketchip.config.{Parameters}
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util.{DontTouch}

// ---------------------------------------------------------------------
// Base system that uses the debug test module (dtm) to bringup the core
// ---------------------------------------------------------------------

/**
 * Base top with periphery devices and ports, and a BOOM + Rocket subsystem
 */
class System(implicit p: Parameters) extends Subsystem
  with HasHierarchicalBusTopology
  with HasAsyncExtInterrupts
  with CanHaveMasterAXI4MemPort
  with CanHaveMasterAXI4MMIOPort
  with CanHaveSlaveAXI4Port
  with HasPeripheryBootROM
{
  override lazy val module = new SystemModule(this)
}

/* FIXME: handle removal of these with 
https://github.com/chipsalliance/rocket-chip/commit/68c9b582d8cf5232f1edd105b5cf6a2f29564d71#diff-4f0d79cde64645952116839e7efd5b43L64
*/
trait CanHaveMasterAXI4MemPortModuleImp extends LazyModuleImp {
   val outer: CanHaveMasterAXI4MemPort
   val mem_axi4 = outer.mem_axi4.getWrappedValue
}

trait CanHaveMasterAXI4MMIOPortModuleImp extends LazyModuleImp {
   val outer: CanHaveMasterAXI4MMIOPort
   val mmio_axi4 = outer.mmio_axi4.getWrappedValue
}

trait CanHaveSlaveAXI4PortModuleImp extends LazyModuleImp {
   val outer: CanHaveSlaveAXI4Port
   val l2_frontend_bus_axi4 = outer.l2_frontend_bus_axi4.getWrappedValue
}

/**
 * Base top module implementation with periphery devices and ports, and a BOOM + Rocket subsystem
 */
class SystemModule[+L <: System](_outer: L) extends SubsystemModuleImp(_outer)
  with HasRTCModuleImp
  with HasExtInterruptsModuleImp
  with CanHaveMasterAXI4MemPortModuleImp
  with CanHaveMasterAXI4MMIOPortModuleImp
  with CanHaveSlaveAXI4PortModuleImp
  with HasPeripheryBootROMModuleImp
  with DontTouch
