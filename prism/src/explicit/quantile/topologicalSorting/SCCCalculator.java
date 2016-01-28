package explicit.quantile.topologicalSorting;

import java.util.List;
import java.util.Set;

import common.iterable.collections.SetFactory;

import explicit.quantile.dataStructure.RewardWrapper;

public abstract class SCCCalculator
{
	protected RewardWrapper model;
	protected Set<Integer> states;

	protected final SCCStorage storage;
	protected final SetFactory setFactory;

	public SCCCalculator(RewardWrapper aModel, SetFactory theSetFactory, Set<Integer> set)
	{
		model = aModel;
		setFactory = theSetFactory;
		states = set;
		storage = new SCCStorage();
	}

	public abstract void calculateSCCs();

	public List<Set<Integer>> getSCCs()
	{
		return storage.getSCCs();
	}

	protected static class TarjanNode
	{
		public int lowlink = -1;
		public int index = -1;
		public int id;

		public TarjanNode(int id)
		{
			this.id = id;
		}

		public String toString()
		{
			return "id: " + id + "--lowlink: " + lowlink + "--index: " + index;
		}
	}
}