package explicit.quantile.topologicalSorting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SCCStorage
{
	private List<Set<Integer>> sccs = new ArrayList<Set<Integer>>();
	private boolean finished = false;

	public void notifyNextSCC(Set<Integer> scc)
	{
		sccs.add(scc);
	}

	public void notifyDone()
	{
		finished = true;
	}

	public List<Set<Integer>> getSCCs()
	{
		if (!finished)
			throw new UnsupportedOperationException("SCC computation is not yet finished.");
		return sccs;
	}
}