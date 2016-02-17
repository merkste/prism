package common.iterable.collections;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.SortedSet;

/**
 * @author Marcus Daum (mdaum@tcs.inf.tu-dresden.de)
 */
public class SortedSingletonSet<T extends Comparable<? super T>> extends SingletonSet<T> implements SortedSet<T>
{
	public SortedSingletonSet()
	{
		this(null);
	}

	public SortedSingletonSet(final T item)
	{
		element = item;
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
		if (! (obj instanceof SortedSingletonSet)){
			return false;
		}
		final SortedSingletonSet<?> other = (SortedSingletonSet<?>) obj;
		if (isEmpty()){
			return other.isEmpty();
		}
		if (other.isEmpty()){
			return false;
		}
		return element.equals(other.element);
	}

	@Override
	public Comparator<? super T> comparator()
	{
		return new Comparator<T>()
		{
			@Override
			public int compare(T o1, T o2){
				return o1.compareTo(o2);
			}
		};
	}

	@Override
	public SortedSet<T> subSet(T fromElement, T toElement)
	{
		if (toElement.compareTo(fromElement) < 0){
			throw new IllegalArgumentException("fromElement (" + fromElement + ") > toElement (" + toElement + ")");
		}
		if (isEmpty()){
			return new SortedSingletonSet<T>();
		}
		if ((element.compareTo(fromElement) >= 0) && (element.compareTo(toElement) < 0)){
			return this;
		}
		return new SortedSingletonSet<T>();
	}

	@Override
	public SortedSet<T> headSet(T toElement)
	{
		if (isEmpty() || (element.compareTo(toElement) >= 0)){
			return new SortedSingletonSet<T>();
		}
		//element.compareTo(toElement) < 0
		return this;
	}

	@Override
	public SortedSet<T> tailSet(T fromElement)
	{
		if (isEmpty() || (element.compareTo(fromElement) < 0)){
			return new SortedSingletonSet<T>();
		}
		//element.compareTo(fromElement) >= 0
		return this;
	}

	@Override
	public T first()
	{
		if (isEmpty()){
			throw new NoSuchElementException("There is no first element in an empty set.");
		}
		return element;
	}

	@Override
	public T last()
	{
		if (isEmpty()){
			throw new NoSuchElementException("There is no last element in an empty set.");
		}
		return element;
	}

	public static void main(String[] args)
	{
		SortedSingletonSet<Integer> set = new SortedSingletonSet<Integer>(5);
		System.out.println(set.getElement() + " is contained in " + set + "? " + set.contains(set.getElement()));
		System.out.println(25 + " is contained in " + set + "? " + set.contains(25) + "\n");
		for (int pivot : new int[]{1, 5, 9}){
			System.out.println("headSet(" + pivot + ") = " + set.headSet(pivot));
			System.out.println("tailSet(" + pivot + ") = " + set.tailSet(pivot));
		}
	}
}