package de.nuro.service;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.records.listener.impl.LogRecordListener;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
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
import org.nd4j.linalg.lossfunctions.LossFunctions;
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
	public static final String TMP_FOLDER = NetworkService.NURO_FOLDER + "/tmp";
	private static final String MNIST_PNG_FOLDER = NURO_FOLDER + "/mnist_png";
	private static final String TRAINED_MODEL_ZIP = NURO_FOLDER + "/trained_mnist_model.zip";

	public int guessNumber() throws IOException {

		// image information
		// 28 * 28 grayscale
		// grayscale implies single channel
		int height = 28;
		int width = 28;
		int channels = 1;
		int rngseed = 123;
		Random randNumGen = new Random(rngseed);
		int batchSize = 128;
		int outputNum = 10;
		int numEpochs = 15;

		File trainedModelFile = new File(TRAINED_MODEL_ZIP);

		File trainData = new File(MNIST_PNG_FOLDER + "/training");

		// Define the FileSplit(PATH, ALLOWED FORMATS,random)
		FileSplit train = new FileSplit(trainData, NativeImageLoader.ALLOWED_FORMATS, randNumGen);

		// Extract the parent path as the image label
		ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();

		ImageRecordReader recordReader = new ImageRecordReader(height, width, channels, labelMaker);

		// Initialize the record reader
		// add a listener, to extract the name
		recordReader.initialize(train);
		// recordReader.setListeners(new LogRecordListener());

		// DataSet Iterator
		DataSetIterator dataIter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, outputNum);

		// Scale pixel values to 0-1
		DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
		scaler.fit(dataIter);
		dataIter.setPreProcessor(scaler);

		if (!trainedModelFile.exists()) {

			// Build Our Neural Network
			System.out.println("BUILD MODEL");
			MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(rngseed)
					.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
					.updater(new Nesterovs(0.006, 0.9)).l2(1e-4).list()
					.layer(0,
							new DenseLayer.Builder().nIn(height * width).nOut(100).activation(Activation.RELU)
									.weightInit(WeightInit.XAVIER).build())
					.layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD).nIn(100)
							.nOut(outputNum).activation(Activation.SOFTMAX).weightInit(WeightInit.XAVIER).build())
					.setInputType(InputType.convolutional(height, width, channels)).build();

			MultiLayerNetwork model = new MultiLayerNetwork(conf);
			model.init();

			model.setListeners(new ScoreIterationListener(10));

			System.out.println("TRAIN MODEL");
			for (int i = 0; i < numEpochs; i++) {
				model.fit(dataIter);
			}

			System.out.println("SAVE TRAINED MODEL");

			// boolean save Updater
			boolean saveUpdater = false;

			// ModelSerializer needs modelname, saveUpdater, Location
			ModelSerializer.writeModel(model, trainedModelFile, saveUpdater);
		}
		System.out.println("GUESS NUMBER");

		File testData = new File(TMP_FOLDER);

		// Define the FileSplit(PATH, ALLOWED FORMATS,random)
		FileSplit test = new FileSplit(testData, NativeImageLoader.ALLOWED_FORMATS, randNumGen);

		// Build Our Neural Network
		System.out.println("LOAD TRAINED MODEL");

		MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(trainedModelFile);

		model.getLabels();

		// Test the Loaded Model with the test data
		recordReader.initialize(test);
		DataSetIterator testIter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, outputNum);
		scaler.fit(testIter);
		testIter.setPreProcessor(scaler);

		// Create Eval object with 10 possible classes
		Evaluation eval = new Evaluation(outputNum);
		INDArray output = null;
		
		while (testIter.hasNext()) {
			DataSet next = testIter.next();
			System.out.println("next: "+next.toString());
			System.out.println("labels: "+dataIter.getLabels().toString());
			output = model.output(next.getFeatures());
			System.out.println("output: "+output);
			eval.eval(next.getLabels(), output);
		}

		System.out.println(eval.stats());
		return maxNumberLabel(output);
	}
	
	private int maxNumberLabel(INDArray output) {
		
		double maxNumber = output.maxNumber().doubleValue();
		for (long i = 0; i < output.toDoubleVector().length; i++) {
			if (Double.compare(output.getDouble(i),maxNumber) == 0) {
				return Long.valueOf(i).intValue();
			}
		}
		return -1;
	}
}
