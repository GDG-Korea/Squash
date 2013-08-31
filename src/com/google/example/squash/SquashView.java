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

package com.google.example.squash;

import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.google.example.recordablesoundpool.RecordableSoundPool;
import com.google.example.recordablesoundpool.Recording;
import com.google.example.squash.replay.ReplayView;

public class SquashView extends View implements OnTouchListener {

    private final Rect mRect = new Rect();

    public static final double WALL_VSTART = 0.5;

    public static final int STATE_PAUSED = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_GAME_OVER = 2;
    public static final double PADDLE_DISTANCE = 0.75;
    public static final double WALL_THICKNESS = 0.05;
    public static final double RANDOM_Y_SPEED = 0.01 * 30;
    public static final double SLOW_SPEED = 0.011 * 60;
    public static final double BALL_RADIUS = 0.025;
    public static final double PADDLE_RADIUS = 0.12;
    public static final double PADDLE_MAX_SPEED = 0.035 * 30;
    public static final double LAUNCH_SPEED_BOOST = 0.009 * 30;

    public int mState = STATE_GAME_OVER;
    public int mScore = 0;
    public int mLaunchScore = 0;

    public ArrayList<Ball> balls = new ArrayList<Ball>();
    public ArrayList<Ball> livingBalls = new ArrayList<Ball>();

    public double mLaunchSpeed = 0;

    public double paddleY = 0.5;
    public double paddleTargetY = 0.5;

    public int mBounceSideSoundId;
    public int mBounceBackSoundId;
    public int mBouncePaddleId;
    public int mLostBallSoundId;
    public int mSplitSoundId;
    public int mLaunchSoundId;

    public ReplayView mReplayView;

    public Context mActivity;
    boolean mSoundReady = false;

    public static final String TAG = "SquashView";

    public RecordableSoundPool mRecordableSoundPool;

    public SquashView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnTouchListener(this);

        mRecordableSoundPool = new RecordableSoundPool();
        mRecordableSoundPool.enableDebugLogging(true);

        mBounceSideSoundId = mRecordableSoundPool.load(getContext(),
                "bounce_side.wav", 0);

        mBounceBackSoundId = mRecordableSoundPool.load(getContext(),
                "bounce_back_wall.wav", 0);

        mBouncePaddleId = mRecordableSoundPool.load(getContext(),
                "bounce_paddle.wav", 0);

        mSplitSoundId = mRecordableSoundPool.load(getContext(), "split.wav", 0);

        mLostBallSoundId = mRecordableSoundPool.load(getContext(),
                "lost_ball.wav", 0);

        mLaunchSoundId = mRecordableSoundPool
                .load(getContext(), "split.wav", 0);

        mRecordableSoundPool.prepare(new RecordableSoundPool.OnReadyListener() {
            @Override
            public void onRecordableSoundPoolReady(RecordableSoundPool pool) {
                Log.d(TAG, "RecordableSoundPool is ready.");
                mSoundReady = true;
            }
        });

        mActivity = context;

    }

    public SquashView(Context context) {
        super(context);

        mState = STATE_GAME_OVER;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        invalidate();
    }

    Paint p = new Paint();

    Boolean keepAnimating = false;

    public void setAnimating(Boolean val) {

        mReplayView = (ReplayView) ((SquashActivity) mActivity)
                .findViewById(R.id.replayView);

        if (val && !keepAnimating) {
            mLastFrameTime = System.currentTimeMillis();
        }
        keepAnimating = val;
        if (val) {
            invalidate();
        }
    }

    double aspectRatio;
    double heightInPixels;

    // Convert back from screenspace
    public int sp(double screenSpaceCoordinate) {
        return (int) Math.round(screenSpaceCoordinate * heightInPixels);
    }

    long mLastFrameTime;

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long currentTime = System.currentTimeMillis();
        long dt = currentTime - mLastFrameTime;
        mLastFrameTime = currentTime;

        int w = this.getWidth();
        int h = this.getHeight();

        aspectRatio = 1.0 * w / h;
        heightInPixels = h;

        // Not sure why I keep getting a null cliprect
        canvas.clipRect(0, 0, w, h);

        // Draw the bg
        mRect.top = 0;
        mRect.bottom = h;
        mRect.left = 0;
        mRect.right = w;

        p.setColor(0xFF000000);
        canvas.drawRect(mRect, p);

        p.setColor(0xffffffFF);

        if (!mSoundReady) {
            p.setColor(0xffffffFF);
            p.setTextSize(60);
            canvas.drawText("Please wait...", 50, canvas.getHeight() / 2, p);
            invalidate();
            return;
        }

        // Draw the side
        mRect.top = 0;
        mRect.bottom = sp(1);
        mRect.left = sp(aspectRatio - WALL_THICKNESS);
        mRect.right = sp(aspectRatio);

        canvas.drawRect(mRect, p);

        // Draw top and bottom rails
        mRect.top = 0;
        mRect.bottom = sp(WALL_THICKNESS);
        mRect.left = sp(0.5);
        mRect.right = sp(aspectRatio);

        canvas.drawRect(mRect, p);

        mRect.top = sp(1 - WALL_THICKNESS);
        mRect.bottom = sp(1);
        mRect.left = sp(0.5);
        mRect.right = sp(aspectRatio);

        canvas.drawRect(mRect, p);

        double diff = paddleTargetY - paddleY;

        paddleY += diff;

        paddleY = Math.max(0 + PADDLE_RADIUS + WALL_THICKNESS, paddleY);
        paddleY = Math.min(1.0 - PADDLE_RADIUS - WALL_THICKNESS, paddleY);

        if (balls != null) {
            for (Ball ball : balls) {
                mRect.top = sp(ball.y - BALL_RADIUS);
                mRect.bottom = sp(ball.y + BALL_RADIUS);
                mRect.left = sp(ball.x - BALL_RADIUS);
                mRect.right = sp(ball.x + BALL_RADIUS);

                canvas.drawRect(mRect, p);

                if (ball.move(this, dt)) {
                    livingBalls.add(ball);
                }

                // XXX Render trail here?
            }
        }

        balls.clear();
        balls.addAll(livingBalls);
        livingBalls.clear();

        if (balls.size() == 0 && mState == STATE_RUNNING) {
            endGame();
        }

        mRect.top = sp(paddleY - PADDLE_RADIUS);
        mRect.bottom = sp(paddleY + PADDLE_RADIUS);
        mRect.left = sp(PADDLE_DISTANCE - BALL_RADIUS);
        mRect.right = sp(PADDLE_DISTANCE + BALL_RADIUS);
        canvas.drawRect(mRect, p);

        if (mState == STATE_GAME_OVER) {
            p.setColor(0xAAAAAAAA);
            p.setTextSize(sp(0.07));
            canvas.drawText("Touch to start", sp(0.25), sp(0.5), p);
        }

        if (mScore > 0) {
            p.setColor(0xAAAAAAFF);
            p.setTextSize(sp(0.08));
            canvas.drawText("Score: " + mScore, sp(0.25), sp(0.7), p);
        }

        if (SquashActivity.challengeScore > 0) {
            p.setColor(0xAAAAAAFF);
            p.setTextSize(sp(0.08));
            canvas.drawText("Beat this: " + SquashActivity.challengeScore,
                    sp(0.18), sp(0.35), p);
        }

        if (keepAnimating) {
            invalidate();
        }

        if (mReplayView != null && mState == STATE_RUNNING
                && !mReplayView.mIsReplaying) {
            mReplayView.recordFrame(this);
        }
    }

    public void splitBall(Ball ball) {
        Ball p = new Ball(ball.x, ball.y);
        p.velX = mLaunchSpeed;
        p.velY = (Math.random() - 0.5) * RANDOM_Y_SPEED;

        mLaunchSpeed += LAUNCH_SPEED_BOOST;

        mRecordableSoundPool.play(mSplitSoundId);

        livingBalls.add(p);
    }

    public void serve() {
        Ball p = new Ball(PADDLE_DISTANCE + 0.1, 0.5);
        p.velX = mLaunchSpeed;
        p.velY = (Math.random() - 0.5) * RANDOM_Y_SPEED;

        mLaunchSpeed += LAUNCH_SPEED_BOOST;

        mReplayView.reset();
        mReplayView.setReplaying(false);
        mRecordableSoundPool.startRecording();
        mRecordableSoundPool.play(mLaunchSoundId);

        balls.add(p);

    }

    @Override
    public boolean onTouch(View arg0, MotionEvent arg1) {

        int action = arg1.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_MOVE:
            paddleTargetY = arg1.getY() / this.getHeight();
            break;
        case MotionEvent.ACTION_DOWN:
            if (!mSoundReady)
                return true;
            if (mState == STATE_GAME_OVER) {
                mState = STATE_RUNNING;

                mScore = 0;
                mLaunchScore = 3;
                mLaunchSpeed = SLOW_SPEED;

                balls = new ArrayList<Ball>();
                serve();
                ((SquashActivity) getContext()).onGameStart(this);

                return true;
            }
        }

        return true;
    }

    // Important for scoring and achievements
    public void incrementScore(Ball ball) {
        mScore++;

        if (mScore == mLaunchScore) {
            mLaunchScore = mScore + 5;
            splitBall(ball);
        } else {
            mRecordableSoundPool.play(mBouncePaddleId);
        }
    }

    public void endGame() {
        mState = STATE_GAME_OVER;

        mRecordableSoundPool.stopRecording();

        Log.d(TAG, "Writing getting recording.");

        Recording r = mRecordableSoundPool.getRecording();

        Log.d(TAG, "Writing out record.");

        String root = Environment.getExternalStorageDirectory().toString();
        try {
            r.writeToFile(mActivity.getAssets(), root + "/out.raw"); // root

            Log.d(TAG, "Saved.");
            // +
            // "/test/sound.raw");
        } catch (IOException e) {
            Log.e(TAG, "Failed to write sound.raw");
            e.printStackTrace();
        }
        ((SquashActivity) getContext()).onGameStop(this);
    }
}
