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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mqtt.homie.internal.homie.PropertyAttributes;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection;

/**
 * Homie 5.x Property attributes
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public class Homie500PropertyAttributes extends PropertyAttributes {
    @Override
    public CompletableFuture<@Nullable Void> subscribeAndReceive(MqttBrokerConnection connection,
            ScheduledExecutorService scheduler, String basetopic, @Nullable AttributeChanged attributeChangedListener,
            int timeout) {
        // All metadata is already available from the description on the device, so nothing to do
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<@Nullable Void> unsubscribe() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
