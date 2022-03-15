package io.github.devrawr.events

import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.plugin.EventExecutor

data class WrappedEventExecutor<T : Event>(
    val event: Class<T>,
    val priority: EventPriority,
    val executor: EventExecutor
)
