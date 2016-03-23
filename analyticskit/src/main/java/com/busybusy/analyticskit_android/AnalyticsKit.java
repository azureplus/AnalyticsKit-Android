/*
 * Copyright 2016 Busy, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.busybusy.analyticskit_android;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Presents an interface for logging Analytics events across multiple analytics providers.
 *
 * @author John Hunt on 3/4/16.
 */
public class AnalyticsKit
{
	private static AnalyticsKit singletonInstance = null;
	HashSet<AnalyticsKitProvider> providers;
	HashMap<String, AnalyticsEvent> timedEvents;

	private AnalyticsKit()
	{
		providers = new HashSet<>();
	}

	/**
	 * Returns the AnalyticsKit singleton
	 * @return the singleton instance
	 */
	public static AnalyticsKit getInstance()
	{
		if (singletonInstance == null)
		{
			// Uses double-checked locking to increase performance.
			// We only need to synchronize for the first few threads who might attempt to create separate instances.
			synchronized (AnalyticsKit.class)
			{
				if (singletonInstance == null)
				{
					singletonInstance = new AnalyticsKit();
				}
			}
		}

		return singletonInstance;
	}

	/**
	 * Registers an {@code AnalyticsKitProvider} instance to receive future events
	 * @param provider the {@code AnalyticsKitProvider} to notify on future calls to {@link AnalyticsKit#logEvent(AnalyticsEvent)}.
	 * @return the {@code AnalyticsKit} instance so multiple calls to {@code registerProvider(AnalyticsKitProvider)} can be chained.
	 */
	public AnalyticsKit registerProvider(@NonNull AnalyticsKitProvider provider)
	{
		providers.add(provider);
		return getInstance();
	}

	/**
	 * Sends the given event to all registered analytics providers (OR just to select providers if the event has been set to restrict the providers).
	 * @param event the event to capture with analytics tools
	 */
	public void logEvent(AnalyticsEvent event)
	{
		if (event.isTimed())
		{
			if (this.timedEvents == null)
			{
				timedEvents = new HashMap<>();
			}

			timedEvents.put(event.name(), event);
		}
		// No else needed: no need to worry about hanging on to this event

		if (providers.size() > 0)
		{
			for (AnalyticsKitProvider provider : providers)
			{
				if (event.providersMask != 0)
				{
					// the user has chosen to restrict the providers to which this event is sent
					if ((provider.getType() & event.providersMask) != 0)
					{
						provider.sendEvent(event);
					}
					// No else needed: the current provider has not been chosen - better luck next time
				}
				else
				{
					// no restrictions - send the event to all registered providers
					provider.sendEvent(event);
				}
			}
		}
	}

	/**
	 * Marks the end of a timed event
	 * @param eventName the unique name of the event that has finished
	 */
	public void endTimedEvent(@NonNull String eventName) throws IllegalStateException
	{
		if (this.timedEvents != null)
		{
			AnalyticsEvent timedEvent = this.timedEvents.remove(eventName);

			if (timedEvent != null)
			{
				if (providers.size() > 0)
				{
					for (AnalyticsKitProvider provider : providers)
					{
						if (timedEvent.providersMask != 0)
						{
							// the user has chosen to restrict the providers to which this event is sent
							if ((provider.getType() & timedEvent.providersMask) != 0)
							{
								provider.endTimedEvent(timedEvent);
							}
							// No else needed: the current provider has not been chosen - better luck next time
						}
						else
						{
							// no restrictions - send the event to all registered providers
							provider.endTimedEvent(timedEvent);
						}
					}
				}
			}
			else
			{
				throw new IllegalStateException("Attempted ending an event that was never started (or was previously ended): " + eventName);
			}
		}
		else // no timed events have been started
		{
			throw new IllegalStateException("Attempted ending an event that was never started (or was previously ended): " + eventName);
		}
	}

	/**
	 * Marks the end of a timed event
	 * @param event the event that has finished
	 */
	public void endTimedEvent(@NonNull AnalyticsEvent event)
	{
		endTimedEvent(event.name());
	}
}
