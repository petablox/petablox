package chord.analyses.mantis;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

/*
 * Relevant system properties;
 * - chord.mantis.data.dir
 * - chord.mantis.out.dir
 * - chord.mantis.num.runs
 *
 * Reads the following files from the directory specified by property chord.mantis.data.dir:
 * 1. [ctrl|bool|long|real]_feature_name.txt
 * 2. [ctrl|bool|long|real]_feature_data.txt from sub-directories
 *    0, 1, ..., [chord.mantis.num.runs]-1.
 * Writes aggregated results to files feature_name.txt, feature_data.txt, and
 * feature_cost.txt in the directory specified by property chord.mantis.out.dir.
 */
public class PostProcessor {
	static final boolean keepSum = true;
	public static void main(String[] args) throws IOException {
		String dataDirName = System.getProperty("chord.mantis.data.dir");
		assert (dataDirName != null);
		String outDirName = System.getProperty("chord.mantis.out.dir");
		assert (outDirName != null);
		int numData = Integer.getInteger("chord.mantis.num.runs");
		assert (numData != 0);
		List<String> ctrlFeatureNames = readStrings(new File(dataDirName, "ctrl_feature_name.txt"));
		List<String> boolFeatureNames = readStrings(new File(dataDirName, "bool_feature_name.txt"));
		List<String> longFeatureNames = readStrings(new File(dataDirName, "long_feature_name.txt"));
		List<String> realFeatureNames = readStrings(new File(dataDirName, "real_feature_name.txt"));
		int numCtrlFeatures = ctrlFeatureNames.size();
		int numBoolFeatures = boolFeatureNames.size();
		int numLongFeatures = longFeatureNames.size();
		int numRealFeatures = realFeatureNames.size();
		int   [][] ctrlData    = new int   [numData][numCtrlFeatures];
		int   [][] boolData    = new int   [numData][numBoolFeatures];
		long  [][] longSumData = new long  [numData][numLongFeatures/2];
		int   [][] longFrqData = new int   [numData][numLongFeatures/2];
		double[][] realSumData = new double[numData][numRealFeatures/2];
		int   [][] realFrqData = new int   [numData][numRealFeatures/2];
		boolean[] ignoreData = new boolean[numData];
		for (int i = 0; i < numData; i++) {
			File dir = new File(dataDirName, Integer.toString(i));
			if (!dir.exists()) {
				System.err.println("WARN: Skipping data: " + dir);
				ignoreData[i] = true;
				continue;
			}
			readIntData (new File(dir, "ctrl_feature_data.txt"), ctrlData[i]);
			readIntData (new File(dir, "bool_feature_data.txt"), boolData[i]);
			readLongData(new File(dir, "long_feature_data.txt"), longSumData[i], longFrqData[i]);
			readRealData(new File(dir, "real_feature_data.txt"), realSumData[i], realFrqData[i]);
		}

		PrintWriter featureNameOut = new PrintWriter(new File(outDirName, "feature_name.txt"));
		PrintWriter featureDataOut = new PrintWriter(new File(outDirName, "feature_data.txt"));
		PrintWriter featureCostOut = new PrintWriter(new File(outDirName, "feature_cost.txt"));

		for (int i = 0; i < numCtrlFeatures; i += 2) {
			int befIdx = i, aftIdx = i + 1;
			boolean isLoop = true;
			for (int j = 0; j < numData; j++) {
				if (ignoreData[j]) continue;
				int bef = ctrlData[j][befIdx];
				int aft = ctrlData[j][aftIdx];
				if (bef - aft != 1) {
					isLoop = false;
					break;
				}
			}
			if (isLoop) {
				featureNameOut.println(ctrlFeatureNames.get(aftIdx));
				for (int j = 0; j < numData; j++) {
					if (ignoreData[j]) continue;
					featureDataOut.print(ctrlData[j][aftIdx] + " ");
				}
				featureDataOut.println();
				featureCostOut.println("2");
			} else {
				featureNameOut.println(ctrlFeatureNames.get(befIdx));
				featureNameOut.println(ctrlFeatureNames.get(aftIdx));
				for (int j = 0; j < numData; j++) {
					if (ignoreData[j]) continue;
					int bef = ctrlData[j][befIdx];
					int aft = ctrlData[j][aftIdx];
					featureDataOut.print((bef - aft) + " ");
				}
				featureDataOut.println();
				for (int j = 0; j < numData; j++) {
					if (ignoreData[j]) continue;
					featureDataOut.print(ctrlData[j][aftIdx] + " ");
				}
				featureDataOut.println();
				featureCostOut.println("2");
				featureCostOut.println("2");
			}
		}

		for (int i = 0; i < numBoolFeatures; i += 2) {
			int truIdx = i, flsIdx = i + 1;
			boolean isSingle = true;
			for (int j = 0; j < numData; j++) {
				if (ignoreData[j]) continue;
				int sum = boolData[j][truIdx] + boolData[j][flsIdx];
				if (sum != 1) {
					isSingle = false;
					break;
				}
			}
			featureNameOut.println(boolFeatureNames.get(truIdx));
			featureNameOut.println(boolFeatureNames.get(flsIdx));
			for (int j = 0; j < numData; j++) {
				if (ignoreData[j]) continue;
				featureDataOut.print(boolData[j][truIdx] + " ");
			}
			featureDataOut.println();
			for (int j = 0; j < numData; j++) {
				if (ignoreData[j]) continue;
				featureDataOut.print(boolData[j][flsIdx] + " ");
			}
			featureDataOut.println();
			if (isSingle) {
				featureCostOut.println("1");
				featureCostOut.println("1");
			} else {
				featureCostOut.println("2");
				featureCostOut.println("2");
			}
		}

		for (int i = 0; i < numLongFeatures / 2; i++) {
			boolean isSingle = true;
			for (int j = 0; j < numData; j++) {
				if (ignoreData[j]) continue;
				int frq = longFrqData[j][i];
				if (frq != 1) {
					isSingle = false;
					break;
				}
			}
			if (isSingle) {
				featureNameOut.println(longFeatureNames.get(i*2));
				for (int j = 0; j < numData; j++) {
					if (ignoreData[j]) continue;
					featureDataOut.print(longSumData[j][i] + " ");
				}
				featureDataOut.println();
				featureCostOut.println("1");
			} else {
				if (keepSum)
					featureNameOut.println(longFeatureNames.get(i*2));
				featureNameOut.println(longFeatureNames.get(i*2 + 1));
				if (keepSum) {
					for (int j = 0; j < numData; j++) {
						if (ignoreData[j]) continue;
						featureDataOut.print(longSumData[j][i] + " ");
					}
					featureDataOut.println();
				}
				for (int j = 0; j < numData; j++) {
					if (ignoreData[j]) continue;
					long sum = longSumData[j][i];
					int frq = longFrqData[j][i];
					long avg = (frq == 0) ? -99999 : sum / frq;
					featureDataOut.print(avg + " ");
				}
				featureDataOut.println();
				if (keepSum)
					featureCostOut.println("2");
				featureCostOut.println("2");
			}
		}

		for (int i = 0; i < numRealFeatures / 2; i++) {
			boolean isSingle = true;
			for (int j = 0; j < numData; j++) {
				if (ignoreData[j]) continue;
				int frq = realFrqData[j][i];
				if (frq != 1) {
					isSingle = false;
					break;
				}
			}
			if (isSingle) {
				featureNameOut.println(realFeatureNames.get(i*2));
				for (int j = 0; j < numData; j++) {
					if (ignoreData[j]) continue;
					featureDataOut.print(realSumData[j][i] + " ");
				}
				featureDataOut.println();
				featureCostOut.println("1");
			} else {
				if (keepSum)
					featureNameOut.println(realFeatureNames.get(i*2));
				featureNameOut.println(realFeatureNames.get(i*2 + 1));
				if (keepSum) {
					for (int j = 0; j < numData; j++) {
						if (ignoreData[j]) continue;
						featureDataOut.print(realSumData[j][i] + " ");
					}
					featureDataOut.println();
				}
				for (int j = 0; j < numData; j++) {
					if (ignoreData[j]) continue;
					double sum = realSumData[j][i];
					int frq = realFrqData[j][i];
					double avg = (frq == 0) ? -99999 : sum / frq;
					featureDataOut.print(avg + " ");
				}
				featureDataOut.println();
				if (keepSum)
					featureCostOut.println("2");
				featureCostOut.println("2");
			}
		}

		featureNameOut.close();
		featureDataOut.close();
		featureCostOut.close();
	}

    private static List<String> readStrings(File file) throws IOException {
		List<String> list = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new FileReader(file));
		String s;
		while ((s = in.readLine()) != null) {
			list.add(s);
		}
		in.close();
		return list;
	}

    private static void readIntData(File file, int[] data) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		String s;
		int i = 0;
		while ((s = in.readLine()) != null) {
			int v = Integer.parseInt(s);
			data[i++] = v;
		}
		assert (i == data.length) : (file + ": " + i + " != " + data.length);
		in.close();
    }

    private static void readLongData(File file, long[] sumData, int[] frqData) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		String s;
		int numLines = 0;
		for (int i = 0; (s = in.readLine()) != null; i++) {
			if (i % 2 == 0) {
				long v = Long.parseLong(s);
				sumData[i/2] = v;
			} else {
				int v = Integer.parseInt(s);
				frqData[i/2] = v;
			}
			numLines++;
		}
		assert (numLines/2 == sumData.length) : (file + ": " + (numLines/2) + " != " + sumData.length);
		assert (numLines/2 == frqData.length) : (file + ": " + (numLines/2) + " != " + frqData.length);
		in.close();
    }

    private static void readRealData(File file, double[] sumData, int[] frqData) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		String s;
		int numLines = 0;
		for (int i = 0; (s = in.readLine()) != null; i++) {
			if (i % 2 == 0) {
				double v = Double.parseDouble(s);
				sumData[i/2] = v;
			} else {
				int v = Integer.parseInt(s);
				frqData[i/2] = v;
			}
		}
		assert (numLines/2 == sumData.length) : (file + ": " + (numLines/2) + " != " + sumData.length);
		assert (numLines/2 == frqData.length) : (file + ": " + (numLines/2) + " != " + frqData.length);
		in.close();
    }
}
