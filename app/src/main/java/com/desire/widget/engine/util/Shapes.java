package com.desire.widget.engine.util;

import android.graphics.Path;
import android.graphics.RectF;

/**
 * Expressive tile shapes (the Material-3 "shape library" look): every shape is generated as a
 * polar curve r(θ) and scaled to fit the given bounds, so any of them can back a toggle, a battery
 * tile, a time tile or a weather chip.
 *
 * <p>Names: circle, squircle, rounded, stadium, cookie (12 soft scallops), wavy (8 waves), clover
 * (4 lobes), flower (6 petals), blob (organic), gem (soft diamond), pebble (soft triangle).
 */
public final class Shapes {
    public static final String[] ALL = {
            "circle", "squircle", "cookie", "wavy", "blob", "clover", "flower", "rounded", "gem", "pebble"
    };

    private static final int STEPS = 180;

    private Shapes() {}

    public static Path path(String name, RectF b) {
        Path p = new Path();
        String n = name != null ? name : "circle";
        switch (n) {
            case "rounded": {
                float r = Math.min(b.width(), b.height()) * 0.3f;
                p.addRoundRect(b, r, r, Path.Direction.CW);
                return p;
            }
            case "stadium":
            case "pill": {
                float r = Math.min(b.width(), b.height()) / 2f;
                p.addRoundRect(b, r, r, Path.Direction.CW);
                return p;
            }
            case "rect":
                p.addRect(b, Path.Direction.CW);
                return p;
            case "squircle":
                return superellipse(b, 4.2f);
            case "circle":
            case "oval":
                p.addOval(b, Path.Direction.CW);
                return p;
            default:
                return polar(n, b);
        }
    }

    /** |x|^n + |y|^n = 1 — the continuous-corner "app icon" square. */
    private static Path superellipse(RectF b, float n) {
        Path p = new Path();
        float cx = b.centerX(), cy = b.centerY(), rx = b.width() / 2f, ry = b.height() / 2f;
        for (int i = 0; i <= STEPS; i++) {
            double t = i * 2 * Math.PI / STEPS;
            double c = Math.cos(t), s = Math.sin(t);
            float x = cx + (float) (Math.signum(c) * Math.pow(Math.abs(c), 2.0 / n)) * rx;
            float y = cy + (float) (Math.signum(s) * Math.pow(Math.abs(s), 2.0 / n)) * ry;
            if (i == 0) p.moveTo(x, y);
            else p.lineTo(x, y);
        }
        p.close();
        return p;
    }

    private static double radius(String n, double t) {
        switch (n) {
            case "cookie":
                return 0.9 + 0.1 * Math.pow(Math.abs(Math.cos(t * 6)), 0.7);
            case "wavy":
                return 0.93 + 0.07 * Math.cos(t * 8);
            case "clover":
                return 0.78 + 0.22 * Math.pow(Math.abs(Math.cos(t * 2)), 0.6);
            case "flower":
                return 0.82 + 0.18 * Math.pow(Math.abs(Math.cos(t * 3)), 0.8);
            case "blob":
                return 0.88 + 0.07 * Math.sin(2 * t + 0.6) + 0.05 * Math.cos(3 * t + 1.3)
                        + 0.025 * Math.sin(5 * t + 0.2);
            case "gem": // soft diamond
                return 0.74 + 0.26 * Math.pow(Math.abs(Math.cos(t * 2)), 2.2);
            case "pebble": // soft triangle
                return 0.82 + 0.18 * Math.cos(t * 3 + Math.PI / 2);
            default:
                return 1.0;
        }
    }

    /** Samples r(θ), then scales so the shape exactly fills the bounds. */
    private static Path polar(String n, RectF b) {
        float[] xs = new float[STEPS];
        float[] ys = new float[STEPS];
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < STEPS; i++) {
            double t = i * 2 * Math.PI / STEPS;
            double r = radius(n, t);
            xs[i] = (float) (Math.sin(t) * r);
            ys[i] = (float) (-Math.cos(t) * r);
            minX = Math.min(minX, xs[i]);
            maxX = Math.max(maxX, xs[i]);
            minY = Math.min(minY, ys[i]);
            maxY = Math.max(maxY, ys[i]);
        }
        float sx = b.width() / Math.max(1e-3f, maxX - minX);
        float sy = b.height() / Math.max(1e-3f, maxY - minY);
        Path p = new Path();
        for (int i = 0; i < STEPS; i++) {
            float x = b.left + (xs[i] - minX) * sx;
            float y = b.top + (ys[i] - minY) * sy;
            if (i == 0) p.moveTo(x, y);
            else p.lineTo(x, y);
        }
        p.close();
        return p;
    }
}
