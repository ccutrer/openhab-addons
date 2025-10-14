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
package org.openhab.io.homekit.internal.accessories;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.internal.HomekitException;
import org.openhab.io.homekit.internal.HomekitSettings;
import org.openhab.io.homekit.internal.HomekitTaggedItem;

import io.github.hapjava.characteristics.Characteristic;
import io.github.hapjava.characteristics.impl.common.ActiveCharacteristic;
import io.github.hapjava.characteristics.impl.common.InUseCharacteristic;
import io.github.hapjava.characteristics.impl.common.ProgramModeCharacteristic;
import io.github.hapjava.characteristics.impl.common.ServiceLabelNamespaceCharacteristic;
import io.github.hapjava.characteristics.impl.common.ServiceLabelNamespaceEnum;
import io.github.hapjava.services.impl.IrrigationSystemService;
import io.github.hapjava.services.impl.ServiceLabelService;

/**
 * Implements an Irrigation System accessory.
 * 
 * To be a complete accessory, the user must configure individual valves linked
 * to this primary service. This class also adds the ServiceLabelService
 * automatically.
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault({})
public class HomekitIrrigationSystemImpl extends AbstractHomekitAccessoryImpl {
    public HomekitIrrigationSystemImpl(HomekitTaggedItem taggedItem, List<HomekitTaggedItem> mandatoryCharacteristics,
            List<Characteristic> mandatoryRawCharacteristics, HomekitAccessoryUpdater updater, HomekitSettings settings)
            throws IncompleteAccessoryException {
        super(taggedItem, mandatoryCharacteristics, mandatoryRawCharacteristics, updater, settings);
        addService(new IrrigationSystemService(getCharacteristic(ActiveCharacteristic.class).get(),
                getCharacteristic(InUseCharacteristic.class).get(),
                getCharacteristic(ProgramModeCharacteristic.class).get()));
    }

    @Override
    public void init() throws HomekitException {
        super.init();

        var serviceLabelNamespace = getCharacteristic(ServiceLabelNamespaceCharacteristic.class)
                .orElseGet(() -> new ServiceLabelNamespaceCharacteristic(
                        () -> CompletableFuture.completedFuture(ServiceLabelNamespaceEnum.ARABIC_NUMERALS)));
        addService(new ServiceLabelService(serviceLabelNamespace));
    }
}
