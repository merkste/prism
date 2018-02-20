package explicit;

import java.util.BitSet;

import common.IterableBitSet;
import prism.PrismComponent;
import prism.PrismException;

public abstract class BSCCConsumer extends SCCConsumer
{
	public BSCCConsumer(PrismComponent parent, Model model)
	{
		super(parent, model);
	}

	public abstract void notifyNextBSCC(BitSet bscc) throws PrismException;

	@Override
	public void notifyNextSCC(BitSet scc) throws PrismException
	{
		if (isBSCC(scc)) {
			notifyNextBSCC(scc);
		}
	}

	boolean isBSCC(BitSet scc)
	{
		return new IterableBitSet(scc).allMatch((int s) -> model.allSuccessorsInSet(s, scc));
	}
}
