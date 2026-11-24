package data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import testutil.AppState;

public class ReviewTest {

	private Participant author;
	private Review review;

	@Before
	public void setUp() {
		AppState.reset();
		author = AppState.participant("Author", "3");
		review = new Review(author);
	}

	@After
	public void tearDown() {
		AppState.reset();
	}

	@Test
	public void newReviewTakesAuthorAndGroupFromAuthor() {
		assertSame(author, review.getAuthor());
		assertEquals("3", review.getGroupNumber());
		assertNull(review.getScribe());
		assertNull(review.getModerator());
		assertNull(review.getAssignedRoom());
		assertNull(review.getLetter());
		assertTrue(review.getReviewers().isEmpty());
	}

	@Test
	public void rolesAndRoomCanBeAssigned() {
		Participant scribe = AppState.participant("Scribe", "1");
		Participant moderator = AppState.participant("Mod", "2");
		Participant other = AppState.participant("Other", "4");
		Room room = AppState.room("A1", true, "10:00", "11:00");

		review.setScribe(scribe);
		review.setModerator(moderator);
		review.setAuthor(other);
		review.setAssignedRoom(room);
		review.setGroupNumber("9");
		review.setLetter("B");

		assertSame(scribe, review.getScribe());
		assertSame(moderator, review.getModerator());
		assertSame(other, review.getAuthor());
		assertSame(room, review.getAssignedRoom());
		assertEquals("9", review.getGroupNumber());
		assertEquals("B", review.getLetter());
	}

	@Test
	public void reviewerIsAddedOnlyOnce() {
		Participant reviewer = AppState.participant("Rev", "1");

		assertTrue(review.addReviewer(reviewer));
		assertFalse(review.addReviewer(reviewer));
		assertEquals(1, review.getReviewers().size());
	}

	@Test
	public void removeReviewerReportsWhetherReviewerWasPresent() {
		Participant reviewer = AppState.participant("Rev", "1");
		review.addReviewer(reviewer);

		assertTrue(review.removeReviewer(reviewer));
		assertFalse(review.removeReviewer(reviewer));
		assertTrue(review.getReviewers().isEmpty());
	}

	@Test
	public void reviewPlanKeepsFinalAndTemporaryListsSeparate() {
		ReviewPlan plan = ReviewPlan.getInstance();
		Review other = new Review(AppState.participant("X", "7"));

		plan.add(review);
		plan.addTemp(other);
		assertEquals(1, plan.getReviews().size());
		assertSame(review, plan.getReviews().get(0));
		assertEquals(1, plan.getTempReviews().size());
		assertSame(other, plan.getTempReviews().get(0));

		plan.resetLists();
		assertTrue(plan.getReviews().isEmpty());
		assertTrue(plan.getTempReviews().isEmpty());
	}
}
