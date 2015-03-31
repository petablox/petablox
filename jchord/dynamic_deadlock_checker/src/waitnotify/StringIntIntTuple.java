package waitnotify;

public class StringIntIntTuple {
	public String fst;
	public int snd;
	public int thrd;
	
	public StringIntIntTuple(String s, int i1, int i2){
		fst = s;
		snd = i1;
		thrd = i2;
	}
	
	public boolean equals(Object other){
		if(!(other instanceof StringIntIntTuple)){
			return false;
		}
		StringIntIntTuple othrSIIPair = (StringIntIntTuple)other;
		String othrStr = othrSIIPair.fst;
		int othrInt1 = othrSIIPair.snd;
		int othrInt2 = othrSIIPair.thrd;
		
		boolean areStringsEq = (fst == null) ? (othrStr == null) : fst.equals(othrStr);
		boolean areInt1sEq = (snd == othrInt1);
		boolean areInt2sEq = (snd == othrInt2);
		
		return areStringsEq && areInt1sEq && areInt2sEq;
	}
}
