package data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class ParticipantTest {

	@Test
	public void constructorStoresAllFields() {
		Participant p = new Participant("Max", "Mustermann", "max@example.org", "5");

		assertEquals("Max", p.getFirstName());
		assertEquals("Mustermann", p.getLastName());
		assertEquals("max@example.org", p.geteMailAdress());
		assertEquals("5", p.getGroupNumber());
		assertEquals(0, p.getNumberOfReviews());
		assertTrue(p.getReviews().isEmpty());
	}

	@Test
	public void settersOverwriteFields() {
		Participant p = new Participant("a", "b", "c", "1");
		p.setFirstName("Sabine");
		p.setLastName("Musterfrau");
		p.seteMailAdress("sabine@example.org");
		p.setGroupNumber("4");
		p.setNumberOfReviews(7);

		assertEquals("Sabine", p.getFirstName());
		assertEquals("Musterfrau", p.getLastName());
		assertEquals("sabine@example.org", p.geteMailAdress());
		assertEquals("4", p.getGroupNumber());
		assertEquals(7, p.getNumberOfReviews());
	}

	@Test
	public void participationCounterGoesUpAndDown() {
		Participant p = new Participant("a", "b", "c", "1");
		p.increaseParticipation();
		p.increaseParticipation();
		assertEquals(2, p.getNumberOfReviews());
		p.decreaseParticipation();
		assertEquals(1, p.getNumberOfReviews());
	}

	@Test
	public void reviewsCanBeAddedReplacedAndReset() {
		Participant author = new Participant("a", "b", "c", "1");
		Review review = new Review(author);

		author.addReview(review);
		assertEquals(1, author.getReviews().size());
		assertSame(review, author.getReviews().get(0));

		ArrayList<Review> replacement = new ArrayList<Review>();
		author.setReviews(replacement);
		assertSame(replacement, author.getReviews());

		author.addReview(review);
		author.resetReviews();
		assertTrue(author.getReviews().isEmpty());
	}

	@Test
	public void compareToSortsByGroupNumberDescending() {
		Participant g1 = new Participant("a", "a", "a", "1");
		Participant g2 = new Participant("b", "b", "b", "2");
		Participant g3 = new Participant("c", "c", "c", "3");

		assertTrue(g1.compareTo(g2) > 0);
		assertTrue(g2.compareTo(g1) < 0);
		assertEquals(0, g2.compareTo(new Participant("x", "x", "x", "2")));

		List<Participant> list = new ArrayList<Participant>(Arrays.asList(g2, g1, g3));
		Collections.sort(list);
		// members of one group end up next to each other, which the
		// Matcher relies on when it creates one review per group
		assertEquals(Arrays.asList(g3, g2, g1), list);
	}
}
