/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.modbus.sunspec.internal.parser;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.transport.modbus.ModbusBitUtilities;
import org.openhab.core.io.transport.modbus.ModbusConstants.ValueType;
import org.openhab.core.io.transport.modbus.ModbusRegisterArray;
import org.openhab.core.library.types.DecimalType;

/**
 * Base class for parsers with some helper methods
 *
 * @author Nagy Attila Gabor - Initial contribution
 *
 */
@NonNullByDefault
public class AbstractBaseParser {

    /**
     * Extract an optional int16 value
     *
     * @param raw the register array to extract from
     * @param index the address of the field
     * @return the parsed value or empty if the field is not implemented
     */
    protected Optional<Short> extractOptionalInt16(ModbusRegisterArray raw, int index) {
        return ModbusBitUtilities.extractStateFromRegisters(raw, index, ValueType.INT16).map(DecimalType::shortValue)
                .filter(value -> value != (short) 0x8000);
    }

    /**
     * Extract a mandatory int16 value
     *
     * @param raw the register array to extract from
     * @param index the address of the field
     * @param def the default value
     * @return the parsed value or the default if the field is not implemented
     */
    protected Short extractInt16(ModbusRegisterArray raw, int index, short def) {
        return Objects.requireNonNull(extractOptionalInt16(raw, index).orElse(def));
    }

    /**
     * Extract an optional uint16 value
     *
     * @param raw the register array to extract from
     * @param index the address of the field
     * @return the parsed value or empty if the field is not implemented
     */
    protected Optional<Integer> extractOptionalUInt16(ModbusRegisterArray raw, int index) {
        return ModbusBitUtilities.extractStateFromRegisters(raw, index, ValueType.UINT16).map(DecimalType::intValue)
                .filter(value -> value != 0xffff);
    }

    /**
     * Extract a mandatory uint16 value
     *
     * @param raw the register array to extract from
     * @param index the address of the field
     * @param def the default value
     * @return the parsed value or the default if the field is not implemented
     */
    protected Integer extractUInt16(ModbusRegisterArray raw, int index, int def) {
        return Objects.requireNonNull(extractOptionalUInt16(raw, index).orElse(def));
    }

    /**
     * Extract an optional acc32/uint32 value
     *
     * @param raw the register array to extract from
     * @param index the address of the field
     * @return the parsed value or empty if the field is not implemented
     */
    protected Optional<Long> extractOptionalAcc32(ModbusRegisterArray raw, int index) {
        return ModbusBitUtilities.extractStateFromRegisters(raw, index, ValueType.UINT32).map(DecimalType::longValue)
                .filter(value -> value != 0);
    }

    /**
     * Extract a mandatory acc32/uint32 value
     *
     * @param raw the register array to extract from
     * @param index the address of the field
     * @param def the default value
     * @return the parsed value or default if the field is not implemented
     */
    protected Long extractAcc32(ModbusRegisterArray raw, int index, long def) {
        return Objects.requireNonNull(extractOptionalAcc32(raw, index).orElse(def));
    }

    /**
     * Extract an optional scale factor
     *
     * @param raw the register array to extract from
     * @param index the address of the field
     * @return the parsed value or empty if the field is not implemented
     */
    protected Optional<Short> extractOptionalSunSSF(ModbusRegisterArray raw, int index) {
        return ModbusBitUtilities.extractStateFromRegisters(raw, index, ValueType.INT16).map(DecimalType::shortValue)
                .filter(value -> value != (short) 0x8000);
    }

    /**
     * Extract an mandatory scale factor
     *
     * @param raw the register array to extract from
     * @param index the address of the field
     * @return the parsed value or 1 if the field is not implemented
     */
    protected Short extractSunSSF(ModbusRegisterArray raw, int index) {
        return Objects.requireNonNull(extractOptionalSunSSF(raw, index).orElse((short) 0));
    }
}
