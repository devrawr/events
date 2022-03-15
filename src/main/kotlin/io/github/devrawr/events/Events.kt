package io.github.devrawr.events

import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin


object Events
{
    private var plugin: Plugin

    private val events = mutableListOf<WrappedEventExecutor<*>>()
    private val listener = object : Listener
    {}

    init
    {
        val pluginManager = Bukkit.getPluginManager()
        val plugins = pluginManager.plugins

        this.plugin = plugins.first()
    }

    fun withPlugin(plugin: Plugin) : Events
    {
        return this.apply {
            this.plugin = plugin

            // unregister the listener first
            HandlerList.unregisterAll(listener)

            for (event in this.events)
            {
                this.register(event)
            }
        }
    }

    inline fun <reified T : Event> listenTo(): ListenerBuilder<T> = listenTo(T::class.java)

    fun <T : Event> listenTo(type: Class<T>): ListenerBuilder<T>
    {
        return ListenerBuilder(type)
    }

    fun <T : Event> register(event: WrappedEventExecutor<T>)
    {
        this.events.add(event)

        val pluginManager = Bukkit.getPluginManager()

        pluginManager.registerEvent(
            event.event, this.listener, event.priority, event.executor, this.plugin
        )
    }
}