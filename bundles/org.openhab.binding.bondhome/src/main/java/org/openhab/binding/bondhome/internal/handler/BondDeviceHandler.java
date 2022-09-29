/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.bondhome.internal.handler;

import static org.openhab.binding.bondhome.internal.BondHomeBindingConstants.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.bondhome.internal.api.BondDevice;
import org.openhab.binding.bondhome.internal.api.BondDeviceAction;
import org.openhab.binding.bondhome.internal.api.BondDeviceProperties;
import org.openhab.binding.bondhome.internal.api.BondDeviceState;
import org.openhab.binding.bondhome.internal.api.BondDeviceType;
import org.openhab.binding.bondhome.internal.api.BondHttpApi;
import org.openhab.binding.bondhome.internal.config.BondDeviceConfiguration;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BondDeviceHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class BondDeviceHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(BondDeviceHandler.class);

    private @NonNullByDefault({}) BondDeviceConfiguration config;
    private @Nullable BondHttpApi api;

    private @Nullable BondDevice deviceInfo;
    private @Nullable BondDeviceProperties deviceProperties;
    private @Nullable BondDeviceState deviceState;

    private @Nullable ScheduledFuture<?> pollingJob;

    private volatile boolean disposed;
    private volatile boolean fullyInitialized;

    private long latestUpdate = -1;

    /**
     * The supported thing types.
     */
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Stream
            .of(THING_TYPE_BOND_FAN, THING_TYPE_BOND_SHADES, THING_TYPE_BOND_FIREPLACE, THING_TYPE_BOND_GENERIC)
            .collect(Collectors.toSet());

    public BondDeviceHandler(Thing thing) {
        super(thing);
        disposed = true;
        fullyInitialized = false;
        config = getConfigAs(BondDeviceConfiguration.class);
        logger.trace("Created handler for bond device with device id {}.", config.deviceId);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (hasConfigurationError() || !fullyInitialized) {
            logger.trace(
                    "Bond device handler for {} received command {} on channel {} but is not yet prepared to handle it.",
                    config.deviceId, command, channelUID);
            return;
        }

        logger.trace("Bond device handler for {} received command {} on channel {}", config.deviceId, command,
                channelUID);
        final BondHttpApi api = this.api;
        if (api == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Bridge API not available");
            // Re-attempt initialization
            scheduler.schedule(() -> {
                logger.trace("Re-attempting initialization");
                initialize();
            }, 30, TimeUnit.SECONDS);
            return;
        }

        if (command instanceof RefreshType) {
            long now = System.currentTimeMillis();
            long timePassedFromLastUpdateInSeconds = (now - latestUpdate) / 1000;
            if (latestUpdate < 0 || timePassedFromLastUpdateInSeconds > 15) {
                logger.trace("Executing refresh command");
                try {
                    deviceState = api.getDeviceState(config.deviceId);
                    updateChannelsFromState(deviceState);
                } catch (IOException e) {
                    @Nullable
                    String errorMessage = e.getMessage();
                    if (errorMessage != null) {
                        if (errorMessage.contains(API_ERR_HTTP_401_UNAUTHORIZED)) {
                            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                                    "Incorrect local token for Bond Bridge.");
                            setBridgeOffline(ThingStatusDetail.CONFIGURATION_ERROR,
                                    "Incorrect local token for Bond Bridge.");
                        } else if (errorMessage.contains(API_ERR_HTTP_404_NOTFOUND)) {
                            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                                    "No Bond device found with the given device id.");
                        } else {
                            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
                        }
                    } else {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
                    }
                }
            } else {
                logger.trace("It has been less than 15s since the last update.  Please retry soon.");
            }
            return;
        }

        BondDeviceAction action = null;
        @Nullable
        Integer value = null;
        final BondDevice devInfo = Objects.requireNonNull(this.deviceInfo);
        switch (channelUID.getId()) {
            case CHANNEL_POWER:
                logger.trace("Power state command");
                api.executeDeviceAction(config.deviceId,
                        command == OnOffType.ON ? BondDeviceAction.TURN_ON : BondDeviceAction.TURN_OFF, null);
                break;

            case CHANNEL_COMMAND:
                logger.trace("{} command", command.toString());
                try {
                    action = BondDeviceAction.valueOf(command.toString());
                } catch (IllegalArgumentException e) {
                    logger.warn("Received unknown command {}.", command);
                    break;
                }

                if (devInfo.actions.contains(action)) {
                    api.executeDeviceAction(config.deviceId, action, null);
                } else {
                    logger.warn("Device {} does not support command {}.", config.deviceId, command);
                }
                break;

            case CHANNEL_FAN_POWER:
                logger.trace("Fan power state command");
                api.executeDeviceAction(config.deviceId,
                        command == OnOffType.ON ? BondDeviceAction.TURN_FP_FAN_ON : BondDeviceAction.TURN_FP_FAN_OFF,
                        null);
                break;

            case CHANNEL_FAN_SPEED:
                logger.trace("Fan speed command");
                if (command instanceof PercentType) {
                    if (devInfo.actions.contains(BondDeviceAction.SET_FP_FAN)) {
                        value = ((PercentType) command).intValue();
                        if (value == 0) {
                            action = BondDeviceAction.TURN_FP_FAN_OFF;
                            value = null;
                        } else {
                            action = BondDeviceAction.SET_FP_FAN;
                        }
                    } else {
                        BondDeviceProperties devProperties = this.deviceProperties;
                        if (devProperties != null) {
                            int maxSpeed = devProperties.maxSpeed;
                            value = (int) Math.ceil(((PercentType) command).intValue() * maxSpeed / 100);
                        } else {
                            value = 1;
                        }
                        if (value == 0) {
                            action = BondDeviceAction.TURN_OFF;
                            value = null;
                        } else {
                            action = BondDeviceAction.SET_SPEED;
                        }
                    }
                    logger.trace("Fan speed command with speed set as {}", value);
                    api.executeDeviceAction(config.deviceId, action, value);
                } else if (command instanceof IncreaseDecreaseType) {
                    logger.trace("Fan increase/decrease speed command");
                    api.executeDeviceAction(config.deviceId,
                            ((IncreaseDecreaseType) command == IncreaseDecreaseType.INCREASE
                                    ? BondDeviceAction.INCREASE_SPEED
                                    : BondDeviceAction.DECREASE_SPEED),
                            null);
                } else if (command instanceof OnOffType) {
                    logger.trace("Fan speed command {}", command);
                    if (devInfo.actions.contains(BondDeviceAction.TURN_FP_FAN_ON)) {
                        action = command == OnOffType.ON ? BondDeviceAction.TURN_FP_FAN_ON
                                : BondDeviceAction.TURN_FP_FAN_OFF;
                    } else if (devInfo.actions.contains(BondDeviceAction.TURN_ON)) {
                        action = command == OnOffType.ON ? BondDeviceAction.TURN_ON : BondDeviceAction.TURN_OFF;
                    }
                    if (action != null) {
                        api.executeDeviceAction(config.deviceId, action, null);
                    }
                } else {
                    logger.info("Unsupported command on fan speed channel");
                }
                break;

            case CHANNEL_FAN_BREEZE_STATE:
                logger.trace("Fan enable/disable breeze command");
                api.executeDeviceAction(config.deviceId,
                        command == OnOffType.ON ? BondDeviceAction.BREEZE_ON : BondDeviceAction.BREEZE_OFF, null);
                break;

            case CHANNEL_FAN_BREEZE_MEAN:
                // TODO(SRGDamia1): write array command fxn
                logger.trace("Support for fan breeze settings not yet available");
                break;

            case CHANNEL_FAN_BREEZE_VAR:
                // TODO(SRGDamia1): write array command fxn
                logger.trace("Support for fan breeze settings not yet available");
                break;

            case CHANNEL_FAN_DIRECTION:
                logger.trace("Fan direction command {}", command.toString());
                if (command instanceof StringType) {
                    api.executeDeviceAction(config.deviceId, BondDeviceAction.SET_DIRECTION,
                            command.toString().equals("winter") ? -1 : 1);
                }
                break;

            case CHANNEL_LIGHT_POWER:
                logger.trace("Fan light state command");
                api.executeDeviceAction(config.deviceId,
                        command == OnOffType.ON ? BondDeviceAction.TURN_LIGHT_ON : BondDeviceAction.TURN_LIGHT_OFF,
                        null);
                break;

            case CHANNEL_LIGHT_BRIGHTNESS:
                if (command instanceof PercentType) {
                    PercentType pctCommand = (PercentType) command;
                    value = pctCommand.intValue();
                    if (value == 0) {
                        action = BondDeviceAction.TURN_LIGHT_OFF;
                        value = null;
                    } else {
                        action = BondDeviceAction.SET_BRIGHTNESS;
                    }
                    logger.trace("Fan light brightness command with value of {}", value);
                    api.executeDeviceAction(config.deviceId, action, value);
                } else if (command instanceof IncreaseDecreaseType) {
                    logger.trace("Fan light brightness increase/decrease command {}", command);
                    api.executeDeviceAction(config.deviceId,
                            ((IncreaseDecreaseType) command == IncreaseDecreaseType.INCREASE
                                    ? BondDeviceAction.INCREASE_BRIGHTNESS
                                    : BondDeviceAction.DECREASE_BRIGHTNESS),
                            null);
                } else if (command instanceof OnOffType) {
                    logger.trace("Fan light brightness command {}", command);
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.TURN_LIGHT_ON : BondDeviceAction.TURN_LIGHT_OFF,
                            null);
                } else {
                    logger.info("Unsupported command on fan light brightness channel");
                }
                break;

            case CHANNEL_UP_LIGHT_ENABLE:
                api.executeDeviceAction(config.deviceId, command == OnOffType.ON ? BondDeviceAction.TURN_UP_LIGHT_ON
                        : BondDeviceAction.TURN_UP_LIGHT_OFF, null);
                break;

            case CHANNEL_UP_LIGHT_POWER:
                // To turn on the up light, we first have to enable it and then turn on the lights
                enableUpLight();
                api.executeDeviceAction(config.deviceId,
                        command == OnOffType.ON ? BondDeviceAction.TURN_LIGHT_ON : BondDeviceAction.TURN_LIGHT_OFF,
                        null);
                break;

            case CHANNEL_UP_LIGHT_BRIGHTNESS:
                enableUpLight();
                if (command instanceof PercentType) {
                    PercentType pctCommand = (PercentType) command;
                    value = pctCommand.intValue();
                    if (value == 0) {
                        action = BondDeviceAction.TURN_LIGHT_OFF;
                        value = null;
                    } else {
                        action = BondDeviceAction.SET_UP_LIGHT_BRIGHTNESS;
                    }
                    logger.trace("Fan up light brightness command with value of {}", value);
                    api.executeDeviceAction(config.deviceId, action, value);
                } else if (command instanceof IncreaseDecreaseType) {
                    logger.trace("Fan uplight brightness increase/decrease command {}", command);
                    api.executeDeviceAction(config.deviceId,
                            ((IncreaseDecreaseType) command == IncreaseDecreaseType.INCREASE
                                    ? BondDeviceAction.INCREASE_UP_LIGHT_BRIGHTNESS
                                    : BondDeviceAction.DECREASE_UP_LIGHT_BRIGHTNESS),
                            null);
                } else if (command instanceof OnOffType) {
                    logger.trace("Fan up light brightness command {}", command);
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.TURN_LIGHT_ON : BondDeviceAction.TURN_LIGHT_OFF,
                            null);
                } else {
                    logger.info("Unsupported command on fan up light brightness channel");
                }
                break;

            case CHANNEL_DOWN_LIGHT_ENABLE:
                api.executeDeviceAction(config.deviceId, command == OnOffType.ON ? BondDeviceAction.TURN_DOWN_LIGHT_ON
                        : BondDeviceAction.TURN_DOWN_LIGHT_OFF, null);
                break;

            case CHANNEL_DOWN_LIGHT_POWER:
                // To turn on the down light, we first have to enable it and then turn on the lights
                api.executeDeviceAction(config.deviceId, BondDeviceAction.TURN_DOWN_LIGHT_ON, null);
                api.executeDeviceAction(config.deviceId,
                        command == OnOffType.ON ? BondDeviceAction.TURN_LIGHT_ON : BondDeviceAction.TURN_LIGHT_OFF,
                        null);
                break;

            case CHANNEL_DOWN_LIGHT_BRIGHTNESS:
                enableDownLight();
                if (command instanceof PercentType) {
                    PercentType pctCommand = (PercentType) command;
                    value = pctCommand.intValue();
                    if (value == 0) {
                        action = BondDeviceAction.TURN_LIGHT_OFF;
                        value = null;
                    } else {
                        action = BondDeviceAction.SET_DOWN_LIGHT_BRIGHTNESS;
                    }
                    logger.trace("Fan down light brightness command with value of {}", value);
                    api.executeDeviceAction(config.deviceId, action, value);
                } else if (command instanceof IncreaseDecreaseType) {
                    logger.trace("Fan down light brightness increase/decrease command");
                    api.executeDeviceAction(config.deviceId,
                            ((IncreaseDecreaseType) command == IncreaseDecreaseType.INCREASE
                                    ? BondDeviceAction.INCREASE_DOWN_LIGHT_BRIGHTNESS
                                    : BondDeviceAction.DECREASE_DOWN_LIGHT_BRIGHTNESS),
                            null);
                } else if (command instanceof OnOffType) {
                    logger.trace("Fan down light brightness command {}", command);
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.TURN_LIGHT_ON : BondDeviceAction.TURN_LIGHT_OFF,
                            null);
                } else {
                    logger.debug("Unsupported command on fan down light brightness channel");
                }
                break;

            case CHANNEL_FLAME:
                if (command instanceof PercentType) {
                    PercentType pctCommand = (PercentType) command;
                    value = pctCommand.intValue();
                    if (value == 0) {
                        action = BondDeviceAction.TURN_OFF;
                        value = null;
                    } else {
                        action = BondDeviceAction.SET_FLAME;
                    }
                    logger.trace("Fireplace flame command with value of {}", value);
                    api.executeDeviceAction(config.deviceId, action, value);
                } else if (command instanceof IncreaseDecreaseType) {
                    logger.trace("Fireplace flame increase/decrease command");
                    api.executeDeviceAction(config.deviceId,
                            ((IncreaseDecreaseType) command == IncreaseDecreaseType.INCREASE
                                    ? BondDeviceAction.INCREASE_FLAME
                                    : BondDeviceAction.DECREASE_FLAME),
                            null);
                } else if (command instanceof OnOffType) {
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.TURN_ON : BondDeviceAction.TURN_OFF, null);
                } else {
                    logger.info("Unsupported command on flame channel");
                }
                break;

            case CHANNEL_ROLLERSHUTTER:
                logger.trace("Rollershutter command {}", command);
                if (command.equals(PercentType.ZERO)) {
                    command = UpDownType.UP;
                } else if (command.equals(PercentType.HUNDRED)) {
                    command = UpDownType.DOWN;
                }
                if (command == UpDownType.UP) {
                    action = BondDeviceAction.OPEN;
                } else if (command == UpDownType.DOWN) {
                    action = BondDeviceAction.CLOSE;
                } else if (command == StopMoveType.STOP) {
                    action = BondDeviceAction.HOLD;
                }
                if (action != null) {
                    api.executeDeviceAction(config.deviceId, action, null);
                }
                break;

            default:
                logger.info("Command {} on unknown channel {}, {}", command.toFullString(), channelUID.getId(),
                        channelUID.toString());
                return;
        }
    }

    private void enableUpLight() {
        Objects.requireNonNull(api).executeDeviceAction(config.deviceId, BondDeviceAction.TURN_UP_LIGHT_ON, null);
    }

    private void enableDownLight() {
        Objects.requireNonNull(api).executeDeviceAction(config.deviceId, BondDeviceAction.TURN_DOWN_LIGHT_ON, null);
    }

    @Override
    public void initialize() {
        logger.trace("Starting initialization for Bond device!");
        config = getConfigAs(BondDeviceConfiguration.class);
        fullyInitialized = false;
        disposed = false;

        // set the thing status to UNKNOWN temporarily
        updateStatus(ThingStatus.UNKNOWN);

        scheduler.execute(this::initializeThing);
    }

    @Override
    public synchronized void dispose() {
        logger.debug("Disposing thing handler for {}.", this.getThing().getUID());
        // Mark handler as disposed as soon as possible to halt updates
        disposed = true;
        fullyInitialized = false;

        final ScheduledFuture<?> pollingJob = this.pollingJob;
        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
        }
        this.pollingJob = null;
    }

    private void initializeThing() {
        if (!getBridgeAndAPI()) {
            return;
        }
        BondHttpApi api = this.api;
        if (api == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Bridge API not available");
            return;
        }

        try {
            logger.trace("Getting device information for {} ({})", config.deviceId, this.getThing().getLabel());
            deviceInfo = api.getDevice(config.deviceId);
            logger.trace("Getting device properties for {} ({})", config.deviceId, this.getThing().getLabel());
            deviceProperties = api.getDeviceProperties(config.deviceId);
        } catch (IOException e) {
            @Nullable
            String errorMessage = e.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains(API_ERR_HTTP_401_UNAUTHORIZED)) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "Incorrect local token for Bond Bridge.");
                    setBridgeOffline(ThingStatusDetail.CONFIGURATION_ERROR, "Incorrect local token for Bond Bridge.");
                    return;
                } else if (errorMessage.contains(API_ERR_HTTP_404_NOTFOUND)) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "No Bond device found with the given device id.");
                    return;
                }
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        }

        final BondDevice devInfo = this.deviceInfo;
        final BondDeviceProperties devProperties = this.deviceProperties;
        if (devInfo == null || devProperties == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Unable to get device properties from Bond");
            return;
        }

        // Anytime the configuration has changed or the binding has been updated,
        // recreate the thing to make sure all possible channels are available
        // NOTE: This will cause the thing to be disposed and re-initialized
        if (wasBindingUpdated() || wasThingUpdatedExternally(devInfo)) {
            recreateAllChannels(devInfo.type, devInfo.hash);
            return;
        }

        updateDevicePropertiesFromBond(devInfo, devProperties);

        deleteExtraChannels(devInfo.actions);

        startPollingJob();

        // Now we're online!
        updateStatus(ThingStatus.ONLINE);
        fullyInitialized = true;
        logger.debug("Finished initializing device!");
    }

    private void updateDevicePropertiesFromBond(BondDevice devInfo, BondDeviceProperties devProperties) {
        // Update all the thing properties based on the result
        Map<String, String> thingProperties = new HashMap<String, String>();
        thingProperties.put(PROPERTIES_BINDING_VERSION, CURRENT_BINDING_VERSION);
        thingProperties.put(CONFIG_DEVICE_ID, config.deviceId);
        logger.trace("Updating device name to {}", devInfo.name);
        thingProperties.put(PROPERTIES_DEVICE_NAME, devInfo.name);
        logger.trace("Updating other device properties for {} ({})", config.deviceId, this.getThing().getLabel());
        thingProperties.put(PROPERTIES_TEMPLATE_NAME, devInfo.template);
        thingProperties.put(PROPERTIES_MAX_SPEED, String.valueOf(devProperties.maxSpeed));
        thingProperties.put(PROPERTIES_TRUST_STATE, String.valueOf(devProperties.trustState));
        thingProperties.put(PROPERTIES_ADDRESS, String.valueOf(devProperties.addr));
        thingProperties.put(PROPERTIES_RF_FREQUENCY, String.valueOf(devProperties.freq));
        logger.trace("Saving properties for {} ({})", config.deviceId, this.getThing().getLabel());
        updateProperties(thingProperties);
    }

    private synchronized void recreateAllChannels(BondDeviceType currentType, String currentHash) {
        if (hasConfigurationError()) {
            logger.trace("Don't recreate channels, I've been disposed!");
            return;
        }

        logger.debug("Recreating all possible channels for a {} for {} ({})",
                currentType.getThingTypeUID().getAsString(), config.deviceId, this.getThing().getLabel());

        // Create a new configuration
        final Map<String, Object> map = new HashMap<>();
        map.put(CONFIG_DEVICE_ID, config.deviceId);
        map.put(CONFIG_LATEST_HASH, currentHash);
        Configuration newConfiguration = new Configuration(map);

        // Change the thing type back to itself to force all channels to be re-created from XML
        changeThingType(currentType.getThingTypeUID(), newConfiguration);
    }

    private synchronized void deleteExtraChannels(List<BondDeviceAction> currentActions) {
        logger.trace("Deleting channels based on the available actions");
        // Get the thing to edit
        ThingBuilder thingBuilder = editThing();

        // Now, look at the whole list of possible channels
        List<Channel> possibleChannels = this.getThing().getChannels();
        Set<String> availableChannelIds = new HashSet<>();

        for (BondDeviceAction action : currentActions) {
            String actionType = action.getChannelTypeId();
            if (actionType != null) {
                availableChannelIds.add(actionType);
                logger.trace(" Action: {}, Relevant Channel Type Id: {}", action.getActionId(), actionType);
            }
        }
        // Remove power channels if we have a dimmer channel for them;
        // the dimmer channel already covers the power case
        if (availableChannelIds.contains(CHANNEL_FAN_SPEED)) {
            availableChannelIds.remove(CHANNEL_POWER);
            availableChannelIds.remove(CHANNEL_FAN_POWER);
        }
        if (availableChannelIds.contains(CHANNEL_LIGHT_BRIGHTNESS)) {
            availableChannelIds.remove(CHANNEL_LIGHT_POWER);
        }
        if (availableChannelIds.contains(CHANNEL_UP_LIGHT_BRIGHTNESS)) {
            availableChannelIds.remove(CHANNEL_UP_LIGHT_POWER);
        }
        if (availableChannelIds.contains(CHANNEL_DOWN_LIGHT_BRIGHTNESS)) {
            availableChannelIds.remove(CHANNEL_DOWN_LIGHT_POWER);
        }
        if (availableChannelIds.contains(CHANNEL_FLAME)) {
            availableChannelIds.remove(CHANNEL_POWER);
        }

        for (Channel channel : possibleChannels) {
            if (availableChannelIds.contains(channel.getUID().getId())) {
                logger.trace("      ++++ Keeping: {}", channel.getUID().getId());
            } else {
                thingBuilder.withoutChannel(channel.getUID());
                logger.trace("      ---- Dropping: {}", channel.getUID().getId());
            }
        }

        // Add all the channels
        logger.trace("Saving the thing with extra channels removed");
        updateThing(thingBuilder.build());
    }

    public String getDeviceId() {
        return config.deviceId;
    }

    public synchronized void updateChannelsFromState(@Nullable BondDeviceState updateState) {
        if (hasConfigurationError()) {
            return;
        }

        if (updateState == null) {
            logger.debug("No state information provided to update channels with");
            return;
        }

        logger.debug("Updating channels from state for {} ({})", config.deviceId, this.getThing().getLabel());

        updateStatus(ThingStatus.ONLINE);

        updateState(CHANNEL_POWER, updateState.power == 0 ? OnOffType.OFF : OnOffType.ON);
        boolean fanOn;
        final BondDevice devInfo = this.deviceInfo;
        if (devInfo != null && devInfo.actions.contains(BondDeviceAction.TURN_FP_FAN_OFF)) {
            fanOn = updateState.fpfanPower != 0;
            updateState(CHANNEL_FAN_POWER, fanOn ? OnOffType.OFF : OnOffType.ON);
            updateState(CHANNEL_FAN_SPEED, new PercentType(updateState.fpfanSpeed));
        } else {
            fanOn = updateState.power != 0;
            int value = 1;
            BondDeviceProperties devProperties = this.deviceProperties;
            if (devProperties != null) {
                double maxSpeed = devProperties.maxSpeed;
                value = (int) (((double) updateState.speed / maxSpeed) * 100);
                logger.trace("Raw fan speed: {}, Percent: {}", updateState.speed, value);
            } else if (updateState.speed != 0 && this.getThing().getThingTypeUID().equals(THING_TYPE_BOND_FAN)) {
                logger.info("Unable to convert fan speed to a percent for {}!", this.getThing().getLabel());
            }
            updateState(CHANNEL_FAN_SPEED, formPercentType(fanOn, value));
        }
        updateState(CHANNEL_FAN_BREEZE_STATE, updateState.breeze[0] == 0 ? OnOffType.OFF : OnOffType.ON);
        updateState(CHANNEL_FAN_BREEZE_MEAN, new PercentType(updateState.breeze[1]));
        updateState(CHANNEL_FAN_BREEZE_VAR, new PercentType(updateState.breeze[2]));
        updateState(CHANNEL_FAN_DIRECTION,
                updateState.direction == 1 ? new StringType("summer") : new StringType("winter"));
        updateState(CHANNEL_FAN_TIMER, new DecimalType(updateState.timer));

        updateState(CHANNEL_LIGHT_POWER, updateState.light == 0 ? OnOffType.OFF : OnOffType.ON);
        updateState(CHANNEL_LIGHT_BRIGHTNESS, formPercentType(updateState.light != 0, updateState.brightness));

        updateState(CHANNEL_UP_LIGHT_ENABLE, updateState.upLight == 0 ? OnOffType.OFF : OnOffType.ON);
        updateState(CHANNEL_UP_LIGHT_POWER,
                (updateState.upLight == 1 && updateState.light == 1) ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_UP_LIGHT_BRIGHTNESS,
                formPercentType((updateState.upLight == 1 && updateState.light == 1), updateState.upLightBrightness));

        updateState(CHANNEL_DOWN_LIGHT_ENABLE, updateState.downLight == 0 ? OnOffType.OFF : OnOffType.ON);
        updateState(CHANNEL_DOWN_LIGHT_POWER,
                (updateState.downLight == 1 && updateState.light == 1) ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_DOWN_LIGHT_BRIGHTNESS, formPercentType(
                (updateState.downLight == 1 && updateState.light == 1), updateState.downLightBrightness));

        updateState(CHANNEL_FLAME, formPercentType(updateState.power != 0, updateState.flame));

        updateState(CHANNEL_ROLLERSHUTTER, formPercentType(updateState.open != 0, 100));
    }

    private PercentType formPercentType(boolean isOn, int value) {
        if (!isOn) {
            return PercentType.ZERO;
        } else {
            return new PercentType(value);
        }
    }

    private boolean hasConfigurationError() {
        final ThingStatusInfo statusInfo = getThing().getStatusInfo();
        return statusInfo.getStatus() == ThingStatus.OFFLINE
                && statusInfo.getStatusDetail() == ThingStatusDetail.CONFIGURATION_ERROR || disposed;
    }

    private synchronized boolean wasBindingUpdated() {
        // Check if the binding has been updated
        @Nullable
        String lastBindingVersion = this.getThing().getProperties().get(PROPERTIES_BINDING_VERSION);
        if (CURRENT_BINDING_VERSION.equals(lastBindingVersion)) {
            return false;
        }

        logger.debug("Bond Home binding has been updated.");
        logger.debug("Current version is {}, prior version was {}.", CURRENT_BINDING_VERSION, lastBindingVersion);

        // Update the thing with the new property value
        final Map<String, String> newProperties = editProperties();
        newProperties.put(PROPERTIES_BINDING_VERSION, CURRENT_BINDING_VERSION);

        updateProperties(newProperties);
        return true;
    }

    private synchronized boolean wasThingUpdatedExternally(BondDevice devInfo) {
        // Check if the Bond hash tree has changed
        final String lastDeviceConfigurationHash = config.lastDeviceConfigurationHash;
        boolean updatedHashTree = !devInfo.hash.equals(lastDeviceConfigurationHash);
        if (updatedHashTree) {
            logger.debug("Hash tree of device has been updated by Bond.");
            logger.debug("Current state is {}, prior state was {}.", devInfo.hash, lastDeviceConfigurationHash);
        }
        return updatedHashTree;
    }

    private boolean getBridgeAndAPI() {
        Bridge myBridge = this.getBridge();
        if (myBridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "No Bond bridge is associated with this Bond device");

            return false;
        } else {
            BondBridgeHandler myBridgeHandler = (BondBridgeHandler) myBridge.getHandler();
            if (myBridgeHandler != null) {
                this.api = myBridgeHandler.getBridgeAPI();
                return true;
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Cannot access API for Bridge associated with this Bond device");
                return false;
            }
        }
    }

    private void setBridgeOffline(ThingStatusDetail detail, String description) {
        Bridge myBridge = this.getBridge();
        if (myBridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "No Bond bridge is associated with this Bond device");
            logger.error("No Bond bridge is associated with this Bond device - cannot create device!");
            return;
        } else {
            BondBridgeHandler myBridgeHandler = (BondBridgeHandler) myBridge.getHandler();
            if (myBridgeHandler != null) {
                myBridgeHandler.setBridgeOffline(detail, description);
                return;
            }
        }
    }

    // Start polling for state
    private synchronized void startPollingJob() {
        final ScheduledFuture<?> pollingJob = this.pollingJob;
        if (pollingJob == null || pollingJob.isCancelled()) {
            Runnable pollingCommand = () -> {
                BondHttpApi api = this.api;
                if (api == null) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Bridge API not available");
                    return;
                }
                logger.trace("Polling for current state for {} ({})", config.deviceId, this.getThing().getLabel());
                try {
                    deviceState = api.getDeviceState(config.deviceId);
                    updateChannelsFromState(deviceState);
                } catch (IOException e) {
                    @Nullable
                    String errorMessage = e.getMessage();
                    if (errorMessage != null) {
                        if (errorMessage.contains(API_ERR_HTTP_401_UNAUTHORIZED)) {
                            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                                    "Incorrect local token for Bond Bridge.");
                            setBridgeOffline(ThingStatusDetail.CONFIGURATION_ERROR,
                                    "Incorrect local token for Bond Bridge.");
                        } else if (errorMessage.contains(API_ERR_HTTP_404_NOTFOUND)) {
                            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                                    "No Bond device found with the given device id.");
                        }
                    } else {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                    }
                }
            };
            this.pollingJob = scheduler.scheduleWithFixedDelay(pollingCommand, 60, 300, TimeUnit.SECONDS);
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE
                && getThing().getStatusInfo().getStatusDetail() == ThingStatusDetail.BRIDGE_OFFLINE) {
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);
            // restart the polling job when the bridge goes back online
            startPollingJob();
        } else if (bridgeStatusInfo.getStatus() == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            // stop the polling job when the bridge goes offline
            ScheduledFuture<?> pollingJob = this.pollingJob;
            if (pollingJob != null) {
                pollingJob.cancel(true);
                this.pollingJob = null;
            }
        }
    }
}
