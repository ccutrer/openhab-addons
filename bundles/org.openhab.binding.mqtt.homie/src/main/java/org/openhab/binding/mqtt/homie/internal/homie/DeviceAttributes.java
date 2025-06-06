/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.binding.mqtt.homie.internal.homie;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mqtt.generic.mapping.AbstractMqttAttributeClass;
import org.openhab.binding.mqtt.generic.mapping.MandatoryField;
import org.openhab.binding.mqtt.generic.mapping.TopicPrefix;

/**
 * Homie Device attributes
 *
 * @author David Graeff - Initial contribution
 */
@TopicPrefix
@NonNullByDefault
public class DeviceAttributes extends AbstractMqttAttributeClass {
    // Lower-case enum value names required. Those are identifiers for the MQTT/homie protocol.
    public enum ReadyState {
        unknown,
        init,
        ready,
        disconnected,
        sleeping,
        lost,
        alert
    }

    public @MandatoryField ReadyState state = ReadyState.unknown;

    @Override
    public Object getFieldsOf() {
        return this;
    }

    /**
     * Reset any cached state on the device attributes, and re-parse if necessary.
     */
    public void reset() {
    }

    public String getHomieVersion() {
        return "unknown";
    }

    public String[] getNodes() {
        return new String[0];
    }

    public NodeAttributes createNodeAttributes() {
        return new NodeAttributes();
    }
}
