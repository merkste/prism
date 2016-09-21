package explicit;

import java.util.BitSet;

import prism.PrismComponent;
import prism.PrismException;

public abstract class ECConsumer extends PrismComponent {
	
	protected NondetModel model;

	public ECConsumer(PrismComponent parent, NondetModel model)
	{
		super(parent);
		this.model = model;
	}

	public abstract void notifyNextMEC(BitSet mec) throws PrismException;
	public void notifyDone() {}

}
