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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.binding.mqtt.generic.values.TextValue;
import org.openhab.core.library.types.StringType;

/**
 * Tests for {@link Lock}
 *
 * @author Anton Kharuzhy - Initial contribution
 */
@NonNullByDefault
public class LockTests extends AbstractComponentTests {
    public static final String CONFIG_TOPIC = "lock/0x0000000000000000_lock_zigbee2mqtt";

    @SuppressWarnings("null")
    @Test
    public void test() throws InterruptedException {
        // @formatter:off
        var component = discoverComponent(configTopicToMqtt(CONFIG_TOPIC),
                """
                { \
                  "availability": [ \
                    { \
                      "topic": "zigbee2mqtt/bridge/state" \
                    } \
                  ], \
                  "device": { \
                    "identifiers": [ \
                      "zigbee2mqtt_0x0000000000000000" \
                    ], \
                    "manufacturer": "Locks inc", \
                    "model": "Lock", \
                    "name": "LockBlower", \
                    "sw_version": "Zigbee2MQTT 1.18.2" \
                  }, \
                  "name": "lock", \
                  "payload_unlock": "UNLOCK_", \
                  "payload_lock": "LOCK_", \
                  "state_unlocked": "UNLOCK_", \
                  "state_locked": "LOCK_", \
                  "state_topic": "zigbee2mqtt/lock/state", \
                  "command_topic": "zigbee2mqtt/lock/set/state" \
                }\
                """);
        // @formatter:on

        assertThat(component.channels.size(), is(1));
        assertThat(component.getName(), is("lock"));

        assertChannel(component, Lock.LOCK_CHANNEL_ID, "zigbee2mqtt/lock/state", "zigbee2mqtt/lock/set/state", "State",
                TextValue.class);

        publishMessage("zigbee2mqtt/lock/state", "LOCK_");
        assertState(component, Lock.LOCK_CHANNEL_ID, new StringType("LOCK_"));
        publishMessage("zigbee2mqtt/lock/state", "UNLOCK_");
        assertState(component, Lock.LOCK_CHANNEL_ID, new StringType("UNLOCK_"));
        publishMessage("zigbee2mqtt/lock/state", "JAMMED");
        assertState(component, Lock.LOCK_CHANNEL_ID, new StringType("JAMMED"));
        publishMessage("zigbee2mqtt/lock/state", "GARBAGE");
        assertState(component, Lock.LOCK_CHANNEL_ID, new StringType("JAMMED"));

        component.getChannel(Lock.LOCK_CHANNEL_ID).getState().publishValue(new StringType("UNLOCK_"));
        assertPublished("zigbee2mqtt/lock/set/state", "UNLOCK_");
        component.getChannel(Lock.LOCK_CHANNEL_ID).getState().publishValue(new StringType("LOCK_"));
        assertPublished("zigbee2mqtt/lock/set/state", "LOCK_");

        assertThrows(IllegalArgumentException.class,
                () -> component.getChannel(Lock.LOCK_CHANNEL_ID).getState().publishValue(new StringType("LOCK")));
        component.getChannel(Lock.LOCK_CHANNEL_ID).getState().publishValue(new StringType("OPEN"));
        assertPublished("zigbee2mqtt/lock/set/state", "OPEN");
    }

    @Test
    public void forceOptimisticIsNotSupported() {
        // @formatter:off
        publishMessage(configTopicToMqtt(CONFIG_TOPIC),
                """
                { \
                  "availability": [ \
                    { \
                      "topic": "zigbee2mqtt/bridge/state" \
                    } \
                  ], \
                  "device": { \
                    "identifiers": [ \
                      "zigbee2mqtt_0x0000000000000000" \
                    ], \
                    "manufacturer": "Locks inc", \
                    "model": "Lock", \
                    "name": "LockBlower", \
                    "sw_version": "Zigbee2MQTT 1.18.2" \
                  }, \
                  "name": "lock", \
                  "payload_unlock": "UNLOCK_", \
                  "payload_lock": "LOCK_", \
                  "optimistic": "true", \
                  "state_topic": "zigbee2mqtt/lock/state", \
                  "command_topic": "zigbee2mqtt/lock/set/state" \
                }\
                """);
        // @formatter:on
    }

    @Override
    protected Set<String> getConfigTopics() {
        return Set.of(CONFIG_TOPIC);
    }
}
