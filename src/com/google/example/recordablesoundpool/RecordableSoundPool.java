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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

/**
 * Manages and plays audio resources for application (like SoundPool), but with
 * recording capabilities.
 * 
 * 1. Call load() to load your sounds. 2. Call prepare(). 3. Wait until you get
 * the callback indicating that the RecordableSoundPool is ready. 4. Call
 * startRecording(). 5. Play sounds with play(). 6. Call stopRecording(). 7. Get
 * the recording with getRecording()
 * 
 * Consume the recording and call its dispose() method when done with it. To
 * reuse, just call startRecording() and continue from there.
 */

public class RecordableSoundPool implements SoundPool.OnLoadCompleteListener {
    // maps sound ID to asset file
    @SuppressLint("UseSparseArrays")
    protected Map<Integer, String> mAssetForSoundId = new HashMap<Integer, String>();

    // are we recording?
    protected boolean mIsRecording = false;

    // if recording, time when when recording started
    protected long mStartTime;

    // recording object
    Recording mRecording = null;

    // the underlying SoundPool object we use to play sounds
    protected static final int MAX_STREAMS = 4;
    protected SoundPool mSoundPool = new SoundPool(MAX_STREAMS,
            AudioManager.STREAM_MUSIC, 0);

    // the listener we call when we're ready to start playing sounds
    OnReadyListener mReadyListener = null;

    // how many sounds did we start loading
    protected int mSoundsRequested = 0;

    // how many sounds we finished loading
    protected int mSoundsLoaded = 0;

    // are we ready to play? (finished loading)
    protected boolean mReady = false;

    // debugging?
    protected boolean mDebug = true;
    protected String mDebugTag = "RecordableSoundPool";

    // sound playback parameters (used when playing audio with the SoundPool)
    protected final static float DEFAULT_VOLUME = 1.0f;
    protected final static int DEFAULT_PRIORITY = 0;
    protected final static float DEFAULT_RATE = 1.0f;

    public interface OnReadyListener {
        public void onRecordableSoundPoolReady(RecordableSoundPool pool);
    }

    public RecordableSoundPool() {
        mSoundPool.setOnLoadCompleteListener(this);
    }

    public void enableDebugLogging(boolean enable, String logTag) {
        mDebug = enable;
        mDebugTag = logTag;
    }

    public void enableDebugLogging(boolean enable) {
        mDebug = enable;
    }

    public int load(Context ctx, String assetName, int priority) {
        if (mReady) {
            throw new IllegalStateException(
                    "Can't load a sound after RecordableSoundPool "
                            + "is ready to play. Load all sounds before calling prepare().");
        }
        try {
            log("Opening asset " + assetName);
            AssetFileDescriptor afd = ctx.getResources().getAssets()
                    .openFd(assetName);
            int soundId = mSoundPool.load(afd, priority);
            log("Sound ID for asset " + assetName + " is " + soundId);
            mAssetForSoundId.put(soundId, assetName);
            ++mSoundsRequested;
            log("Total # of sounds requested so far: " + mSoundsRequested);
            return soundId;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load asset: " + assetName);
        }
    }

    public void prepare(OnReadyListener readyListener) {
        if (mReady) {
            throw new IllegalStateException("Can't call prepare() twice.");
        }
        if (mSoundsLoaded >= mSoundsRequested) {
            readyListener.onRecordableSoundPoolReady(this);
        } else {
            mReadyListener = readyListener;
        }
    }

    public void startRecording() {
        if (!mReady) {
            throw new IllegalStateException(
                    "Can't call startRecording(). Not ready.");
        }
        mIsRecording = true;
        mStartTime = System.currentTimeMillis();
        if (mRecording != null)
            mRecording.dispose();
        mRecording = new Recording(mDebug);
        if (mDebug)
            mRecording.enableDebugLogging(true);
        log("Recording started, " + mStartTime);
    }

    public void play(int soundId) {
        if (!mReady) {
            warn("WARNING: Can't play sound " + soundId
                    + " because RecordableSoundPool ");
            warn("  is not ready. Please call prepare() and WAIT FOR THE CALLBACK");
            warn("   before attempting to play any sounds.");
            return;
        }
        if (!mAssetForSoundId.containsKey(soundId)) {
            throw new IllegalArgumentException("Invalid sound ID " + soundId);
        }
        if (mIsRecording) {
            long timestamp = System.currentTimeMillis() - mStartTime;
            log("Sound " + soundId + " played at " + timestamp / 1000.0f + "s");
            mRecording.addEvent(timestamp, mAssetForSoundId.get(soundId));
        }
        mSoundPool.play(soundId, DEFAULT_VOLUME, DEFAULT_VOLUME,
                DEFAULT_PRIORITY, 0, DEFAULT_RATE);
    }

    public void stopRecording() {
        if (!mReady) {
            throw new IllegalStateException(
                    "Can't call stopRecording(). Not ready.");
        }

        log("Recording stopped.");
        mIsRecording = false;
        mRecording.setDuration(System.currentTimeMillis() - mStartTime);
    }

    public Recording getRecording() {
        if (!mReady) {
            throw new IllegalStateException(
                    "Can't call getRecording(). Not ready.");
        }

        Recording result = mRecording;
        mRecording = null;
        mIsRecording = false;
        return result;
    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int soundId, int status) {
        log("Sound ID " + soundId + " finished loading, status " + status);
        if (status != 0) {
            throw new RuntimeException("Sound ID " + soundId
                    + " failed to load: " + status);
        }
        ++mSoundsLoaded;
        log("Sounds loaded: " + mSoundsLoaded + "/" + mSoundsRequested);
        if (mSoundsLoaded >= mSoundsRequested) {
            log("All sounds loaded! Invoking callback.");
            mReady = true;
            if (mReadyListener != null) {
                mReadyListener.onRecordableSoundPoolReady(this);
            }
            mReadyListener = null;
        }
    }

    protected void log(String msg) {
        if (mDebug) {
            Log.d(mDebugTag, msg);
        }
    }

    protected void warn(String msg) {
        Log.w(mDebugTag, msg);
    }
}
