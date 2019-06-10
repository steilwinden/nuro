package de.nuro.service;

import de.nuro.model.Cluster;
import de.nuro.model.Point;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ClusterService {

    private static final int RGB_WHITE = -1;

    public BufferedImage cropImageByLargestCluster(BufferedImage image) {

        List<Cluster> clusterList = new ArrayList<>();
        int imageHeight = image.getHeight();
        int imageWidth = image.getWidth();

        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {

                int rgb = image.getRGB(x, y);

                if (rgb == RGB_WHITE) {
                    Point point = new Point(x,y);
                    mergeNeighbours(point, clusterList, imageWidth);
                }
            }
        }
        print(clusterList, imageWidth, imageHeight);
        return image;
    }

    /**
     * Es gibt 4 Nachbarn, die untersucht werden müssen:
     *
     * 3 2 1
     * 4 X
     *
     */
    private void mergeNeighbours(Point point, List<Cluster> clusterList, int imageWidth) {

        int x = point.getX();
        int y = point.getY();

        if (y - 1 >= 0 && x + 1 < imageWidth) {
            Point neighbourPoint = new Point(x + 1, y - 1);
            mergePointIntoClusterList(point, neighbourPoint, clusterList);
        }
        if (y - 1 >= 0) {
            Point neighbourPoint = new Point(x, y - 1);
            mergePointIntoClusterList(point, neighbourPoint, clusterList);
        }
        if (y - 1 >= 0 && x - 1 >= 0) {
            Point neighbourPoint = new Point(x - 1, y - 1);
            mergePointIntoClusterList(point, neighbourPoint, clusterList);
        }
        if (x - 1 >= 0) {
            Point neighbourPoint = new Point(x - 1, y);
            mergePointIntoClusterList(point, neighbourPoint, clusterList);
        }
        System.out.println("clusterList: "+ Arrays.toString(clusterList.toArray()));
    }

    private void mergePointIntoClusterList(Point point, Point neighbourPoint, List<Cluster> clusterList) {

        Cluster cluster = findPointInClusterList(point, clusterList);
        if (cluster == null) {
            cluster = new Cluster(point);
            clusterList.add(cluster);
        }

        Cluster neighbourCluster = findPointInClusterList(neighbourPoint, clusterList);

        if (neighbourCluster != null && !cluster.contains(neighbourPoint)) {
            cluster.addAllPoints(neighbourCluster);
            clusterList.remove(neighbourCluster);
        }
    }

    /**
     * @return Cluster, when found; else null
     */
    private Cluster findPointInClusterList(Point point, List<Cluster> clusterList) {

        for(Cluster cluster : clusterList) {
            if (cluster.getPoints().contains(point)) {
                return cluster;
            }
        }
        return null;
    }

    private void print(List<Cluster> clusterList, int imageWidth, int imageHeight) {

        for (int y = 0; y < imageHeight; y++) {
            StringBuilder print = new StringBuilder();

            for (int x = 0; x < imageWidth; x++) {

                Point point = new Point(x, y);
                Cluster cluster = findPointInClusterList(point, clusterList);

                if (cluster != null) {
                    print.append(clusterList.indexOf(cluster));
                } else {
                    print.append(" ");
                }
            }
            System.out.println("cluster="+print);
        }
    }
}
