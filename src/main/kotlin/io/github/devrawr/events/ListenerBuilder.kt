package io.github.devrawr.events

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerJoinEvent

typealias Filter<T> = (T) -> Boolean
typealias HandleEvent<T> = (T) -> Unit

class Cancel<T>(
    private val message: String?,
    private val filter: Filter<T>
)
{
    fun attemptCancel(event: T)
    {
        if (filter.invoke(event))
        {
            when (event)
            {
                is Cancellable ->
                {
                    event.isCancelled = true

                    if (message != null && event is PlayerEvent)
                    {
                        event.player.sendMessage(message)
                    }
                }
                is PlayerJoinEvent -> event.player.kickPlayer(message ?: "Event cancelled")
                else -> println("Used ListenerBuilder.cancelOn() with an event which cannot be cancelled, $event")
            }
        }
    }
}

class ListenerBuilder<T : Event>(private val type: Class<T>)
{
    private val filters = mutableListOf<Filter<T>>()
    private val cancelOn = mutableListOf<Cancel<T>>()
    private var priority: EventPriority = EventPriority.NORMAL

    fun filter(filter: Filter<T>): ListenerBuilder<T>
    {
        return this.apply {
            this.filters.add(filter)
        }
    }

    fun cancelOn(message: String? = null, filter: Filter<T>): ListenerBuilder<T>
    {
        return this.apply {
            this.cancelOn.add(
                Cancel(
                    message, filter
                )
            )
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
                        function.attemptCancel(event)
                    }

                    handle.invoke(event)
                }
            )
        }
    }
}