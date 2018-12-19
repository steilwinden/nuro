package de.nuro.rawlearning.service;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.records.listener.impl.LogRecordListener;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;
import org.springframework.stereotype.Service;

/**
 * A Simple Multi Layered Perceptron (MLP) applied to digit classification for
 * the MNIST Dataset (http://yann.lecun.com/exdb/mnist/).
 *
 * This file builds one input layer and one hidden layer.
 *
 * The input layer has input dimension of numRows*numColumns where these variables indicate the
 * number of vertical and horizontal pixels in the image. This layer uses a rectified linear unit
 * (relu) activation function. The weights for this layer are initialized by using Xavier initialization
 * (https://prateekvjoshi.com/2016/03/29/understanding-xavier-initialization-in-deep-neural-networks/)
 * to avoid having a steep learning curve. This layer will have 1000 output signals to the hidden layer.
 *
 * The hidden layer has input dimensions of 1000. These are fed from the input layer. The weights
 * for this layer is also initialized using Xavier initialization. The activation function for this
 * layer is a softmax, which normalizes all the 10 outputs such that the normalized sums
 * add up to 1. The highest of these normalized values is picked as the predicted class.
 *
 */
@Service
public class NetworkService {

    private static File neuralNetFile = new File("C:/Development/neural_network/my_network");
    private static String mnistPngFolder = "C:/Development/neural_network/mnist_png";

    public void initNeuralNet() {

        try {
            //number of rows and columns in the input pictures
            final int numRows = 28;
            final int numColumns = 28;
            int outputNum = 10; // number of output classes
            int batchSize = 128; // batch size for each epoch
            int rngSeed = 123; // random number seed for reproducibility
            int numEpochs = 15; // number of epochs to perform

            //Get the DataSetIterators:
            DataSetIterator mnistTrain = new MnistDataSetIterator(batchSize, true, rngSeed);
            DataSetIterator mnistTest = new MnistDataSetIterator(batchSize, false, rngSeed);

            System.out.println("Build model....");
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(rngSeed) //include a random seed for reproducibility
                // use stochastic gradient descent as an optimization algorithm
                .updater(new Nesterovs(0.006, 0.9))
                .l2(1e-4)
                .list()
                .layer(0, new DenseLayer.Builder() //create the first, input layer with xavier initialization
                    .nIn(numRows * numColumns)
                    .nOut(1000)
                    .activation(Activation.RELU)
                    .weightInit(WeightInit.XAVIER)
                    .build())
                .layer(1, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD) //create hidden layer
                    .nIn(1000)
                    .nOut(outputNum)
                    .activation(Activation.SOFTMAX)
                    .weightInit(WeightInit.XAVIER)
                    .build())
                .pretrain(false).backprop(true) //use backpropagation to adjust weights
                .build();

            MultiLayerNetwork model = new MultiLayerNetwork(conf);
            model.init();
            //print the score with every 1 iteration
            model.setListeners(new ScoreIterationListener(1));

            System.out.println("Train model....");
            for (int i = 0; i < numEpochs; i++) {
                model.fit(mnistTrain);
            }

            File myModel = new File(System.getProperty("user.home"), "MNIST/my_model.mln");
            model = MultiLayerNetwork.load(myModel, true);

            System.out.println("Evaluate model....");
            Evaluation eval = new Evaluation(outputNum); //create an evaluation object with 10 possible classes
            while (mnistTest.hasNext()) {
                DataSet next = mnistTest.next();
                INDArray output = model.output(next.getFeatures()); //get the networks prediction
                eval.eval(next.getLabels(), output); //check the prediction against the true class
            }

            System.out.println(eval.stats());
            System.out.println("****************Example finished********************");

            model.save(neuralNetFile, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void perform(final int[] grayValues) throws IOException {

        MultiLayerNetwork model = MultiLayerNetwork.load(neuralNetFile, false);

        NDArray ndArray = new NDArray(grayValues);
        INDArray output = model.output(ndArray);
        System.out.println("RESULT:");
        for (int i = 0; i < 10; i++) {
            System.out.println(i + ": " + output.getDouble(i));
        }
    }

    public void imagePipeline() throws IOException {

        int height = 28;
        int width = 28;
        int channels = 1;
        int rngseed = 123;
        Random randNumGen = new Random(rngseed);
        int batchSize = 1;
        int outputNum = 10;

        File trainData = new File(mnistPngFolder + "/training");
        File testData = new File(mnistPngFolder + "/testing");

        FileSplit train = new FileSplit(trainData, NativeImageLoader.ALLOWED_FORMATS, randNumGen);
        FileSplit test = new FileSplit(testData, NativeImageLoader.ALLOWED_FORMATS, randNumGen);

        ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();

        ImageRecordReader recordReader = new ImageRecordReader(height, width, channels, labelMaker);

        recordReader.initialize(train);
        recordReader.setListeners(new LogRecordListener());

        DataSetIterator dataIter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, outputNum);

        for (int i = 0; i < 2; i++) {
            DataSet ds = dataIter.next();
            System.out.println(ds);
            System.out.println(dataIter.getLabels());
        }
    }

}
