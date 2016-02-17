package explicit.modelviews;

import java.util.BitSet;

import explicit.Model;
import explicit.ReachabilityComputer;

public enum Restriction
{
	TRANSITIVE_CLOSURE {
		public BitSet getStateSet(final Model model, final BitSet states)
		{
			return new ReachabilityComputer(model).computeSuccStar(states);
		}
	},
	STRICT {
		public BitSet getStateSet(final Model model, final BitSet states)
		{
			return states;
		}
	};

	public abstract BitSet getStateSet(final Model model, final BitSet states);
}