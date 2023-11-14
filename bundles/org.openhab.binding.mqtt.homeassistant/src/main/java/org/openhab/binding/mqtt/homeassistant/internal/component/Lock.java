/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.mqtt.homeassistant.internal.component;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mqtt.generic.values.OnOffValue;
import org.openhab.binding.mqtt.generic.values.TextValue;
import org.openhab.binding.mqtt.homeassistant.internal.config.dto.AbstractChannelConfiguration;
import org.openhab.binding.mqtt.homeassistant.internal.exception.ConfigurationException;

import com.google.gson.annotations.SerializedName;

/**
 * A MQTT lock, following the https://www.home-assistant.io/components/lock.mqtt/ specification.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class Lock extends AbstractComponent<Lock.ChannelConfiguration> {
    public static final String LOCK_CHANNEL_ID = "lock";
    public static final String STATE_CHANNEL_ID = "state";
    public static final String OPEN_CHANNEL_ID = "open";

    /**
     * Configuration class for MQTT component
     */
    static class ChannelConfiguration extends AbstractChannelConfiguration {
        ChannelConfiguration() {
            super("MQTT Lock");
        }

        protected boolean optimistic = false;

        @SerializedName("command_topic")
        protected @Nullable String commandTopic;
        @SerializedName("state_topic")
        protected String stateTopic = "";
        @SerializedName("payload_lock")
        protected String payloadLock = "LOCK";
        @SerializedName("payload_unlock")
        protected String payloadUnlock = "UNLOCK";
        @SerializedName("payload_open")
        protected String payloadOpen = "OPEN";
        @SerializedName("state_jammed")
        protected String stateJammed = "JAMMED";
        @SerializedName("state_locked")
        protected String stateLocked = "LOCKED";
        @SerializedName("state_locking")
        protected String stateLocking = "LOCKING";
        @SerializedName("state_unlocked")
        protected String stateUnlocked = "UNLOCKED";
        @SerializedName("state_unlocking")
        protected String stateUnlocking = "UNLOCKING";
    }

    public Lock(ComponentFactory.ComponentConfiguration componentConfiguration) {
        super(componentConfiguration, ChannelConfiguration.class);

        // We do not support all HomeAssistant quirks
        if (channelConfiguration.optimistic && !channelConfiguration.stateTopic.isBlank()) {
            throw new ConfigurationException("Component:Lock does not support forced optimistic mode");
        }

        String stateTopic = channelConfiguration.stateTopic;

        // State can indicate additional information than just
        // locked/unlocked, so expose it as a separate channel
        if (stateTopic != null) {
            Set<String> states = new HashSet<>();
            states.add(channelConfiguration.stateJammed);
            states.add(channelConfiguration.stateLocked);
            states.add(channelConfiguration.stateLocking);
            states.add(channelConfiguration.stateUnlocked);
            states.add(channelConfiguration.stateUnlocking);

            TextValue value = new TextValue(states);
            buildChannel(STATE_CHANNEL_ID, value, "State", componentConfiguration.getUpdateListener())
                    .stateTopic(stateTopic).isAdvanced(true).build();
        }

        buildChannel(LOCK_CHANNEL_ID,
                new OnOffValue(channelConfiguration.stateLocked, channelConfiguration.stateUnlocked,
                        channelConfiguration.payloadLock, channelConfiguration.payloadUnlock),
                "Lock", componentConfiguration.getUpdateListener())
                .stateTopic(channelConfiguration.stateTopic, channelConfiguration.getValueTemplate())
                .commandTopic(channelConfiguration.commandTopic, channelConfiguration.isRetain(),
                        channelConfiguration.getQos())
                .build();
        buildChannel(OPEN_CHANNEL_ID, new OnOffValue(channelConfiguration.payloadOpen), "Open",
                componentConfiguration.getUpdateListener())
                .commandTopic(channelConfiguration.commandTopic, channelConfiguration.isRetain(),
                        channelConfiguration.getQos())
                .build();
    }
}
