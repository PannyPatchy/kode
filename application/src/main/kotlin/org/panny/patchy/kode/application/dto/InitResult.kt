package org.panny.patchy.kode.application.dto

import org.panny.patchy.kode.domain.entity.KotlinProject

/**
 * Outcome of `kode init`. Presenter-agnostic: the CLI decides how to render it
 * (design ch. 01 / 03).
 */
sealed interface InitResult {
    val project: KotlinProject

    /**
     * `.kode.json` was written. [overwritten] is `true` when an existing config
     * was replaced.
     */
    data class Generated(
        override val project: KotlinProject,
        val overwritten: Boolean,
    ) : InitResult

    /**
     * An existing `.kode.json` was kept because the user declined to overwrite it.
     * Nothing was written.
     */
    data class Aborted(
        override val project: KotlinProject,
    ) : InitResult
}
