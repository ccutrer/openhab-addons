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

import org.openhab.io.homekit.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.internal.HomekitCharacteristicType;
import org.openhab.io.homekit.internal.HomekitException;
import org.openhab.io.homekit.internal.HomekitSettings;
import org.openhab.io.homekit.internal.HomekitTaggedItem;

import io.github.hapjava.characteristics.Characteristic;
import io.github.hapjava.characteristics.impl.common.OnCharacteristic;
import io.github.hapjava.characteristics.impl.outlet.OutletInUseCharacteristic;
import io.github.hapjava.services.impl.OutletService;

/**
 *
 * @author Eugen Freiter - Initial contribution
 */
public class HomekitOutletImpl extends AbstractHomekitAccessoryImpl {
    private final BooleanItemReader inUseReader;

    public HomekitOutletImpl(HomekitTaggedItem taggedItem, List<HomekitTaggedItem> mandatoryCharacteristics,
            List<Characteristic> mandatoryRawCharacteristics, HomekitAccessoryUpdater updater, HomekitSettings settings)
            throws IncompleteAccessoryException {
        super(taggedItem, mandatoryCharacteristics, mandatoryRawCharacteristics, updater, settings);
        inUseReader = createBooleanReader(HomekitCharacteristicType.INUSE_STATUS);
    }

    @Override
    public void init() throws HomekitException {
        super.init();
        addService(new OutletService(getCharacteristic(OnCharacteristic.class).get(),
                new OutletInUseCharacteristic(inUseReader,
                        (cb) -> subscribe(HomekitCharacteristicType.INUSE_STATUS, cb),
                        () -> unsubscribe(HomekitCharacteristicType.INUSE_STATUS))));
    }
}
