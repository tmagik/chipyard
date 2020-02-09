package firesim.firesim

import java.io.File

import chisel3.util.{log2Up}
import freechips.rocketchip.config.{Parameters, Config}
import freechips.rocketchip.groundtest.TraceGenParams
import freechips.rocketchip.tile._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.rocket.DCacheParams
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.tilelink.BootROMParams
import freechips.rocketchip.devices.debug.DebugModuleParams
import freechips.rocketchip.diplomacy.{RationalCrossing}
import boom.common.{BoomCrossingKey, BoomTilesKey}
import testchipip.{BlockDeviceKey, BlockDeviceConfig}
import sifive.blocks.devices.uart.{PeripheryUARTKey, UARTParams}
import scala.math.{min, max}
import tracegen.TraceGenKey
import icenet._

import firesim.bridges._
import firesim.util.{WithNumNodes, FireSimClockKey, FireSimClockParameters}
import firesim.configs._

class WithBootROM extends Config((site, here, up) => {
  case BootROMParams => {
    val chipyardBootROM = new File(s"./generators/testchipip/bootrom/bootrom.rv${site(XLen)}.img")
    val firesimBootROM = new File(s"./target-rtl/chipyard/generators/testchipip/bootrom/bootrom.rv${site(XLen)}.img")

     val bootROMPath = if (chipyardBootROM.exists()) {
      chipyardBootROM.getAbsolutePath()
    } else {
      firesimBootROM.getAbsolutePath()
    }
    BootROMParams(contentFileName = bootROMPath)
  }
})

class WithPeripheryBusFrequency(freq: BigInt) extends Config((site, here, up) => {
  case PeripheryBusKey => up(PeripheryBusKey).copy(frequency=freq)
})

class WithUARTKey extends Config((site, here, up) => {
  case PeripheryUARTKey => List(UARTParams(
     address = BigInt(0x54000000L),
     nTxEntries = 256,
     nRxEntries = 256))
})

class WithBlockDevice extends Config(new testchipip.WithBlockDevice)

class WithNICKey extends Config((site, here, up) => {
  case NICKey => NICConfig(
    inBufFlits = 8192,
    ctrlQueueDepth = 64)
})

class WithRocketL2TLBs(entries: Int) extends Config((site, here, up) => {
  case RocketTilesKey => up(RocketTilesKey) map (tile => tile.copy(
    core = tile.core.copy(
      nL2TLBEntries = entries
    )
  ))
})

class WithPerfCounters extends Config((site, here, up) => {
  case RocketTilesKey => up(RocketTilesKey) map (tile => tile.copy(
    core = tile.core.copy(nPerfCounters = 29)
  ))
})

class WithBoomL2TLBs(entries: Int) extends Config((site, here, up) => {
  case BoomTilesKey => up(BoomTilesKey) map (tile => tile.copy(
    core = tile.core.copy(nL2TLBEntries = entries)
  ))
})

class WithBoomEnableTrace extends Config((site, here, up) => {
  case BoomTilesKey => up(BoomTilesKey) map (tile => tile.copy(trace = true))
})

// Disables clock-gating; doesn't play nice with our FAME-1 pass
class WithoutClockGating extends Config((site, here, up) => {
  case DebugModuleParams => up(DebugModuleParams, site).copy(clockGate = false)
})

// Testing configurations
// This enables printfs used in testing
class WithScalaTestFeatures extends Config((site, here, up) => {
    case PrintTracePort => true
})

// FASED Config Aliases. This to enable config generation via "_" concatenation
// which requires that all config classes be defined in the same package
class DDR3FRFCFS extends FRFCFS16GBQuadRank
class DDR3FRFCFSDiv2 extends FRFCFS16GBQuadRank(clockDiv=2)
class DDR3FRFCFSLLC4MB extends FRFCFS16GBQuadRankLLC4MB

// L2 Config Aliases. For use with "_" concatenation
class L2SingleBank512K extends freechips.rocketchip.subsystem.WithInclusiveCache

class L2QuadBank2M extends freechips.rocketchip.subsystem.WithInclusiveCache(
    nBanks = 4,
    nWays = 16,
    capacityKB = 2048,
    subBankingFactor = 8,
    outerLatencyCycles = 30)

/*******************************************************************************
* Full TARGET_CONFIG configurations. These set parameters of the target being
* simulated.
*
* In general, if you're adding or removing features from any of these, you
* should CREATE A NEW ONE, WITH A NEW NAME. This is because the manager
* will store this name as part of the tags for the AGFI, so that later you can
* reconstruct what is in a particular AGFI. These tags are also used to
* determine which driver to build.
*******************************************************************************/
class FireSimRocketChipConfig extends Config(
  new WithBootROM ++
  new WithPeripheryBusFrequency(BigInt(3200000000L)) ++
  new WithExtMemSize(0x400000000L) ++ // 16GB
  new WithoutTLMonitors ++
  new WithUARTKey ++
  new WithNICKey ++
  new WithBlockDevice ++
  new WithRocketL2TLBs(1024) ++
  new WithPerfCounters ++
  new WithoutClockGating ++
  new WithDefaultMemModel ++
  new WithDefaultFireSimBridges ++
  new freechips.rocketchip.system.DefaultConfig)

class WithNDuplicatedRocketCores(n: Int) extends Config((site, here, up) => {
  case RocketTilesKey => List.tabulate(n)(i => up(RocketTilesKey).head.copy(hartId = i))
})

// single core config
class FireSimRocketChipSingleCoreConfig extends Config(new FireSimRocketChipConfig)

// dual core config
class FireSimRocketChipDualCoreConfig extends Config(
  new WithNDuplicatedRocketCores(2) ++
  new FireSimRocketChipSingleCoreConfig)

// quad core config
class FireSimRocketChipQuadCoreConfig extends Config(
  new WithNDuplicatedRocketCores(4) ++
  new FireSimRocketChipSingleCoreConfig)

// hexa core config
class FireSimRocketChipHexaCoreConfig extends Config(
  new WithNDuplicatedRocketCores(6) ++
  new FireSimRocketChipSingleCoreConfig)

// octa core config
class FireSimRocketChipOctaCoreConfig extends Config(
  new WithNDuplicatedRocketCores(8) ++
  new FireSimRocketChipSingleCoreConfig)

// SHA-3 accelerator config
class FireSimRocketChipSha3L2Config extends Config(
  new WithInclusiveCache ++
  new sha3.WithSha3Accel ++
  new WithNBigCores(1) ++
  new FireSimRocketChipConfig)

// SHA-3 accelerator config with synth printfs enabled
class FireSimRocketChipSha3L2PrintfConfig extends Config(
  new WithInclusiveCache ++
  new sha3.WithSha3Printf ++ 
  new sha3.WithSha3Accel ++
  new WithNBigCores(1) ++
  new FireSimRocketChipConfig)

// SiFive FSDK configs
class FsdkAloeConfig extends Config(
  new WithBootROM ++
  new WithPeripheryBusFrequency(BigInt(1400000000L)) ++
  new WithExtMemSize(0x400000000L) ++ // 16GB
  new WithoutTLMonitors ++
  new WithUARTKey ++
  new WithNICKey ++
  new WithBlockDevice ++
  new WithRocketL2TLBs(1024) ++
  new WithPerfCounters ++
  new WithoutClockGating ++
  new WithDefaultMemModel ++
  new WithDefaultFireSimBridges ++
  new freechips.rocketchip.system.DefaultConfig ++
  new WithInclusiveCache(
    nBanks = 4,
    nWays = 16,
    capacityKB = 2048,
    subBankingFactor = 8,
    outerLatencyCycles = 30)
)
// single core config
class FsdkAloe1xConfig extends Config(new FsdkAloeConfig)

// dual core config
class FsdkAloe2xConfig extends Config(
  new WithNDuplicatedRocketCores(2) ++
  new FireSimRocketChipSingleCoreConfig)

// quad core config
class FsdkAloe4xConfig extends Config(
  new WithNDuplicatedRocketCores(4) ++
  new FireSimRocketChipSingleCoreConfig)



class FireSimBoomConfig extends Config(
  new WithBootROM ++
  new WithPeripheryBusFrequency(BigInt(3200000000L)) ++
  new WithExtMemSize(0x400000000L) ++ // 16GB
  new WithoutTLMonitors ++
  new WithUARTKey ++
  new WithNICKey ++
  new WithBlockDevice ++
  new WithBoomEnableTrace ++
  new WithBoomL2TLBs(1024) ++
  new WithoutClockGating ++
  new WithDefaultMemModel ++
  new boom.common.WithLargeBooms ++
  new boom.common.WithNBoomCores(1) ++
  new WithDefaultFireSimBridges ++
  new freechips.rocketchip.system.BaseConfig
)

// A safer implementation than the one in BOOM in that it
// duplicates whatever BOOMTileKey.head is present N times. This prevents
// accidentally (and silently) blowing away configurations that may change the
// tile in the "up" view
class WithNDuplicatedBoomCores(n: Int) extends Config((site, here, up) => {
  case BoomTilesKey => List.tabulate(n)(i => up(BoomTilesKey).head.copy(hartId = i))
  case MaxHartIdBits => log2Up(site(BoomTilesKey).size)
})

class FireSimBoomDualCoreConfig extends Config(
  new WithNDuplicatedBoomCores(2) ++
  new FireSimBoomConfig)

class FireSimBoomQuadCoreConfig extends Config(
  new WithNDuplicatedBoomCores(4) ++
  new FireSimBoomConfig)

//**********************************************************************************
//* Heterogeneous Configurations
//*********************************************************************************/

// dual core config (rocket + small boom)
class FireSimRocketBoomConfig extends Config(
  new WithBoomL2TLBs(1024) ++ // reset l2 tlb amt ("WithSmallBooms" overrides it)
  new boom.common.WithRenumberHarts ++ // fix hart numbering
  new boom.common.WithSmallBooms ++ // change single BOOM to small
  new freechips.rocketchip.subsystem.WithNBigCores(1) ++ // add a "big" rocket core
  new FireSimBoomConfig
)

//**********************************************************************************
//* Supernode Configurations
//*********************************************************************************/

class SupernodeFireSimRocketChipConfig extends Config(
  new WithNumNodes(4) ++
  new WithExtMemSize(0x200000000L) ++ // 8GB
  new FireSimRocketChipConfig)

class SupernodeFireSimRocketChipSingleCoreConfig extends Config(
  new WithNumNodes(4) ++
  new WithExtMemSize(0x200000000L) ++ // 8GB
  new FireSimRocketChipSingleCoreConfig)

class SupernodeSixNodeFireSimRocketChipSingleCoreConfig extends Config(
  new WithNumNodes(6) ++
  new WithExtMemSize(0x40000000L) ++ // 1GB
  new FireSimRocketChipSingleCoreConfig)

class SupernodeEightNodeFireSimRocketChipSingleCoreConfig extends Config(
  new WithNumNodes(8) ++
  new WithExtMemSize(0x40000000L) ++ // 1GB
  new FireSimRocketChipSingleCoreConfig)

class SupernodeFireSimRocketChipDualCoreConfig extends Config(
  new WithNumNodes(4) ++
  new WithExtMemSize(0x200000000L) ++ // 8GB
  new FireSimRocketChipDualCoreConfig)

class SupernodeFireSimRocketChipQuadCoreConfig extends Config(
  new WithNumNodes(4) ++
  new WithExtMemSize(0x200000000L) ++ // 8GB
  new FireSimRocketChipQuadCoreConfig)

class SupernodeFireSimRocketChipHexaCoreConfig extends Config(
  new WithNumNodes(4) ++
  new WithExtMemSize(0x200000000L) ++ // 8GB
  new FireSimRocketChipHexaCoreConfig)

class SupernodeFireSimRocketChipOctaCoreConfig extends Config(
  new WithNumNodes(4) ++
  new WithExtMemSize(0x200000000L) ++ // 8GB
  new FireSimRocketChipOctaCoreConfig)

class WithTraceGen(params: Seq[DCacheParams], nReqs: Int = 8192)
    extends Config((site, here, up) => {
  case TraceGenKey => params.map { dcp => TraceGenParams(
    dcache = Some(dcp),
    wordBits = site(XLen),
    addrBits = 48,
    addrBag = {
      val nSets = dcp.nSets
      val nWays = dcp.nWays
      val blockOffset = site(SystemBusKey).blockOffset
      val nBeats = min(2, site(SystemBusKey).blockBeats)
      val beatBytes = site(SystemBusKey).beatBytes
      List.tabulate(2 * nWays) { i =>
        Seq.tabulate(nBeats) { j =>
          BigInt((j * beatBytes) + ((i * nSets) << blockOffset))
        }
      }.flatten
    },
    maxRequests = nReqs,
    memStart = site(ExtMem).get.master.base,
    numGens = params.size)
  }
  case MaxHartIdBits => log2Up(params.size)
})

class FireSimTraceGenConfig extends Config(
  new WithTraceGen(
    List.fill(2) { DCacheParams(nMSHRs = 2, nSets = 16, nWays = 2) }) ++
  new WithTraceGenBridge ++
  new FireSimRocketChipConfig)

class WithL2TraceGen(params: Seq[DCacheParams], nReqs: Int = 8192)
    extends Config((site, here, up) => {
  case TraceGenKey => params.map { dcp => TraceGenParams(
    dcache = Some(dcp),
    wordBits = site(XLen),
    addrBits = 48,
    addrBag = {
      val sbp = site(SystemBusKey)
      val l2p = site(InclusiveCacheKey)
      val nSets = max(l2p.sets, dcp.nSets)
      val nWays = max(l2p.ways, dcp.nWays)
      val nBanks = site(BankedL2Key).nBanks
      val blockOffset = sbp.blockOffset
      val nBeats = min(2, sbp.blockBeats)
      val beatBytes = sbp.beatBytes
      List.tabulate(2 * nWays) { i =>
        Seq.tabulate(nBeats) { j =>
          BigInt((j * beatBytes) + ((i * nSets * nBanks) << blockOffset))
        }
      }.flatten
    },
    maxRequests = nReqs,
    memStart = site(ExtMem).get.master.base,
    numGens = params.size)
  }
  case MaxHartIdBits => log2Up(params.size)
})

class FireSimTraceGenL2Config extends Config(
  new WithL2TraceGen(
    List.fill(2) { DCacheParams(nMSHRs = 2, nSets = 16, nWays = 2) }) ++
  new WithInclusiveCache(
    nBanks = 4,
    capacityKB = 1024,
    outerLatencyCycles = 50) ++
  new WithTraceGenBridge ++
  new FireSimRocketChipConfig)


class WithRationalTiles(multiplier: Int, divisor: Int) extends Config((site, here, up) => {
  case FireSimClockKey => FireSimClockParameters(Seq(multiplier -> divisor))
  case RocketCrossingKey => up(RocketCrossingKey, site) map { r =>
    r.copy(crossingType = RationalCrossing())
  }
  case BoomCrossingKey => up(BoomCrossingKey, site) map { r =>
    r.copy(crossingType = RationalCrossing())
  }
})

class HalfRateUncore extends WithRationalTiles(2,1)

// Eagle X Mock Configs
class EagleMockConfig(numCores: Int) extends Config(
  new WithBootROM ++
  new freechips.rocketchip.subsystem.WithInclusiveCache(nBanks = 16, capacityKB = 8192) ++
  new hwacha.DefaultHwachaConfig ++                        // use Hwacha vector accelerator
  new WithNBigCores(numCores) ++
  new FireSimRocketChipConfig)

class EX20C extends EagleMockConfig(20)
class EX16C extends EagleMockConfig(16)
class EX12C extends EagleMockConfig(12)
class EX8C extends EagleMockConfig(8)
