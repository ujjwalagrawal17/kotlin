/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.NamedDomainObjectCollection
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainerWithPresets
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget

open class KotlinMultiplatformExtension : KotlinProjectExtension(), KotlinTargetContainerWithPresetFunctions {
    override lateinit var presets: NamedDomainObjectCollection<KotlinTargetPreset<*>>
        internal set

    override lateinit var targets: NamedDomainObjectCollection<KotlinTarget>
        internal set

    internal var isGradleMetadataAvailable: Boolean = false
    internal var isGradleMetadataExperimental: Boolean = false

    fun metadata(configure: KotlinOnlyTarget<KotlinCommonCompilation>.() -> Unit = { }): KotlinOnlyTarget<KotlinCommonCompilation> =
        @Suppress("UNCHECKED_CAST")
        (targets.getByName(KotlinMultiplatformPlugin.METADATA_TARGET_NAME) as KotlinOnlyTarget<KotlinCommonCompilation>).also(configure)
}

private fun KotlinTarget.isProducedFromPreset(kotlinTargetPreset: KotlinTargetPreset<*>): Boolean =
    originatingPreset == kotlinTargetPreset

internal inline fun <reified T : KotlinTarget, reified P : KotlinTargetPreset<T>> KotlinTargetsContainerWithPresets.configureOrCreate(
    targetName: String,
    targetPreset: P,
    configure: T.() -> Unit
): T {
    val existingTarget = targets.findByName(targetName)
    when {
        existingTarget is T && existingTarget.isProducedFromPreset(targetPreset) -> {
            configure(existingTarget)
            return existingTarget
        }
        existingTarget == null -> {
            val newTarget = targetPreset.createTarget(targetName)
            targets.add(newTarget)
            configure(newTarget)
            return newTarget
        }
        else -> {
            error(
                "The target '$targetName' already exists, but it was not created with the '${targetPreset.name}' preset. " +
                        "To configure it, access it by name in `kotlin.targets`" +
                        " or use the preset function '${existingTarget.originatingPreset?.name}'"
                            .takeIf { existingTarget.originatingPreset != null }
            )
        }
    }
}