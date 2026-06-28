package uk.tsundokus.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import uk.tsundokus.core.domain.util.Loggable

@Serializable
abstract class LoggableNavKey : Loggable(), NavKey
