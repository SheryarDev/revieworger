package logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
import data.ReviewPlan;
import data.Room;
import data.Slot;
import testutil.AppState;

public class RandomFunctionsTest {

	private Slot slot;
	private Review review;

	@Before
	public void setUp() {
		AppState.reset();
		Participant author = AppState.participant("Author", "1");
		ParticipantTableModel.getInstance().addParticipant(author);
		AppState.addGroup("2", 3);
		AppState.addGroup("3", 3);

		slot = AppState.slot("01.12.26", "08:00", "12:00");
		Room room = AppState.room("A1", true, "08:00", "09:00");
		slot.addRoom(room);
		review = new Review(author);
		room.setReview(review);
		review.setAssignedRoom(room);
	}

	@After
	public void tearDown() {
		AppState.reset();
	}

	@Test
	public void selectedReviewerComesFromThePoolAndIsRemovedFromIt() {
		Pool pool = new Pool(slot);
		List<Participant> candidates = new ArrayList<Participant>(
				pool.generatePoolForReviewers(review));
		assertEquals(6, candidates.size());

		Set<Participant> picked = new HashSet<Participant>();
		for (int i = 0; i < candidates.size(); i++) {
			Participant reviewer = RandomFunctions.selectReviewerFromPool(pool);
			assertTrue(candidates.contains(reviewer));
			assertFalse(pool.getReviewers().contains(reviewer));
			assertEquals(candidates.size() - i - 1, pool.getCurrentReviewerPoolsize());
			picked.add(reviewer);
		}
		// nobody can be drawn twice
		assertEquals(candidates.size(), picked.size());
	}

	@Test
	public void selectedModeratorComesFromThePoolAndStaysInIt() {
		Pool pool = new Pool(slot);
		List<Participant> candidates = new ArrayList<Participant>(
				pool.generatePoolForModerator(review, false, new ArrayList<Participant>()));

		Participant moderator = RandomFunctions.selectModeratorFromPool(pool);

		assertTrue(candidates.contains(moderator));
		assertEquals(candidates.size(), pool.getCurrentModeratorPoolsize());
	}

	@Test
	public void shuffleMovesEveryReviewExactlyOnceAndClearsTheAssignment() {
		// repeated because the order is random: sampling with replacement
		// (the old bug) is caught with near certainty after a few rounds
		for (int round = 0; round < 25; round++) {
			ReviewPlan plan = ReviewPlan.getInstance();
			plan.resetLists();
			// left over from an earlier attempt, must not survive the shuffle
			plan.addTemp(new Review(AppState.participant("Stale", "x")));
			List<Review> original = new ArrayList<Review>();
			for (int i = 0; i < 6; i++) {
				Review r = new Review(AppState.participant("A" + i, "g" + i));
				r.setAssignedRoom(AppState.room("R" + i, true, "08:00", "09:00"));
				r.setModerator(AppState.participant("M" + i, "m"));
				r.setScribe(AppState.participant("S" + i, "s"));
				r.addReviewer(AppState.participant("V" + i, "v"));
				plan.add(r);
				original.add(r);
			}

			RandomFunctions.shuffleReviews(plan);

			assertTrue(plan.getReviews().isEmpty());
			assertEquals(original.size(), plan.getTempReviews().size());
			assertEquals(new HashSet<Review>(original),
					new HashSet<Review>(plan.getTempReviews()));
			for (Review r : plan.getTempReviews()) {
				assertNull(r.getAssignedRoom());
				assertNull(r.getModerator());
				assertNull(r.getScribe());
				assertTrue(r.getReviewers().isEmpty());
			}
		}
	}
}
