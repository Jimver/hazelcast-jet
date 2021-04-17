/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.impl.processor;

import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.spi.properties.HazelcastProperties;
import com.hazelcast.spi.properties.HazelcastProperty;

import java.text.MessageFormat;
import java.util.UUID;

public final class IMapStateHelper {
    // Properties
    public static final HazelcastProperty SNAPSHOT_STATE
            = new HazelcastProperty("state.snapshot", false);
    public static final HazelcastProperty PHASE_STATE
            = new HazelcastProperty("state.phase", false);
    public static final HazelcastProperty LIVE_STATE
            = new HazelcastProperty("state.live", false);
    public static final HazelcastProperty LIVE_STATE_ASYNC
            = new HazelcastProperty("state.live.async", false);
    public static final HazelcastProperty PHASE_BATCH
            = new HazelcastProperty("state.phase.batch", false);
    public static final HazelcastProperty PHASE_BATCH_WATERMARK
            = new HazelcastProperty("state.phase.batch.watermark", false);
    public static final HazelcastProperty WAIT_FOR_FUTURES
            = new HazelcastProperty("wait.for.futures", false);
    public static final HazelcastProperty BLOB_STATE
            = new HazelcastProperty("state.blob", true);

    // Booleans which control if IMap state is used or not
    private static boolean snapshotStateEnabled; // Toggle for snapshot state
    private static boolean phaseStateEnabled; // Toggle for phase (2) snapshot state
    private static boolean liveStateEnabled; // Toggle for live state
    private static boolean liveStateAsync; // Toggle for live state async
    private static boolean phaseStateBatchEnabled; // Toggle for batched phase state
    private static boolean phaseStateBatchWatermarkEnabled; // Toggle for batched phase state on watermark
    private static boolean waitForFuturesEnabled; // Toggle for wait for futures
    private static boolean blobStateEnabled; // Toggle for serialized blob state

    // Used to keep track if imap state boolean is already cached
    private static boolean snapshotStateEnabledCached;
    private static boolean phaseStateEnabledCached;
    private static boolean liveStateEnabledCached;
    private static boolean liveStateAsyncCached;
    private static boolean phaseStateBatchEnabledCached;
    private static boolean phaseStateBatchWatermarkEnabledCached;
    private static boolean waitForFuturesEnabledCached;
    private static boolean blobStateEnabledCached;

    // Private constructor to prevent instantiation
    private IMapStateHelper() {

    }

    private static boolean getBool(JetConfig config, HazelcastProperty property) {
        if (config == null) {
            return Boolean.parseBoolean(property.getDefaultValue());
        }
        return new HazelcastProperties(config.getProperties()).getBoolean(property);
    }

    /**
     * Helper method that gets whether the snapshot IMap state is enabled.
     *
     * @param config The JetConfig where the "state.snapshot" property should be "true" of "false"
     * @return True if the config says true, false if it says false.
     */
    public static boolean isSnapshotStateEnabled(JetConfig config) {
        if (!snapshotStateEnabledCached) {
            snapshotStateEnabled = getBool(config, SNAPSHOT_STATE);
            snapshotStateEnabledCached = true;
        }
        return snapshotStateEnabled;
    }

    public static boolean isPhaseStateEnabled(JetConfig config) {
        if (!phaseStateEnabledCached) {
            phaseStateEnabled = getBool(config, PHASE_STATE);
            phaseStateEnabledCached = true;
        }
        return phaseStateEnabled;
    }

    public static boolean isLiveStateEnabled(JetConfig config) {
        if (!liveStateEnabledCached) {
            liveStateEnabled = getBool(config, LIVE_STATE);
            liveStateEnabledCached = true;
        }
        return liveStateEnabled;
    }

    public static boolean isLiveStateAsync(JetConfig config) {
        if (!liveStateAsyncCached) {
            liveStateAsync = getBool(config, LIVE_STATE_ASYNC);
            liveStateAsyncCached = true;
        }
        return liveStateAsync;
    }

    public static boolean isBatchPhaseStateEnabled(JetConfig config) {
        if (!phaseStateBatchEnabledCached) {
            phaseStateBatchEnabled = getBool(config, PHASE_BATCH);
            phaseStateBatchEnabledCached = true;
        }
        return phaseStateBatchEnabled;
    }

    public static boolean isBatchPhaseStateWatermarkEnabled(JetConfig config) {
        if (!phaseStateBatchWatermarkEnabledCached) {
            phaseStateBatchWatermarkEnabled = getBool(config, PHASE_BATCH);
            phaseStateBatchWatermarkEnabledCached = true;
        }
        return phaseStateBatchWatermarkEnabled;
    }

    public static boolean isWaitForFuturesEnabled(JetConfig config) {
        if (!waitForFuturesEnabledCached) {
            waitForFuturesEnabled = getBool(config, WAIT_FOR_FUTURES);
            waitForFuturesEnabledCached = true;
        }
        return waitForFuturesEnabled;
    }

    public static boolean isBlobStateEnabled(JetConfig config) {
        if (!blobStateEnabledCached) {
            blobStateEnabled = getBool(config, BLOB_STATE);
            blobStateEnabledCached = true;
        }
        return blobStateEnabled;
    }

    public static boolean isSnapshotOrPhaseEnabled(JetConfig config) {
        return isSnapshotStateEnabled(config) || isPhaseStateEnabled(config);
    }

    public static String getBenchmarkIMapTimesListName(String jobName) {
        return String.format("benchmark-imap-%s", jobName);
    }

    public static String getBenchmarkPhase1TimesListName(String jobName) {
        return String.format("benchmark-phase1-%s", jobName);
    }

    public static String getBenchmarkPhase2TimesListName(String jobName) {
        return String.format("benchmark-phase2-%s", jobName);
    }

    /**
     * Helper method for live state IMap name.
     *
     * @param vertexName The name of the transform
     * @return The live state IMap name for the given transform
     */
    public static String getLiveStateImapName(String vertexName) {
        return vertexName;
    }

    /**
     * Helper method for snapshot state IMap name.
     *
     * @param vertexName The name of the transform
     * @return The snapshot state IMap name for the given transform
     */
    public static String getSnapshotMapName(String vertexName) {
        return MessageFormat.format("old_snapshot_{0}", vertexName);
    }

    /**
     * Helper method for phase (2) snapshot state IMap name.
     *
     * @param vertexName The name of the transform
     * @return The phase snapshot state IMap name for the given transform
     */
    public static String getPhaseSnapshotMapName(String vertexName) {
        return MessageFormat.format("snapshot_{0}", vertexName);
    }

    public static String getSnapshotIdName(String jobName) {
        return MessageFormat.format("ssid-{0}", jobName);
    }

    public static String clusterCountdownLatchHelper(String jobName) {
        return "cdl-" + jobName;
    }

    public static String memberCountdownLatchHelper(UUID memberName, String jobName) {
        return String.format("cdl-%s-%s", memberName.toString(), jobName);
    }

}
