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

import android.graphics.Canvas;

import com.google.example.squash.Ball;
import com.google.example.squash.SquashView;

public class FrameData {
    static final int MAX_BALL = 5;

    ArrayList<Ball> balls = new ArrayList<Ball>(5);
    double paddleY;
    long timestamp;
    int score;

    public FrameData() {
        for (int i = 0; i < 5; i++) {
            balls.add(new Ball());
        }
    }

    void copyData(SquashView sv, long timestamp) {
        paddleY = sv.paddleY;

        this.timestamp = timestamp;

        int count = sv.balls.size();

        for (int i = 0; i < count; i++) {
            balls.get(i).copy(sv.balls.get(i));
        }

        if (count < MAX_BALL) {
            balls.get(count).isActive = false;
        }

        score = sv.mScore;
    }

    void playback(SquashView sv) {
        sv.paddleY = paddleY;
        sv.balls = balls;
    }

    void renderImage(ReplayView rv, Canvas canvas) {
        int w = rv.getWidth();
        int h = rv.getHeight();

        double aspectRatio = 1.0 * w / h;
        rv.heightInPixels = h;

        // Not sure why I keep getting a null cliprect
        canvas.clipRect(0, 0, w, h);

        // Draw the bg
        rv.mRect.top = 0;
        rv.mRect.bottom = h;
        rv.mRect.left = 0;
        rv.mRect.right = w;

        rv.p.setColor(0xFF000000);
        canvas.drawRect(rv.mRect, rv.p);

        rv.p.setColor(0xffffffFF);

        // Draw the side
        rv.mRect.top = 0;
        rv.mRect.bottom = rv.sp(1);
        rv.mRect.left = rv.sp(aspectRatio - SquashView.WALL_THICKNESS);
        rv.mRect.right = rv.sp(aspectRatio);

        canvas.drawRect(rv.mRect, rv.p);

        // Draw top and bottom rails
        rv.mRect.top = 0;
        rv.mRect.bottom = rv.sp(SquashView.WALL_THICKNESS);
        rv.mRect.left = rv.sp(0.5);
        rv.mRect.right = rv.sp(aspectRatio);

        canvas.drawRect(rv.mRect, rv.p);

        rv.mRect.top = rv.sp(1 - SquashView.WALL_THICKNESS);
        rv.mRect.bottom = rv.sp(1);
        rv.mRect.left = rv.sp(0.5);
        rv.mRect.right = rv.sp(aspectRatio);

        canvas.drawRect(rv.mRect, rv.p);

        if (balls != null) {
            for (Ball ball : balls) {
                if (ball.isActive == false)
                    break;

                rv.mRect.top = rv.sp(ball.y - SquashView.BALL_RADIUS);
                rv.mRect.bottom = rv.sp(ball.y + SquashView.BALL_RADIUS);
                rv.mRect.left = rv.sp(ball.x - SquashView.BALL_RADIUS);
                rv.mRect.right = rv.sp(ball.x + SquashView.BALL_RADIUS);

                canvas.drawRect(rv.mRect, rv.p);

            }
        }

        rv.mRect.top = rv.sp(paddleY - SquashView.PADDLE_RADIUS);
        rv.mRect.bottom = rv.sp(paddleY + SquashView.PADDLE_RADIUS);
        rv.mRect.left = rv.sp(SquashView.PADDLE_DISTANCE
                - SquashView.BALL_RADIUS);
        rv.mRect.right = rv.sp(SquashView.PADDLE_DISTANCE
                + SquashView.BALL_RADIUS);
        canvas.drawRect(rv.mRect, rv.p);

        if (score > 0) {
            rv.p.setColor(0xAAFFAAAA);
            rv.p.setTextSize(rv.sp(0.08));
            canvas.drawText("Score: " + score, rv.sp(0.25), rv.sp(0.7), rv.p);
        }
    }

}
