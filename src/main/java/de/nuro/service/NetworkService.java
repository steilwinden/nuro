package de.nuro.service;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;
import org.springframework.stereotype.Service;

/**
 * A Simple Multi Layered Perceptron (MLP) applied to digit classification for
 * the MNIST Dataset (http://yann.lecun.com/exdb/mnist/).
 *
 * This file builds one input layer and one hidden layer.
 *
 * The input layer has input dimension of numRows*numColumns where these
 * variables indicate the number of vertical and horizontal pixels in the image.
 * This layer uses a rectified linear unit (relu) activation function. The
 * weights for this layer are initialized by using Xavier initialization
 * (https://prateekvjoshi.com/2016/03/29/understanding-xavier-initialization-in-deep-neural-networks/)
 * to avoid having a steep learning curve. This layer will have 1000 output
 * signals to the hidden layer.
 *
 * The hidden layer has input dimensions of 1000. These are fed from the input
 * layer. The weights for this layer is also initialized using Xavier
 * initialization. The activation function for this layer is a softmax, which
 * normalizes all the 10 outputs such that the normalized sums add up to 1. The
 * highest of these normalized values is picked as the predicted class.
 *
 */
@Service
public class NetworkService {

    public static final String NURO_FOLDER = "/home/dennis/Development/nuro";
    public static final String ADHOC_FOLDER = NetworkService.NURO_FOLDER + "/adhoc";
    private static final String MNIST_PNG_FOLDER = NURO_FOLDER + "/mnist_png";
    private static final String TRAINED_MODEL_ZIP = NURO_FOLDER + "/trained_mnist_model.zip";

    // image information
    // 28 * 28 grayscale
    // grayscale implies single channel
    private int height = 28;
    private int width = 28;
    private int channels = 1;
    private int rngseed = 123;
    private Random randNumGen = new Random(rngseed);
    private int batchSize = 128;
    private int outputNum = 10;
    private int numEpochs = 15;
    double rate = 0.0015; // learning rate

    public int guessNumber() throws IOException {

        File trainedModelFile = new File(TRAINED_MODEL_ZIP);

        if (!trainedModelFile.exists()) {

            // Build Our Neural Network
            System.out.println("BUILD MODEL");

            MultiLayerConfiguration conf = buildModelConfTwoLayer();

            MultiLayerNetwork model = new MultiLayerNetwork(conf);
            model.init();
            model.setListeners(new ScoreIterationListener(10));

            DataSetIterator trainIter = createDataSetIteratorFromFile(new File(MNIST_PNG_FOLDER + "/training"));

            System.out.println("TRAIN MODEL");
            for (int i = 0; i < numEpochs; i++) {
                model.fit(trainIter);
            }

            // boolean save Updater
            boolean saveUpdater = false;

            // ModelSerializer needs modelname, saveUpdater, Location
            ModelSerializer.writeModel(model, trainedModelFile, saveUpdater);

            System.out.println("EVALUATE MODEL");
            DataSetIterator testIter = createDataSetIteratorFromFile(new File(MNIST_PNG_FOLDER + "/testing"));

            // Create Eval object with 10 possible classes
            Evaluation eval = new Evaluation(outputNum);

            // Evaluate the network
            while (testIter.hasNext()) {
                DataSet next = testIter.next();
                INDArray output = model.output(next.getFeatures());
                eval.eval(next.getLabels(), output);
            }

            System.out.println(eval.stats());
        }

        System.out.println("LOAD TRAINED MODEL");
        MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(trainedModelFile);

        DataSetIterator adhocIter = createDataSetIteratorFromFile(new File(ADHOC_FOLDER));

        // Create Eval object with 10 possible classes
        Evaluation eval = new Evaluation(outputNum);
        INDArray output = null;

        while (adhocIter.hasNext()) {
            DataSet next = adhocIter.next();
            System.out.println("next: " + next.toString());
            System.out.println("labels: " + adhocIter.getLabels().toString());
            output = model.output(next.getFeatures());
            System.out.println("output: " + output);
            eval.eval(next.getLabels(), output);
        }

        System.out.println(eval.stats());
        int result = maxNumberLabel(output);
        System.out.println("result: " + result);
        return result;
    }

    private int maxNumberLabel(final INDArray output) {

        double maxNumber = output.maxNumber().doubleValue();
        for (long i = 0; i < output.toDoubleVector().length; i++) {
            if (Double.compare(output.getDouble(i), maxNumber) == 0) {
                return Long.valueOf(i).intValue();
            }
        }
        return -1;
    }

    private DataSetIterator createDataSetIteratorFromFile(final File data) throws IOException {

        // Extract the parent path as the image label
        ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();

        ImageRecordReader recordReader = new ImageRecordReader(height, width, channels, labelMaker);

        // Scale pixel values to 0-1
        DataNormalization scaler = new ImagePreProcessingScaler(0, 1);

        FileSplit fileSplit = new FileSplit(data, NativeImageLoader.ALLOWED_FORMATS, randNumGen);

        // Initialize the record reader
        // add a listener, to extract the name
        recordReader.initialize(fileSplit);
        // recordReader.setListeners(new LogRecordListener());

        // DataSet Iterator
        DataSetIterator dataSetIterator = new RecordReaderDataSetIterator(recordReader, batchSize, 1, outputNum);

        scaler.fit(dataSetIterator);
        dataSetIterator.setPreProcessor(scaler);

        return dataSetIterator;
    }

    private MultiLayerConfiguration buildModelConfTwoLayer() {

        return new NeuralNetConfiguration.Builder()
            .seed(rngseed) //include a random seed for reproducibility
            .activation(Activation.RELU)
            .weightInit(WeightInit.XAVIER)
            .updater(new Nesterovs(rate, 0.98))
            .l2(rate * 0.005) // regularize learning model
            .list()
            .layer(0, new DenseLayer.Builder() //create the first input layer.
                .nIn(height * width)
                .nOut(500)
                .build())
            .layer(1, new DenseLayer.Builder() //create the second input layer
                .nIn(500)
                .nOut(100)
                .build())
            .layer(2, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD) //create hidden layer
                .activation(Activation.SOFTMAX)
                .nIn(100)
                .nOut(outputNum)
                .build())
            .setInputType(InputType.convolutional(height, width, channels)).build();
    }
}
