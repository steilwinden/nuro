package de.nuro.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BlackWhiteService {

    public int calcThreshold(final byte[] bytes) throws IOException {

        // sobald der frequencyThreshold <= 2 ist, nehmen wir an, dass der grayLevel zur Zahl gehört.
        final int frequencyThreshold = 2;

        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {

            Map<Integer, Integer> grayLevelToFrequencyMap = calculateGrayLevelToFrequencyMap(bis);

            int maxFrequencyKey = maxFrequencyKeyInHistogram(grayLevelToFrequencyMap);
            System.out.println(String.format("maxFrequencyKey: %d", maxFrequencyKey));
            System.out.println(String.format("frequency: %d", grayLevelToFrequencyMap.get(maxFrequencyKey)));

            int thresholdKey = maxFrequencyKey;
            while (thresholdKey > 0 && (!grayLevelToFrequencyMap.containsKey(thresholdKey)
                    || grayLevelToFrequencyMap.get(thresholdKey) > frequencyThreshold)) {
                thresholdKey--;
            }

            System.out.println(String.format("threshold: %d", thresholdKey));
            printHistorgram(grayLevelToFrequencyMap);
            return thresholdKey;
        }
    }

    private Map<Integer, Integer> calculateGrayLevelToFrequencyMap(final ByteArrayInputStream bis) throws IOException {

        BufferedImage image = ImageIO.read(bis);

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
            String occurences = "";
            if (greyLevelToFrequencyMap.containsKey(grayLevel)) {
                for (int i = 0; i < greyLevelToFrequencyMap.get(grayLevel); i++) {
                    occurences += "+";
                }
            }
            System.out.println(String.format("grayLevel=%d: %s", grayLevel, occurences));
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

        int grayLevel = (r + g + b) / 3;
        return grayLevel;
    }
}
