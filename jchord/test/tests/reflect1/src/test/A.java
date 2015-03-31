package test;

public class A implements C {
	Object f;
	public A() {
		System.out.println("CALLED");
	}
	public void foo() {
		System.out.println("");
	 	this.f = new Object();
	}
}
