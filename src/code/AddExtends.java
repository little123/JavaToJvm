package code;

public class AddExtends extends Add {
	public static int m = 2;
	int n = 1;
	Add add = new Add();
	boolean b = true;
	char c = 'q';

	@Override
	public int plus(int i, int j) {
		int s = 0;
		if (i == j) {
			s = i;
		} else {
			s = j;
		}
		return s;
	}
	
	public Add Test(Add add,int i){
		return null;
	}
}
