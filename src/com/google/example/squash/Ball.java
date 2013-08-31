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

public class Ball {

    public double x;
    public double y;

    public double velX;
    public double velY;

    public double BOUNCE_ACCEL = 1.15;

    public boolean isActive = true;

    public Ball() {
    }

    public Ball(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // Copy argument ball into this ball.
    public void copy(Ball ball) {
        this.x = ball.x;
        this.y = ball.y;
    }

    public boolean move(SquashView sv, long dt) {
        this.x += 1.0 * velX * (dt / 1000.0);
        this.y += 1.0 * velY * (dt / 1000.0);

        if (x > sv.aspectRatio - SquashView.WALL_THICKNESS) {
            velX *= -1;
            velX *= BOUNCE_ACCEL;
            x = sv.aspectRatio - SquashView.WALL_THICKNESS
                    - SquashView.BALL_RADIUS;

            sv.mRecordableSoundPool.play(sv.mBounceBackSoundId);
        }

        if (x > SquashView.PADDLE_DISTANCE - SquashView.WALL_THICKNESS
                && x < SquashView.PADDLE_DISTANCE + SquashView.WALL_THICKNESS) {
            if (y > sv.paddleY - SquashView.PADDLE_RADIUS
                    && y < sv.paddleY + SquashView.PADDLE_RADIUS) {
                velX *= -1;
                x = SquashView.PADDLE_DISTANCE + SquashView.WALL_THICKNESS;
                velY = (sv.paddleY - y) / SquashView.PADDLE_RADIUS * 0.013 * 30;

                sv.incrementScore(this);
            }
        }

        if (x < 0) {
            sv.mRecordableSoundPool.play(sv.mLostBallSoundId);
            return false;
        }

        if (y > 1 - SquashView.WALL_THICKNESS && x > SquashView.WALL_VSTART) {
            velY *= -1;
            y = 1 - SquashView.WALL_THICKNESS - SquashView.BALL_RADIUS;
            sv.mRecordableSoundPool.play(sv.mBounceSideSoundId);
        }

        if (y < SquashView.WALL_THICKNESS && x > SquashView.WALL_VSTART) {
            velY *= -1;
            y = SquashView.WALL_THICKNESS + SquashView.BALL_RADIUS;
            sv.mRecordableSoundPool.play(sv.mBounceSideSoundId);
        }

        return true;
    }
}
