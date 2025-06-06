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
package org.openhab.binding.mqtt.homie.internal.homie500;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mqtt.generic.mapping.MandatoryField;
import org.openhab.binding.mqtt.generic.mapping.TopicPrefix;
import org.openhab.binding.mqtt.homie.internal.homie.DeviceAttributes;
import org.openhab.binding.mqtt.homie.internal.homie.NodeAttributes;

/**
 * Homie 5.x Device attributes
 *
 * @author Cody Cutrer - Initial contribution
 */
@TopicPrefix
@NonNullByDefault
public class Homie500DeviceAttributes extends DeviceAttributes {
    public @MandatoryField @Nullable String description;

    @Override
    public void reset() {
    }

    @Override
    public NodeAttributes createNodeAttributes() {
        return new Homie500NodeAttributes();
    }
}
