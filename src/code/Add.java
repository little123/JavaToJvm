package code;

public class Add {
	private static int m;
	private static int n;

	public static int plus(int i, int j) {
		int s = i + j;
		return s;
	}
	
	public static void main(String[] args) {
		int t;
		t = plus(1,1);
		
		m = 1;
		n = 1;
		t = m + n;
	}
}
