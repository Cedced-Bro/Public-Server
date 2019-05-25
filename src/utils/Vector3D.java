package utils;

public class Vector3D<D, D1, D2> implements Tupel {

	public final D data0;
	public final D1 data1;
	public final D2 data2;
	
	public Vector3D (D data0, D1 data1, D2 data2) {
		this.data0 = data0;
		this.data1 = data1;
		this.data2 = data2;
	}
}
