package petablox.android.missingmodels.util;

/*
 * Translates from the Petablox method signature to the Soot method signature.
 * @author Saswat Anand
 * @author Osbert Bastani
 */
public class Compare {
	/*
	public static void main(String[] args) throws IOException {
		String line;
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		PrintWriter writer = new PrintWriter(args[1]);
		while((line = reader.readLine()) != null){
			//String[] tokens = getTokens(line);
			//writer.println(getPetabloxSigFor(tokens[0]) + tokens[1] + "#" + tokens[2]);
		}
		reader.close();
		writer.close();
	}
	*/

	public static String[] getTokens(String modelSig) {
		String[] result = new String[3];

		String[] tokens = modelSig.split("#");
		result[2] = tokens[1];

		String[] sigTokens = tokens[0].split("\\[");
		result[1] = "[" + sigTokens[sigTokens.length-1];

		StringBuilder sb = new StringBuilder();
		for(int i=0; i<sigTokens.length-1; i++) {
			if(i == sigTokens.length-2) {
				sb.append(sigTokens[i]);
			} else {
				sb.append(sigTokens[i] + "[");
			}
		}
		result[0] = sb.toString();

		return result;
	}

	public static String getSootSigFor(String chordSig) {
		int atIndex = chordSig.indexOf('@');
		String className = chordSig.substring(atIndex+1);
		String subsig = chordSig.substring(0,atIndex);
		return "<"+className+": "+getSootSubsigFor(subsig)+">";
	}

	private static String getSootSubsigFor(String chordSubsig) {
		String name = chordSubsig.substring(0, chordSubsig.indexOf(':'));
		String retType = chordSubsig.substring(chordSubsig.indexOf(')')+1);
		String paramTypes = chordSubsig.substring(chordSubsig.indexOf('(')+1, chordSubsig.indexOf(')'));
		return parseDesc(retType) + " " + name + "(" + parseDesc(paramTypes) + ")";
	}

	static String parseDesc(String desc) {
		StringBuilder params = new StringBuilder();
		String param = null;
		char c;
		int arraylevel=0;
		boolean didone = false;

		int len = desc.length();
		for (int i=0; i < len; i++) {
			c = desc.charAt(i);
			if (c =='B') {
				param = "byte";
			} else if (c =='C') {
				param = "char";
			} else if (c == 'D') {
				param = "double";
			} else if (c == 'F') {
				param = "float";
			} else if (c == 'I') {
				param = "int";
			} else if (c == 'J') {
				param = "long";
			} else if (c == 'S') {
				param = "short";
			} else if (c == 'Z') {
				param = "boolean";
			} else if (c == 'V') {
				param = "void";
			} else if (c == '[') {
				arraylevel++;
				continue;
			} else if (c == 'L') {
				int j;
				j = desc.indexOf(';',i+1);
				param = desc.substring(i+1,j);
				// replace '/'s with '.'s
				param = param.replace('/','.');
				i = j;
			} else
				assert false;

			if (didone) params.append(',');
			params.append(param);
			while (arraylevel>0) {
				params.append("[]");
				arraylevel--;
			}
			didone = true;
		}
		return params.toString();
	}
}
