package mesosphere.marathon
package state

import mesosphere.marathon.Protos.Constraint
import mesosphere.marathon.core.pod.Network
import mesosphere.marathon.raml.Resources

import scala.concurrent.duration._

/**
  * Configures exponential backoff behavior when launching potentially sick apps.
  * This prevents sandboxes associated with consecutively failing tasks from filling up the hard disk on Mesos slaves.
  * The backoff period is multiplied by the factor for each consecutive failure until it reaches maxLaunchDelaySeconds.
  * This applies also to instances that are killed due to failing too many health checks.
  * @param backoff The initial backoff applied when a launched instance fails.
  *   minimum: 0.0
  * @param factor The factor applied to the current backoff to determine the new backoff.
  *   minimum: 0.0
  * @param maxLaunchDelay The maximum backoff applied when subsequent failures are detected.
  *   minimum: 0.0
  */
case class BackoffStrategy(
    backoff: FiniteDuration = 1.seconds,
    maxLaunchDelay: FiniteDuration = 5.minutes,
    factor: Double = 1.15)

/**
  * A generic spec that specifies something that Marathon is able to launch instances of.
  */

// TODO(PODS): Group some of this into little types and pattern match when things really
// don't make sense to do generically, eg 'executor', 'cmd', 'args', etc.
// we should try to group things up logically - pod does a decent job of this
trait RunSpec extends plugin.RunSpec {
  val id: PathId
  val env: Map[String, EnvVarValue]
  val labels: Map[String, String]
  val acceptedResourceRoles: Set[String]
  val secrets: Map[String, Secret]
  val instances: Int
  val constraints: Set[Constraint]
  val version: Timestamp
  val resources: Resources
  val backoffStrategy: BackoffStrategy
  val upgradeStrategy: UpgradeStrategy
  def isResident: Boolean = persistentVolumes.nonEmpty
  def withInstances(instances: Int): RunSpec
  def isUpgrade(to: RunSpec): Boolean
  def needsRestart(to: RunSpec): Boolean
  def isOnlyScaleChange(to: RunSpec): Boolean
  def isScaledToZero: Boolean = instances == 0
  val versionInfo: VersionInfo
  val container = Option.empty[Container]
  val cmd = Option.empty[String]
  val args = Seq.empty[String]
  val isSingleInstance: Boolean = false
  val volumes = Seq.empty[Volume]
  val volumeMounts = Seq.empty[VolumeMount]
  val persistentVolumes = Seq.empty[PersistentVolume]
  val persistentVolumeMounts = Seq.empty[VolumeMount]
  val externalVolumes = Seq.empty[ExternalVolume]
  val diskForPersistentVolumes: Double = 0.0
  val user: Option[String]
  val unreachableStrategy: UnreachableStrategy
  val killSelection: KillSelection
  val networks: Seq[Network]

  final def ref: RunSpecRef = RunSpecRef(id, version)
  /**
    * Reference to the last config ref
    */
  final def configRef: RunSpecConfigRef = RunSpecConfigRef(id, versionInfo.lastConfigChangeVersion)
}

/**
  * Points to a specific version of a runSpec
  */
final case class RunSpecRef(
    id: PathId,
    version: Timestamp)

/**
  * Points to a runSpec at some config point in time
  */
final case class RunSpecConfigRef(
    id: PathId,
    configVersion: Timestamp)
