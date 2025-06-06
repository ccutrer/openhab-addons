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
package org.openhab.binding.mqtt.homie.internal.homie300;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mqtt.generic.mapping.MQTTvalueTransform;
import org.openhab.binding.mqtt.generic.mapping.MandatoryField;
import org.openhab.binding.mqtt.generic.mapping.TopicPrefix;
import org.openhab.binding.mqtt.homie.internal.homie.DeviceAttributes;

/**
 * Homie 3.x Device attributes
 *
 * @author David Graeff - Initial contribution
 */
@TopicPrefix
@NonNullByDefault
public class Homie300DeviceAttributes extends DeviceAttributes {
    public @MandatoryField @Nullable String homie;
    public @MandatoryField @Nullable String name;
    public @MandatoryField @MQTTvalueTransform(splitCharacter = ",") String @Nullable [] nodes;

    @Override
    public String getHomieVersion() {
        String homie = this.homie;
        return homie != null ? homie : super.getHomieVersion();
    }

    @Override
    public String[] getNodes() {
        String[] nodes = this.nodes;
        return nodes != null ? nodes : super.getNodes();
    }
}
