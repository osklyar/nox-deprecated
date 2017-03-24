/**
 * Created by skol on 02.03.17.
 */
package nox.internal.gradlize;

import com.google.common.collect.Sets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.SortedSet;

import static org.junit.Assert.assertEquals;


public class DependencyResolverImplTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void sortedSet_subSetNonEmpty_success() {
		SortedSet<Integer> set = Sets.newTreeSet(Sets.newHashSet(-10, -5, 0, 5, 10, 25));

		assertEquals("[-10, -5, 0, 5, 10]", set.subSet(-10, 25).toString());
		assertEquals("[-5, 0, 5]", set.subSet(-5, 10).toString());
		assertEquals("[-5, 0, 5, 10, 25]", set.subSet(-8, 30).toString()); // cuts head, extends beyond tail
		assertEquals("[-10, -5, 0, 5]", set.subSet(-15, 8).toString()); // extends beyond head, cuts tail
		assertEquals("[-5, 0, 5]", set.subSet(-8, 8).toString()); // cuts
		assertEquals("[]", set.subSet(1, 2).toString()); // cuts to void
		assertEquals("[]", set.subSet(26, 30).toString()); // outside tail
		assertEquals("[]", set.subSet(-15, -10).toString()); // outside head (excluding to)
		assertEquals("[]", set.subSet(-15, -12).toString()); // outside head (completely)
	}

	@Test
	public void sortedSet_subSetEmpty_fail() {
		SortedSet<Integer> set = Sets.newTreeSet();

		set = set.subSet(-5, 5);

		exception.expect(IllegalArgumentException.class);
		set.subSet(-10, 10);
	}


}
