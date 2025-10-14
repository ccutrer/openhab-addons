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
import org.openhab.io.homekit.internal.HomekitException;
import org.openhab.io.homekit.internal.HomekitSettings;
import org.openhab.io.homekit.internal.HomekitTaggedItem;

import io.github.hapjava.characteristics.Characteristic;
import io.github.hapjava.characteristics.impl.securitysystem.CurrentSecuritySystemStateCharacteristic;
import io.github.hapjava.characteristics.impl.securitysystem.TargetSecuritySystemStateCharacteristic;
import io.github.hapjava.services.impl.SecuritySystemService;

/**
 * Implements SecuritySystem as a GroupedAccessory made up of multiple items:
 * <ul>
 * <li>CurrentSecuritySystemState: String type</li>
 * <li>TargetSecuritySystemState: String type</li>
 * </ul>
 * 
 * @author Cody Cutrer - Initial contribution
 */
public class HomekitSecuritySystemImpl extends AbstractHomekitAccessoryImpl {
    public HomekitSecuritySystemImpl(HomekitTaggedItem taggedItem, List<HomekitTaggedItem> mandatoryCharacteristics,
            List<Characteristic> mandatoryRawCharacteristics, HomekitAccessoryUpdater updater,
            HomekitSettings settings) {
        super(taggedItem, mandatoryCharacteristics, mandatoryRawCharacteristics, updater, settings);
    }

    @Override
    public void init() throws HomekitException {
        super.init();
        addService(new SecuritySystemService(getCharacteristic(CurrentSecuritySystemStateCharacteristic.class).get(),
                getCharacteristic(TargetSecuritySystemStateCharacteristic.class).get()));
    }
}
