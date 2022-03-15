package io.github.devrawr.events

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerJoinEvent

typealias Filter<T> = (T) -> Boolean
typealias HandleEvent<T> = (T) -> Unit

data class CancelMessage<T>(
    val message: String,
    val filter: Filter<T>
)


class ListenerBuilder<T : Event>(private val type: Class<T>)
{
    private val filters = mutableListOf<Filter<T>>()
    private val cancelOn = mutableListOf<Filter<T>>()
    private val cancelMessages = mutableListOf<CancelMessage<T>>()

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

    fun cancelWithMessage(message: String, filter: Filter<T>): ListenerBuilder<T>
    {
        return this.apply {
            this.cancelMessages.add(
                CancelMessage(
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

                    for (function in cancelMessages)
                    {
                        if (function.filter.invoke(event))
                        {
                            when (event)
                            {
                                is Cancellable ->
                                {
                                    event.isCancelled = true

                                    if (event is PlayerEvent)
                                    {
                                        event.player.sendMessage(function.message)
                                    }
                                }
                                is PlayerJoinEvent -> event.player.kickPlayer(function.message)
                                else -> println("Used ListenerBuilder.cancelOn() with an event which cannot be cancelled, ${this.type.name}")
                            }
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

                            break
                        }
                    }

                    handle.invoke(event)
                }
            )
        }
    }
}