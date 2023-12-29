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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mqtt.generic.values.TextValue;
import org.openhab.binding.mqtt.homeassistant.generic.internal.MqttBindingConstants;
import org.openhab.binding.mqtt.homeassistant.internal.config.dto.AbstractChannelConfiguration;

import com.google.gson.annotations.SerializedName;

/**
 * A MQTT sensor, following the https://www.home-assistant.io/integrations/tag.mqtt/ specification.
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public class Tag extends AbstractComponent<Tag.ChannelConfiguration> {
    public static final String TAG_CHANNEL_ID = "tag";

    /**
     * Configuration class for MQTT component
     */
    static class ChannelConfiguration extends AbstractChannelConfiguration {
        ChannelConfiguration() {
            super("MQTT Tag");
        }

        @SerializedName("topic")
        protected String topic = "";
    }

    public Tag(ComponentFactory.ComponentConfiguration componentConfiguration, boolean newStyleChannels) {
        super(componentConfiguration, ChannelConfiguration.class, newStyleChannels, true);

        buildChannel(TAG_CHANNEL_ID, MqttBindingConstants.CHANNEL_TYPE_UID_TRIGGER, new TextValue(), getName(),
                componentConfiguration.getUpdateListener())
                .stateTopic(channelConfiguration.topic, channelConfiguration.getValueTemplate())//
                .trigger(true).build();
    }
}
