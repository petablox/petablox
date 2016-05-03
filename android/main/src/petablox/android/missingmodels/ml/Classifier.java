package petablox.android.missingmodels.ml;

import java.util.Random;

public class Classifier {
	private static double dot(double[] v1, double[] v2) {
		double dotProduct = 0.0;
		for(int i=0; i<v1.length; i++) {
			dotProduct += v1[i]*v2[i];
		}
		return dotProduct;
	}

	private static double sigmoid(double z) {
		return 1.0/(1.0 + Math.exp(-z));
	}

	// assume x[0] = 1
	public static double predict(double[] theta, double[] x) {
		return sigmoid(dot(x, theta));
	}

	public static boolean predictLabel(double[] theta, double[] x, double cutoff) {
		return sigmoid(dot(x, theta)) > cutoff;
	}
	
	public static boolean[] predictLabel(double[] theta, double[][] x, double cutoff) {
		boolean[] yLabel = new boolean[x.length];
		for(int i=0; i<yLabel.length; i++) {
			yLabel[i] = predictLabel(theta, x[i], cutoff);
		}
		return yLabel;
	}
	
	public static double[] predict(double[] theta, double[][] x) {
		double[] y = new double[x.length];
		for(int i=0; i<y.length; i++) {
			y[i] = predict(theta, x[i]);
		}
		return y;
	}

	public static double logLikelihood(double[] theta, double[][] x, double[] y) {
		double likelihood = 0.0;
		for(int i=0; i<x.length; i++) {
			double p = predict(theta, x[i]);
			likelihood += y[i]*Math.log(p) + (1.0-y[i])*Math.log(1-p);
		}
		return likelihood;
	}

	private static double[] logLikelihoodDerivative(double[] theta, double[][] x, double[] y) {
		double[] derivative = new double[theta.length];
		for(int i=0; i<x.length; i++) {
			for(int j=0; j<theta.length; j++) {
				double p = predict(theta, x[i]);
				derivative[j] += (y[i]-p)*x[i][j];
			}
		}
		return derivative;
	}

	public static double[] maximumLikelihood(double[][] x, double[] y, double alpha) {
		if(x == null || x.length == 0) {
			return new double[1];
		}
		double[] theta = new double[x[0].length];
		System.out.println("Training set:");
		for(int i=0; i<x.length; i++) {
			System.out.println(toString(x[i]) + ": " + y);
		}
		for(int i=0; i<200; i++) {
			double[] thetaDerivative = logLikelihoodDerivative(theta, x, y);
			System.out.println("Iteration " + i + ": " + toString(theta));
			for(int j=0; j<theta.length; j++) {
				theta[j] += alpha*thetaDerivative[j];
			}
		}
		return theta;
	}
	
	private static double[] generateRandomTheta(int length, Random random) {
		double[] theta = new double[length];
		for(int i=0; i<length; i++) {
			theta[i] = random.nextGaussian();
		}
		return theta;		
	}
	
	private static double[] generateRandomX(int length, Random random) {
		double[] x = new double[length];
		x[0] = 1.0;
		for(int i=1; i<length; i++) {
			x[i] = random.nextGaussian();
		}
		return x;
	}
	
	private static double generateRandomY(double[] theta, double[] x, Random random) {
		double p = predict(theta, x);
		double q = random.nextDouble();
		return q < p ? 1.0 : 0.0;
	}
	
	private static double[][] generateRandomXArray(int num, int length, Random random) {
		double[][] x = new double[num][length];
		for(int i=0; i<num; i++) {
			x[i] = generateRandomX(length, random);
		}
		return x;
	}
	
	private static double[] generateRandomYArray(double[] theta, double[][] x, Random random) {
		double[] y = new double[x.length]; 
		for(int i=0; i<y.length; i++) {
			y[i] = generateRandomY(theta, x[i], random);
		}
		return y;
	}
	
	private static String toString(double[] arr) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for(int i=0; i<arr.length; i++) {
			sb.append(arr[i] + ",");
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append("]");
		return sb.toString();
	}
	
	public static void main(String[] args) {
		Random random = new Random();
		int num = 10000;
		int length = 10;
		double alpha = 0.001;
		
		double[] theta = generateRandomTheta(length, random);
		double[][] x = generateRandomXArray(num, length, random);
		double[] y = generateRandomYArray(theta, x, random);
		
		double[] thetaHat = maximumLikelihood(x, y, alpha);
		System.out.println("True theta: " + toString(theta));
		System.out.println("Approximated theta: " + toString(thetaHat));
	}
}

