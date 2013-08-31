/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.example.recordablesoundpool;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.res.AssetManager;
import android.util.Log;

public class Recording implements Closeable {
    // debugging?
    protected boolean mDebug = false;
    protected String mDebugTag = "RecordableSoundPool:Recording";

    class RecordedEvent {
        long timestamp; // relative to start of recording
        String assetName;

        public RecordedEvent(long timestamp, String assetName) {
            this.timestamp = timestamp;
            this.assetName = assetName;
        }
    }

    protected List<RecordedEvent> mRecordedEvents = new ArrayList<RecordedEvent>();

    // current "clock" given in samples
    protected int mSamplesPerSecond;
    protected int mClock; // in samples

    // MixerGlue context, if one was already created
    protected long mHContext; // 0 if not yet created

    // Maps asset name to MixerGlue handle
    Map<String, Long> mHandleForAsset = new HashMap<String, Long>();

    // Duration of recording, in millis
    protected long mDuration = 0;

    Recording(boolean debugLog) {
        mDebug = debugLog;

        // set up Mixer Glue
        log("Setting up mixer glue.");
        mHContext = MixerGlue.start();
        log("Context handle: " + mHContext);
        mSamplesPerSecond = MixerGlue.getSamplesPerSecond(mHContext);
        log("Samples per second: " + mSamplesPerSecond);
        mClock = 0;
    }

    void setDuration(long duration) {
        mDuration = duration;
    }

    void addEvent(long timestamp, String assetName) {
        if (mRecordedEvents.size() > 0) {
            long lastEventTimestamp = mRecordedEvents.get(mRecordedEvents
                    .size() - 1).timestamp;
            if (timestamp < lastEventTimestamp) {
                throw new IllegalArgumentException(
                        "addEvent() must be called with events "
                                + "in chronological order.");
            }
        }
        mRecordedEvents.add(new RecordedEvent(timestamp, assetName));
        log("Added event timestamp=" + (timestamp * 0.001f) + " asset="
                + assetName);
    }

    public void enableDebugLogging(boolean enable) {
        mDebug = enable;
    }

    public float getDuration() {
        return mDuration * 0.001f;
    }

    public int getTotalSamples() {
        return (int) (mDuration * mSamplesPerSecond / 1000);
    }

    public int read(AssetManager mgr, byte[] buf, int offset, int length) {
        int stepSize = MixerGlue.getMinBufSize(mHContext);
        int bytesWritten = 0;

        while (length - bytesWritten > stepSize) {
            // is it time to start playing a sound?
            long timestamp = mClock * 1000 / mSamplesPerSecond;
            if (mRecordedEvents.size() > 0
                    && mRecordedEvents.get(0).timestamp <= timestamp) {
                // time to play next sound
                String asset = mRecordedEvents.get(0).assetName;
                log("Starting sound " + asset + " at timestamp "
                        + (timestamp * 0.001f));
                long handle;
                if (mHandleForAsset.containsKey(asset)) {
                    handle = mHandleForAsset.get(asset);
                    log("Reusing: handle " + handle + " => asset " + asset);
                } else {
                    log("Loading asset into new handle.");
                    handle = MixerGlue.load(mHContext, mgr, asset);
                    log("New: handle " + handle + " => asset " + asset);
                    if (handle == 0) {
                        throw new RuntimeException(
                                "MixerGlue failed to load asset " + asset);
                    }
                }

                MixerGlue.play(mHContext, handle);

                // remove this event from the queue
                mRecordedEvents.remove(0);
            }

            // mix stepSize samples
            bytesWritten += MixerGlue.mix(mHContext, buf,
                    offset + bytesWritten, stepSize);

            // advance clock
            mClock += stepSize;
        }

        return bytesWritten;
    }

    public void writeToFile(AssetManager mgr, FileOutputStream fos)
            throws IOException {
        byte[] buf = new byte[32768];
        int totalSamples = getTotalSamples();
        log("Writing to file, duration " + getDuration() + ", "
                + "total samples " + totalSamples);
        int samplesRead = 0, n;
        while (samplesRead < totalSamples) {
            n = read(mgr, buf, 0, buf.length);
            samplesRead += n;
            fos.write(buf, 0, n);
        }
    }

    public void writeToFile(AssetManager mgr, String fileName)
            throws IOException {
        log("Writing to file: " + fileName);
        FileOutputStream fos = new FileOutputStream(fileName);
        writeToFile(mgr, fos);
        fos.close();
    }

    public int getSamplesPerSecond() {
        return mSamplesPerSecond;
    }

    @Override
    public void close() {
        dispose();
    }

    public void dispose() {
        if (mHContext != 0)
            MixerGlue.end(mHContext);
        mHContext = 0;
    }

    protected void log(String msg) {
        if (mDebug)
            Log.d(mDebugTag, msg);
    }

    protected void warn(String msg) {
        Log.w(mDebugTag, msg);
    }
}
