package org.granitemc.granite.api.event;

import org.granitemc.granite.api.plugin.PluginContainer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * **************************************************************************************
 * License (MIT)
 * <p/>
 * Copyright (c) 2014. Granite Team
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * **************************************************************************************
 */

public class EventHandlerContainer {
    private On annotation;
    private Method method;

    private Object instance;

    private PluginContainer plugin;

    public EventHandlerContainer(PluginContainer plugin, Object instance, Method method) {
        annotation = method.getAnnotation(On.class);
        this.method = method;

        this.plugin = plugin;
        this.instance = instance;
    }

    public Class<? extends Event> getEventType() {
        return annotation.event();
    }

    public Object getInstance() {
        return instance;
    }

    public PluginContainer getPlugin() {
        return plugin;
    }

    public void invoke(Event event) {
        try {
            method.invoke(instance, event);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.getCause().printStackTrace();
        }
    }
}
