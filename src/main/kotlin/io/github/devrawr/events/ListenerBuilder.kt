package io.github.devrawr.events

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin

typealias Filter<T> = (T) -> Boolean
typealias HandleEvent<T> = (T) -> Unit

class ListenerBuilder<T : Event>(private val type: Class<T>)
{
    private val filters = mutableListOf<Filter<T>>()
    private val cancelOn = mutableListOf<Filter<T>>()
    private val handle = mutableListOf<HandleEvent<T>>()

    private var priority: EventPriority = EventPriority.NORMAL

    fun filter(filter: Filter<T>): ListenerBuilder<T>
    {
        return this.apply {
            this.filters.add(filter)
        }
    }

    fun cancelOn(filter: Filter<T>): ListenerBuilder<T>
    {
        return this.apply {
            this.cancelOn.add(filter)
        }
    }

    fun priority(priority: EventPriority): ListenerBuilder<T>
    {
        return this.apply {
            this.priority = priority
        }
    }

    fun on(handle: HandleEvent<T>): ListenerBuilder<T>
    {
        return this.apply {
            this.handle.add(handle)
        }
    }

    fun apply(plugin: JavaPlugin): ListenerBuilder<T>
    {
        return this.apply {
            Events.register(
                WrappedEventExecutor(
                    this.type,
                    this.priority,
                ) { _, event ->
                    try
                    {
                        event as T // haha stupid smart cast work around LOOL L
                    } catch (ignored: Exception)
                    {
                        return@WrappedEventExecutor
                    }

                    for (filter in filters)
                    {
                        if (!filter.invoke(event))
                        {
                            return@WrappedEventExecutor
                        }
                    }

                    for (function in cancelOn)
                    {
                        if (function.invoke(event))
                        {
                            when (event)
                            {
                                is Cancellable -> event.isCancelled = true
                                is PlayerJoinEvent -> event.player.kickPlayer("Event cancelled")
                                else -> println("Used ListenerBuilder.cancelOn() with an event which cannot be cancelled, ${this.type.name}")
                            }
                        }
                    }

                    for (function in handle)
                    {
                        function.invoke(event)
                    }
                }
            )
        }
    }
}