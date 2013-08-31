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

package com.google.example.squash.replay;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.google.example.squash.SquashView;

public class ReplayView extends View {

    public static int MAX_FRAMES = 60 * 60;

    ArrayList<FrameData> frameData = new ArrayList<FrameData>(600);

    private int lastPointer = 0;
    public boolean mIsReplaying;
    private long mReplayTime;
    private long mLastTime;

    public Rect mRect = new Rect();
    Paint p = new Paint();

    public int framePointer = 0;

    public ReplayView(Context context) {
        super(context);
    }

    public ReplayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        for (int i = 0; i < MAX_FRAMES; i++) {
            FrameData fd = new FrameData();
            frameData.add(fd);
        }
    }

    public void recordFrame(SquashView sv) {
        if (framePointer == MAX_FRAMES) {
            // No room for more frames, and you should not
            // allocate new ones on a UI thread.
            return;
        }

        FrameData fd = frameData.get(framePointer);
        fd.copyData(sv, System.currentTimeMillis());
        frameData.add(fd);

        framePointer++;
    }

    private long getStartTime() {
        return frameData.get(0).timestamp;
    }

    public void setReplaying(boolean val) {
        mIsReplaying = val;

        if (val) {
            if (framePointer == 0) {
                Log.e("ReplayView", "You are replaying a zero replay.");
                setReplaying(false);
                return;
            }

            mReplayTime = getStartTime();
            mLastTime = System.currentTimeMillis();

            invalidate();
        }
    }

    public void reset() {
        framePointer = 0;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long newTime = System.currentTimeMillis();

        mReplayTime += newTime - mLastTime;

        mLastTime = newTime;

        if (mIsReplaying) {
            renderAtTime(mReplayTime, canvas);

            invalidate();
        }
    }

    int heightInPixels;

    // Convert back from screenspace
    public int sp(double screenSpaceCoordinate) {
        return (int) Math.round(screenSpaceCoordinate * heightInPixels);
    }

    void renderAtTime(long time, Canvas cv) {
        if (framePointer == 0)
            return;

        int endpoint = frameData.size() - 1;
        for (int i = lastPointer; i < frameData.size() - 1; i++) {

            if (frameData.get(i).timestamp >= time
                    && time < frameData.get(i + 1).timestamp) {
                frameData.get(i).renderImage(this, cv);
                lastPointer = i;
                return;
            }
        }

        if (time > frameData.get(endpoint).timestamp) {
            frameData.get(endpoint).renderImage(this, cv);
            lastPointer = 0;
            mIsReplaying = false;
            Log.e("ReviewView", "Replay over");
            return;
        }

        // You must be asking for these out of order, so we'll try again.
        if (lastPointer > 0) {
            lastPointer = 0;
            renderAtTime(time, cv);
        }

        if (endpoint > 0) {
            Log.e("ReplayView",
                    "No frame found: Latest: "
                            + frameData.get(endpoint).timestamp + " to " + time
                            + " " + (frameData.get(endpoint).timestamp - time)
                            + " " + (frameData.get(0).timestamp - time));
        }
    }
}
