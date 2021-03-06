package de.nuro.model;

import java.util.ArrayList;
import java.util.List;

public class Cluster {

    private List<Point> points;

    public Cluster(Point point) {
        points = new ArrayList<>();
        points.add(point);
    }

    public void addCluster(Cluster cluster) {
        points.addAll(cluster.getPoints());
    }

    public List<Point> getPoints() {
        return points;
    }

    public boolean contains(Point point) {
        return points.contains(point);
    }

    @Override
    public String toString() {
        return "Cluster{" +
                "points=" + points +
                '}';
    }
}
