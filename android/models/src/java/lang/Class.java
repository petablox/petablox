class Class
{

	public java.lang.String name;

	public static java.lang.Class<?> forName(java.lang.String className) throws java.lang.ClassNotFoundException {
        	java.lang.Class k = new java.lang.Class();
		k.name = className;
        	return k;
	}

	public java.lang.String getName() {
		return name;
	}


}
