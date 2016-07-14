package android.content;

class ComponentName
{
	public java.lang.String name;

	public  ComponentName(java.lang.String pkg, java.lang.String cls) 
	{ 
		this.name = cls;
	}

	public  ComponentName(android.content.Context pkg, java.lang.String cls) 
	{ 
		this.name = cls;
	}

	public  ComponentName(android.content.Context pkg, java.lang.Class<?> cls) { 
		this.name = cls.name;
	}
}
	
