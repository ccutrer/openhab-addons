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

import static org.openhab.io.homekit.internal.HomekitCharacteristicType.ACTIVE_STATUS;

import java.util.List;

import org.openhab.io.homekit.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.internal.HomekitException;
import org.openhab.io.homekit.internal.HomekitSettings;
import org.openhab.io.homekit.internal.HomekitTaggedItem;

import io.github.hapjava.characteristics.Characteristic;
import io.github.hapjava.characteristics.impl.heatercooler.CurrentHeaterCoolerStateCharacteristic;
import io.github.hapjava.characteristics.impl.heatercooler.TargetHeaterCoolerStateCharacteristic;
import io.github.hapjava.characteristics.impl.thermostat.CurrentTemperatureCharacteristic;
import io.github.hapjava.characteristics.impl.thermostat.TemperatureDisplayUnitCharacteristic;
import io.github.hapjava.services.impl.HeaterCoolerService;

/**
 * Implements Heater Cooler
 *
 * @author Eugen Freiter - Initial contribution
 */

public class HomekitHeaterCoolerImpl extends AbstractHomekitAccessoryImpl {
    public HomekitHeaterCoolerImpl(HomekitTaggedItem taggedItem, List<HomekitTaggedItem> mandatoryCharacteristics,
            List<Characteristic> mandatoryRawCharacteristics, HomekitAccessoryUpdater updater, HomekitSettings settings)
            throws IncompleteAccessoryException {
        super(taggedItem, mandatoryCharacteristics, mandatoryRawCharacteristics, updater, settings);
    }

    @Override
    public void init() throws HomekitException {
        super.init();
        final HeaterCoolerService service = new HeaterCoolerService(
                HomekitCharacteristicFactory.createActiveCharacteristic(getCharacteristic(ACTIVE_STATUS).get(),
                        getUpdater()),
                getCharacteristic(CurrentTemperatureCharacteristic.class).get(),
                getCharacteristic(CurrentHeaterCoolerStateCharacteristic.class).get(),
                getCharacteristic(TargetHeaterCoolerStateCharacteristic.class).get());

        var temperatureDisplayUnit = getCharacteristic(TemperatureDisplayUnitCharacteristic.class);
        if (temperatureDisplayUnit.isEmpty()) {
            service.addOptionalCharacteristic(
                    HomekitCharacteristicFactory.createSystemTemperatureDisplayUnitCharacteristic());
        }

        addService(service);
    }
}
