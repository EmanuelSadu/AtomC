package external;

import virtualMachine.MVAtomC;

public class PutI extends ExtFunc {

	public void run() {
		System.out.printf("#%d\n", MVAtomC.getInstance().popi());
	}

}
