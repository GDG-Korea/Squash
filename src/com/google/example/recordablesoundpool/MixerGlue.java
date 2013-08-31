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

import android.util.Log;

class MixerGlue {
    static {
        System.loadLibrary("mixerglue");
        Log.d("MixerGlue", "Initializing native lib.");
        init();
        Log.d("MixerGlue", "Done initializing native library.");
    }

    static native void init();

    static native long start();

    static native long load(long context, Object asset_manager,
            String sound_asset_name);

    static native void play(long context, long handle);

    static native int getMinBufSize(long context);

    static native int getSamplesPerSecond(long context);

    static native int mix(long context, byte[] buf, int offset, int length);

    static native void end(long context);
}
