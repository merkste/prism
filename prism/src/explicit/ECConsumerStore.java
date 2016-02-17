package explicit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import prism.PrismComponent;

public class ECConsumerStore extends ECConsumer {

	private List<BitSet> mecs = new ArrayList<BitSet>();

	public ECConsumerStore(PrismComponent parent, NondetModel model) {
		super(parent, model);
	}

	@Override
	public void notifyNextMEC(BitSet mec) {
		mecs.add(mec);
	}
	
	/**
	 * Get the list of states for computed MECs.
	 */
	public List<BitSet> getMECStates() {
		return mecs;
	}


}
