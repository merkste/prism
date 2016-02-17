package common.iterable.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
/**
 * 
 * @author Marcus Daum (mdaum@tcs.inf.tu-dresden.de)
 */
public class SortedFixedCapacityIntegerSet implements SortedSet<Integer>
{
	private final int[] set;
	private int highestUsedIndex = -1;
	
	public SortedFixedCapacityIntegerSet(int capacity)
	{
		set = new int[capacity];
	}
	private SortedFixedCapacityIntegerSet(int[] elements)
	{
		assert (isSorted(elements));
		set = elements;
		highestUsedIndex = elements.length-1;
	}

	private boolean isSorted(int[] elements)
	{
		if (elements.length == 0){
			return true;
		}
		int previous = elements[0];
		for (int index = 1; index < elements.length; index++){
			if (elements[index] <= previous){
				return false;
			}
		}
		return true;
	}
	private int binarySearch(int element)
	{
		if (isEmpty() || element < set[0]){
			return -1;
		}
		if (element > set[highestUsedIndex]){
			return -highestUsedIndex-2;
		}
		return Arrays.binarySearch(set, 0, highestUsedIndex+1, element);
	}
	private int calculateInsertionPoint(int index)
	{
		return (index < 0)? -index-1: index;
	}
	private int[] elementsFrom(int index)
	{
		return elements(index, highestUsedIndex+1);
	}
	private int[] elementsTo(int index)
	{
		return elements(0, index);
	}
	private int[] elements(int fromIndex, int toIndex)
	{
		assert (0 <= fromIndex && fromIndex <= toIndex && toIndex <= highestUsedIndex+1);
		int[] elements = new int[toIndex-fromIndex];
		System.arraycopy(set, fromIndex, elements, 0, elements.length);
		return elements;
	}
	private void addElement(int element, int atIndex)
	{
		assert (0 <= atIndex && atIndex <= highestUsedIndex+1);
		System.arraycopy(set, atIndex, set, atIndex+1, highestUsedIndex+1-atIndex);
		set[atIndex] = element;
		highestUsedIndex++;
	}
	private void removeElement(int atIndex)
	{
		assert (0 <= atIndex && atIndex <= highestUsedIndex);
		System.arraycopy(set, atIndex+1, set, atIndex, highestUsedIndex-atIndex);
		highestUsedIndex--;
	}

	@Override
	public int size()
	{
		return highestUsedIndex+1;
	}

	@Override
	public boolean isEmpty()
	{
		return (highestUsedIndex == -1);
	}


	@Override
	public boolean contains(Object o)
	{
		if (isEmpty()){
			return false;
		}
		if (o instanceof Integer){
			return (binarySearch((int) o) >= 0);
		}
		return false;
	}

	@Override
	public Iterator<Integer> iterator()
	{
		return new Iterator<Integer>(){
			private int next = 0;
			private int size = highestUsedIndex + 1;
			
			@Override
			public boolean hasNext()
			{
				return next < size;
			}
			@Override
			public Integer next()
			{
				return set[next++];
			}
			@Override
			public void remove()
			{
				removeElement(--next);
				size--;
			}
		};
	}

	@Override
	public Object[] toArray()
	{
		if (isEmpty())
			return new Object[] {};
		Integer[] result = new Integer[highestUsedIndex+1];
		for (int i = 0; i < result.length; i++)
			result[i] = set[i];
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a)
	{
		final T[] result;
		final int size = size();
		if (a.length >= size) {
			result = a;
		} else {
			result = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size());
		}

		final Iterator<Integer> it = iterator();
		for (int index = 0; index < size; index++) {
			result[index] = (T) (it.hasNext() ? it.next() : null);
		}
		return result;
	}

	@Override
	public boolean add(Integer e)
	{
		int intE = (int) e;
		int index = binarySearch(intE);
		if (index >= 0){
			return false;
		}
		if (highestUsedIndex >= set.length-1){
			throw new IllegalArgumentException("The capacity has been reached, no more elements can be added.");
		}
		addElement(intE, calculateInsertionPoint(index));
		return true;
	}

	@Override
	public boolean remove(Object e)
	{
		if (e instanceof Integer){
			int intE = (int) e;
			//try to find the element
			int elementIndex = binarySearch(intE);
			if (elementIndex < 0){
				//e is not stored
				return false;
			}
			removeElement(elementIndex);
			return true;
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c)
	{
		for (Iterator<?> it = c.iterator(); it.hasNext();){
			if (!contains(it.next())){
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends Integer> c)
	{
		boolean modified = false;
		for (int i : c){
			modified |= add(i);
		}
		return modified;
	}

	@Override
	public boolean retainAll(Collection<?> c)
	{
		boolean modified = false;
		for (Iterator<Integer> it = iterator(); it.hasNext();){
			if (!c.contains(it.next())){
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public boolean removeAll(Collection<?> c)
	{
		boolean modified = false;
		if (size() > c.size()){
			for (Iterator<?> it = c.iterator(); it.hasNext();){
				modified |= remove(it.next());
			}
		} else {
			for (Iterator<?> it = iterator(); it.hasNext();){
				if (c.contains(it.next())){
					it.remove();
					modified = true;
				}
			}
		}
		return modified;
	}

	@Override
	public void clear()
	{
		highestUsedIndex = -1;
	}

	@Override
	public String toString()
	{
		if (set == null){
			return "null";
		}
		if (highestUsedIndex == -1){
			return "[]";
		}
		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int index = 0; ; index++){
			b.append(set[index]);
			if (index == highestUsedIndex){
				return b.append(']').toString();
			}
			b.append(", ");
		}
	}

	@Override
	public Comparator<? super Integer> comparator()
	{
		return null;
	}

	@Override
	public SortedSet<Integer> subSet(Integer fromElement, Integer toElement)
	{
		if (fromElement < 0){
			throw new IllegalArgumentException("fromElement (" + fromElement + ") < 0");
		}
		if (highestUsedIndex+1 < toElement){
			throw new IllegalArgumentException("toElement (" + toElement + ") > actual size");
		}
		if (toElement < fromElement){
			throw new IllegalArgumentException("fromElement > toElement: " + fromElement + " > " + toElement);
		}
		return new SortedFixedCapacityIntegerSet(elements(fromElement, toElement));
	}

	@Override
	public SortedSet<Integer> headSet(Integer toElement)
	{
		if (toElement < 0){
			throw new IllegalArgumentException("toElement (" + toElement + ") < 0");
		}
		if (highestUsedIndex+1 < toElement){
			throw new IllegalArgumentException("toElement (" + toElement + ") > actual size");
		}
		return new SortedFixedCapacityIntegerSet(elementsTo(toElement));
	}

	@Override
	public SortedSet<Integer> tailSet(Integer fromElement)
	{
		if (fromElement < 0){
			throw new IllegalArgumentException("fromElement (" + fromElement + ") < 0");
		}
		if (highestUsedIndex+1 < fromElement){
			throw new IllegalArgumentException("fromElement (" + fromElement + ") > actual size");
		}
		return new SortedFixedCapacityIntegerSet(elementsFrom(fromElement));
	}

	@Override
	public Integer first()
	{
		if (isEmpty()){
			throw new NoSuchElementException("There is no first element in an empty set.");
		}
		return set[0];
	}

	@Override
	public Integer last()
	{
		if (isEmpty()){
			throw new NoSuchElementException("There is no last element in an empty set.");
		}
		return set[highestUsedIndex];
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + highestUsedIndex;
		result = prime * result + Arrays.hashCode(set);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj){
			return true;
		}
		if (obj == null){
			return false;
		}
		if (!(obj instanceof SortedFixedCapacityIntegerSet)){
			return false;
		}
		final SortedFixedCapacityIntegerSet other = (SortedFixedCapacityIntegerSet) obj;
		if (highestUsedIndex != other.highestUsedIndex){
			return false;
		}
		if (!Arrays.equals(set, other.set)){
			return false;
		}
		return true;
	}

	public static void main(String[] args)
	{
		SortedFixedCapacityIntegerSet set = new SortedFixedCapacityIntegerSet(6);
		System.out.println(set);
		set.add(0);
		System.out.println(set);
		set.add(1);
		System.out.println(set);
		set.add(2);
		System.out.println(set);
		set.add(3);
		System.out.println(set);
		set.add(4);
		System.out.println(set);
		set.add(5);
		System.out.println(set);
		for (int index = 0; index < set.size()+1; index++){
			System.out.println("elements from index " + index + ": " + Arrays.toString(set.elementsFrom(index)));
		}
		for (int index = 0; index < set.size()+1; index++){
			System.out.println("elements to index " + index + ": " + Arrays.toString(set.elementsTo(index)));
		}
		////////////////////////////////////////////////////////////////////////
		for (int element : new int[]{-1, 0, 1, 2, 3, 4, 5, 6}){
			System.out.println("contains " + element + ": " + set.contains(element));
		}
		////////////////////////////////////////////////////////////////////////
		set = new SortedFixedCapacityIntegerSet(6);
		for (int element : new int[]{5, 10, 7, 4711, -1, 3}){
			System.out.print("add " + element + ": ");
			set.add(element);
			System.out.println(set);
		}
		////////////////////////////////////////////////////////////////////////
		System.out.println("\nstarting set: " + set);
		for (int element : new int[]{4711, 5, -1, 7, 3, 10}){
			System.out.print("remove " + element + " (per iterator): ");
			for (Iterator<Integer> it = set.iterator(); it.hasNext();) {
				int next = it.next();
				if (next == element){
					it.remove();
				}
			}
			System.out.println(set);
		}
		////////////////////////////////////////////////////////////////////////
		set = new SortedFixedCapacityIntegerSet(6);
		set.add(5);
		set.add(10);
		set.add(7);
		set.add(4711);
		set.add(-1);
		set.add(3);
		System.out.println("\nstarting set: " + set);
		for (int element : new int[]{7, -1, 4711, 55}){
			set.remove(element);
			System.out.println("remove " + element + ": " + set);
		}
		////////////////////////////////////////////////////////////////////////
		set = new SortedFixedCapacityIntegerSet(6);
		set.add(5);
		set.add(10);
		set.add(7);
		set.add(4711);
		set.add(-1);
		set.add(3);
		System.out.println("\nstarting set: " + set);
		for (int index = 0; index < set.size()+1; index++){
			System.out.println("head set from index " + index + ": " + set.headSet(index));
		}
		////////////////////////////////////////////////////////////////////////
		System.out.println("\nstarting set: " + set);
		for (int index = 0; index < set.size()+1; index++){
			System.out.println("tail set from index " + index + ": " + set.tailSet(index));
		}
		////////////////////////////////////////////////////////////////////////
		System.out.println("\nstarting set: " + set);
		for (int fromElement = 0; fromElement < set.size()+1; fromElement++){
			for (int toElement = fromElement; toElement < set.size()+1; toElement++){
				System.out.println("sub set from indizes " + fromElement + " to " + toElement + ": " + set.subSet(fromElement, toElement));
			}
		}
	}
}