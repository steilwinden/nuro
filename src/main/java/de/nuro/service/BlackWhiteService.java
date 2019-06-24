package de.nuro.service;

import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BlackWhiteService {

    // Sobald der THRESHOLD_FREQUENCY <= 2 ist, nehmen wir an, dass der grayLevel zur Zahl gehört.
    private static final int THRESHOLD_FREQUENCY = 2;

    public int calcThreshold(BufferedImage image) throws IOException {

        Map<Integer, Integer> grayLevelToFrequencyMap = calculateGrayLevelToFrequencyMap(image);

        int maxFrequencyKey = maxFrequencyKeyInHistogram(grayLevelToFrequencyMap);
        System.out.println(String.format("maxFrequencyKey: %d", maxFrequencyKey));
        System.out.println(String.format("frequency: %d", grayLevelToFrequencyMap.get(maxFrequencyKey)));

        int thresholdKey = maxFrequencyKey;
        while (thresholdKey > 0 && (!grayLevelToFrequencyMap.containsKey(thresholdKey)
                || grayLevelToFrequencyMap.get(thresholdKey) > THRESHOLD_FREQUENCY)) {
            thresholdKey--;
        }

        System.out.println(String.format("threshold: %d", thresholdKey));
//            printHistorgram(grayLevelToFrequencyMap);
        return thresholdKey;
    }

    private Map<Integer, Integer> calculateGrayLevelToFrequencyMap(BufferedImage image) {

        Map<Integer, Integer> grayLevelToFrequencyMap = new HashMap<>();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {

                int grayLevel = calculateGrayLevel(image, x, y);

                int frequency = 1;
                if (grayLevelToFrequencyMap.containsKey(grayLevel)) {
                    frequency = grayLevelToFrequencyMap.get(grayLevel) + 1;
                }
                grayLevelToFrequencyMap.put(grayLevel, frequency);
            }
        }
        return grayLevelToFrequencyMap;
    }

    private static int maxFrequencyKeyInHistogram(final Map<Integer, Integer> greyLevelToFrequencyMap) {

        int maxFrequency = 0;
        int maxFrequencyKey = 0;

        for (int grayLevel : greyLevelToFrequencyMap.keySet().stream().sorted().collect(Collectors.toList())) {

            Integer frequency = greyLevelToFrequencyMap.get(grayLevel);
            if (frequency > maxFrequency) {
                maxFrequency = frequency;
                maxFrequencyKey = grayLevel;
            }
        }
        return maxFrequencyKey;
    }

    private static void printHistorgram(final Map<Integer, Integer> greyLevelToFrequencyMap) {

        for (int grayLevel = 0; grayLevel < 256; grayLevel++) {
            StringBuilder occurences = new StringBuilder();
            if (greyLevelToFrequencyMap.containsKey(grayLevel)) {
                for (int i = 0; i < greyLevelToFrequencyMap.get(grayLevel); i++) {
                    occurences.append("+");
                }
            }
            System.out.println(String.format("grayLevel=%d: %s", grayLevel, occurences.toString()));
        }
    }

    public BufferedImage toBlackWhiteInverted(final BufferedImage image, final int thresholdBlack) {

        for (int x = 0; x < image.getWidth(); ++x) {
            for (int y = 0; y < image.getHeight(); ++y) {

                int grayLevel = calculateGrayLevel(image, x, y);
                int blackWhiteLevelInverted = grayLevel < thresholdBlack ? 255 : 0;
                int blackWhiteInverted =
                        (blackWhiteLevelInverted << 16) + (blackWhiteLevelInverted << 8) + blackWhiteLevelInverted;
                image.setRGB(x, y, blackWhiteInverted);
            }
        }

        return image;
    }

    private int calculateGrayLevel(final BufferedImage image, final int x, final int y) {

        int rgb = image.getRGB(x, y);
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;

        return (r + g + b) / 3;
    }
}
