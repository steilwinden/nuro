package de.nuro.service;

import de.nuro.model.Cluster;
import de.nuro.model.Point;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ClusterService {

    private static final int RGB_WHITE = -1;
    private static final int NOF_PIXEL_FRAME = 2;

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
        Cluster largestCluster = clusterList.stream().max(Comparator.comparing(e -> e.getPoints().size())).get();
        return cropImageByCluster(image, largestCluster, imageWidth, imageHeight);
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
    }

    private void mergePointIntoClusterList(Point point, Point neighbourPoint, List<Cluster> clusterList) {

        Cluster cluster = findPointInClusterList(point, clusterList);
        if (cluster == null) {
            cluster = new Cluster(point);
            clusterList.add(cluster);
        }

        Cluster neighbourCluster = findPointInClusterList(neighbourPoint, clusterList);

        if (neighbourCluster != null && !cluster.contains(neighbourPoint)) {
            cluster.addCluster(neighbourCluster);
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
            System.out.println(String.format("cluster(%02d)=%s",y,print));
        }
    }

    private BufferedImage cropImageByCluster(BufferedImage image, Cluster cluster, int imageWidth, int imageHeight) {

        int x1 = imageWidth;
        int x2 = 0;
        int y1 = imageHeight;
        int y2 = 0;

        for (Point point : cluster.getPoints()) {
            if (point.getX() < x1) {
                x1 = point.getX();
            }
            if (point.getX() > x2) {
                x2 = point.getX();
            }
            if (point.getY() < y1) {
                y1 = point.getY();
            }
            if (point.getY() > y2) {
                y2 = point.getY();
            }
        }

        int halfLength = Math.round(Math.max(x2 - x1, y2 - y1) / 2);
        int centerX = Math.round((x2 - x1) / 2) + x1;
        int centerY = Math.round((y2 - y1) / 2) + y1;

        int startX = centerX - halfLength;
        int startY = centerY - halfLength;

        if (startX < 0 || startX + 2*halfLength > imageWidth || startY < 0 || startY + 2*halfLength > imageHeight) {
            throw new IllegalArgumentException("Die Zahl befindet sich zu nah am Rand des Bildes. "
                    +"startX="+startX+" startY="+startY);
        }

        System.out.println("startX: "+startX);
        System.out.println("startY: "+startY);
        System.out.println("halfLength: "+halfLength);

        BufferedImage subImage = image.getSubimage(startX, startY,
                2*halfLength + NOF_PIXEL_FRAME, 2*halfLength + NOF_PIXEL_FRAME);
        print(subImage);
        return subImage;
    }

    private void print(BufferedImage image) {

        for (int y = 0; y < image.getHeight(); y++) {

            StringBuilder print = new StringBuilder();
            for (int x = 0; x < image.getWidth(); x++) {

                int rgb = image.getRGB(x, y);
                if (rgb == RGB_WHITE) {
                    print.append("0");
                }
                else {
                    print.append(" ");
                }
            }
            System.out.println(String.format("subimage(%02d)=%s", y, print));
        }
    }
}
