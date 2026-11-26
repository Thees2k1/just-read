package com.justread.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

@Serializable data object AddBook : NavKey

@Serializable data class Reader(val bookId: String) : NavKey
