package com.czispacialviewer.util;

public class RegionIntersection {

    public static boolean intersects(double ax, double ay, double aw, double ah,
                                     double bx, double by, double bw, double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    public static Intersection intersection(double ax, double ay, double aw, double ah,
                                            double bx, double by, double bw, double bh) {
        double x1 = Math.max(ax, bx);
        double y1 = Math.max(ay, by);
        double x2 = Math.min(ax + aw, bx + bw);
        double y2 = Math.min(ay + ah, by + bh);
        if (x2 <= x1 || y2 <= y1) {
            return null;
        }
        return new Intersection((int)Math.round(x1), (int)Math.round(y1),
                (int)Math.round(x2 - x1), (int)Math.round(y2 - y1));
    }

    public static class Intersection {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        public Intersection(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}
