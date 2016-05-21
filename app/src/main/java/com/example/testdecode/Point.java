package com.example.testdecode;

/**
 * Created by Ruiming Huang on 3/8/2016.
 */
public class Point implements Comparable<Point> {
    double centX;
    double centY;

    double distance;

    public Point(double centX, double centY) {
        this.centX = centX;
        this.centY = centY;
    }

    public double getCentX() {
        return centX;
    }

    public void setCentX(double centX) {
        this.centX = centX;
    }

    public double getCentY() {
        return centY;
    }

    public void setCentY(double centY) {
        this.centY = centY;
    }

    public void setDistanceTo(Point pb) {
        this.distance = Math.sqrt((pb.centX - this.centX) * (pb.centX - this.centX) + (pb.centY - this.centY) * (pb.centY - this.centY));
    }

    public int compareTo(Point pb) {
        if (this.distance < pb.distance)
            return -1;
        else if (this.distance == pb.distance)
            return 0;
        else
            return 1;
    }
}
