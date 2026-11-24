package logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import data.Participant;
import data.ParticipantTableModel;
import data.Review;
import data.Room;
import data.Slot;
import testutil.AppState;

/**
 * Small, hand-built scheduling scenarios: 4 groups of 3 students, 2 slots
 * with 2 rooms each. Every student has to take part in exactly 2 reviews,
 * so each slot needs all 12 students.
 */
public class MatcherScenarioTest {

	private static final int MAX_ATTEMPTS = 5000;

	private Slot slot1;
	private Slot slot2;

	@Before
	public void setUp() {
		AppState.reset();
		for (String group : new String[] { "1", "2", "3", "4" }) {
			AppState.addGroup(group, 3);
		}
		slot1 = AppState.slot("01.12.26", "08:00", "10:00");
		slot2 = AppState.slot("01.12.26", "10:00", "12:00");
		AppState.addSlotToTree(slot1,
				AppState.room("A1", true, "08:00", "09:00"),
				AppState.room("A2", false, "08:00", "09:00"));
		AppState.addSlotToTree(slot2,
				AppState.room("A1", true, "10:00", "11:00"),
				AppState.room("A2", false, "10:00", "11:00"));
	}

	@After
	public void tearDown() {
		AppState.reset();
	}

	@Test
	public void separateScribeProducesAValidPlan() {
		Matcher matcher = new Matcher("feste Anzahl", false, false);
		ArrayList<Review> plan = matchUntilFound(matcher);

		assertEquals(4, plan.size());
		for (Review review : plan) {
			assertNotSame(review.getAuthor(), review.getScribe());
			// author + scribe + moderator + 3 reviewers = 6 = 2 * 12 / 4
			assertEquals(3, review.getReviewers().size());
		}
		assertCommonInvariants(plan);
	}

	@Test
	public void authorAsScribeProducesAValidPlan() {
		Matcher matcher = new Matcher("feste Anzahl", true, false);
		ArrayList<Review> plan = matchUntilFound(matcher);

		assertEquals(4, plan.size());
		for (Review review : plan) {
			assertSame(review.getAuthor(), review.getScribe());
			// author(=scribe) + moderator + 4 reviewers = 6
			assertEquals(4, review.getReviewers().size());
		}
		assertCommonInvariants(plan);
	}

	@Test
	public void matcherCollectsAndSortsTheSlotsFromTheTree() {
		Matcher matcher = new Matcher("feste Anzahl", false, false);
		assertTrue(matcher.isFirstRound());

		matcher.MatchReview();

		assertEquals(2, matcher.getSlots().size());
		for (Slot s : matcher.getSlots()) {
			assertEquals(2, s.getRooms().size());
			for (Room r : s.getRooms()) {
				assertSame(s, r.getSlot());
			}
		}
	}

	@Test
	public void slotWithMostRoomsIsFilledFirst() {
		AppState.reset();
		AppState.addGroup("1", 3);
		AppState.addGroup("2", 3);
		Slot small = AppState.slot("01.12.26", "08:00", "10:00");
		Slot big = AppState.slot("02.12.26", "08:00", "10:00");
		AppState.addSlotToTree(small, AppState.room("S1", true, "08:00", "09:00"));
		AppState.addSlotToTree(big,
				AppState.room("B1", true, "08:00", "09:00"),
				AppState.room("B2", true, "08:00", "09:00"),
				AppState.room("B3", true, "08:00", "09:00"));

		Matcher matcher = new Matcher("feste Anzahl", false, false);
		matcher.MatchReview();

		assertSame(big, matcher.getSlots().get(0));
		assertSame(small, matcher.getSlots().get(1));
	}

	@Test
	public void oneReviewPerGroupEvenIfTheListIsNotSortedByGroup() {
		// participants typed in "interleaved" (1,2,3,4,1,2,3,4,...)
		AppState.reset();
		for (int member = 1; member <= 3; member++) {
			for (String group : new String[] { "1", "2", "3", "4" }) {
				ParticipantTableModel.getInstance().addParticipant(
						AppState.participant("G" + group + "M" + member, group));
			}
		}
		AppState.addSlotToTree(slot1,
				AppState.room("A1", true, "08:00", "09:00"),
				AppState.room("A2", false, "08:00", "09:00"));
		AppState.addSlotToTree(slot2,
				AppState.room("A1", true, "10:00", "11:00"),
				AppState.room("A2", false, "10:00", "11:00"));

		ArrayList<Review> plan = matchUntilFound(new Matcher("feste Anzahl", false, false));

		assertEquals(4, plan.size());
		assertCommonInvariants(plan);
	}

	@Test
	public void callerGetsAPlanWithReviewLetters() throws Exception {
		// ExecutingFrame runs the matcher through Callable.call()
		Matcher matcher = new Matcher("feste Anzahl", false, false);
		ArrayList<Review> plan = null;
		for (int i = 0; i < MAX_ATTEMPTS && plan == null; i++) {
			plan = matcher.call();
		}
		assertNotNull(plan);

		List<String> letters = new ArrayList<String>();
		for (Review r : plan) {
			letters.add(r.getLetter());
		}
		assertEquals(java.util.Arrays.asList("A", "B", "C", "D"), letters);
	}

	@Test
	public void failedCallReturnsNullAndAssignsNoLetters() throws Exception {
		AppState.reset();
		AppState.addGroup("1", 3);
		AppState.addGroup("2", 3);
		// no slots at all - nothing can be placed
		Matcher matcher = new Matcher("feste Anzahl", false, false);

		assertEquals(null, matcher.call());
		for (Review r : data.ReviewPlan.getInstance().getTempReviews()) {
			assertEquals(null, r.getLetter());
		}
	}

	@Test
	public void runningANewCalculationDoesNotDuplicateRooms() {
		// the GUI creates a new Matcher for every calculation
		new Matcher("feste Anzahl", false, false).MatchReview();
		new Matcher("feste Anzahl", false, false).MatchReview();

		assertEquals(2, slot1.getRooms().size());
		assertEquals(2, slot2.getRooms().size());
	}

	@Test
	public void failedRoundIsReportedAsNull() {
		// one slot with one room cannot hold 4 reviews
		AppState.reset();
		for (String group : new String[] { "1", "2", "3", "4" }) {
			AppState.addGroup(group, 3);
		}
		AppState.addSlotToTree(AppState.slot("01.12.26", "08:00", "10:00"),
				AppState.room("A1", true, "08:00", "09:00"));

		Matcher matcher = new Matcher("feste Anzahl", false, false);
		assertEquals(null, matcher.MatchReview());
		assertFalse(matcher.isFirstRound());
		matcher.setFirstRound(true);
		assertTrue(matcher.isFirstRound());
	}

	private static ArrayList<Review> matchUntilFound(Matcher matcher) {
		for (int i = 0; i < MAX_ATTEMPTS; i++) {
			ArrayList<Review> result = matcher.MatchReview();
			if (result != null) {
				return result;
			}
		}
		throw new AssertionError("no plan found in " + MAX_ATTEMPTS + " attempts");
	}

	private static void assertCommonInvariants(List<Review> plan) {
		Set<String> authorGroups = new HashSet<String>();
		for (Review review : plan) {
			assertNotNull(review.getAssignedRoom());
			assertSame(review, review.getAssignedRoom().getReview());
			assertNotNull(review.getModerator());
			assertTrue(authorGroups.add(review.getGroupNumber()));
			for (Participant p : rolesOf(review)) {
				if (p != review.getAuthor()) {
					assertNotEquals("nobody reviews their own group",
							review.getGroupNumber(), p.getGroupNumber());
				}
			}
			assertEquals("nobody has two roles in one review",
					rolesOf(review).size(), new HashSet<Participant>(rolesOf(review)).size());
		}

		for (Participant p : ParticipantTableModel.getInstance().getParticipants()) {
			assertEquals(p.getFirstName() + " takes part in two reviews",
					2, p.getReviews().size());
			assertEquals("counter matches the reviews of " + p.getFirstName(),
					p.getReviews().size(), p.getNumberOfReviews());
			assertNotSame("never twice in the same slot",
					p.getReviews().get(0).getAssignedRoom().getSlot(),
					p.getReviews().get(1).getAssignedRoom().getSlot());
		}
	}

	private static List<Participant> rolesOf(Review review) {
		List<Participant> roles = new ArrayList<Participant>();
		roles.add(review.getAuthor());
		roles.add(review.getModerator());
		if (review.getScribe() != review.getAuthor()) {
			roles.add(review.getScribe());
		}
		roles.addAll(review.getReviewers());
		return roles;
	}
}
