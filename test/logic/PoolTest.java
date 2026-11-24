package logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import data.Participant;
import data.ParticipantTableModel;
import data.Review;
import data.Room;
import data.Slot;
import testutil.AppState;

public class PoolTest {

	private Participant author;
	private Participant authorMate;
	private Participant b1;
	private Participant b2;
	private Participant c1;
	private Slot slot;
	private Room room;
	private Review review;

	@Before
	public void setUp() {
		AppState.reset();
		author = add("Author", "1");
		authorMate = add("Mate", "1");
		b1 = add("B1", "2");
		b2 = add("B2", "2");
		c1 = add("C1", "3");

		slot = AppState.slot("01.12.26", "08:00", "12:00");
		room = AppState.room("A1", true, "10:00", "11:00");
		slot.addRoom(room);
		room.setSlot(slot);

		review = new Review(author);
		room.setReview(review);
		review.setAssignedRoom(room);
	}

	@After
	public void tearDown() {
		AppState.reset();
	}

	// ------------------------------------------------------- reviewer pool

	@Test
	public void reviewerPoolContainsEveryoneOutsideTheAuthorsGroup() {
		List<Participant> pool = new Pool(slot).generatePoolForReviewers(review);

		assertEquals(3, pool.size());
		assertTrue(pool.contains(b1));
		assertTrue(pool.contains(b2));
		assertTrue(pool.contains(c1));
		assertFalse(pool.contains(author));
		assertFalse(pool.contains(authorMate));
	}

	@Test
	public void reviewerPoolSkipsParticipantsWithTwoReviews() {
		c1.setNumberOfReviews(2);
		b1.setNumberOfReviews(1);

		List<Participant> pool = new Pool(slot).generatePoolForReviewers(review);

		assertFalse(pool.contains(c1));
		assertTrue(pool.contains(b1));
	}

	@Test
	public void reviewerPoolSkipsParticipantsAlreadyInTheReview() {
		review.addReviewer(b1);
		review.setModerator(b2);
		review.setScribe(c1);

		assertTrue(new Pool(slot).generatePoolForReviewers(review).isEmpty());
	}

	@Test
	public void reviewNotYetVisibleInTheSlotStillExcludesItsMembers() {
		// the review knows its room, but the room does not show the review
		// yet - only the "already in this review" rule protects it now
		room.setReview(null);
		review.addReviewer(b1);
		review.setModerator(b2);
		review.setScribe(c1);

		assertTrue(new Pool(slot).generatePoolForReviewers(review).isEmpty());
		for (boolean notReviewerGroup : new boolean[] { true, false }) {
			assertTrue(new Pool(slot).generatePoolForModerator(review,
					notReviewerGroup, new ArrayList<Participant>()).isEmpty());
		}
	}

	@Test
	public void reviewNotYetVisibleInTheSlotKeepsOtherCandidates() {
		room.setReview(null);
		review.addReviewer(b1);

		List<Participant> pool = new Pool(slot).generatePoolForReviewers(review);

		assertEquals(2, pool.size());
		assertFalse(pool.contains(b1));
		assertFalse(pool.contains(author));
	}

	@Test
	public void reviewerPoolSkipsParticipantsBusyInAnotherRoomOfTheSlot() {
		Room otherRoom = AppState.room("B2", true, "10:00", "11:00");
		slot.addRoom(otherRoom);
		Review otherReview = new Review(c1);
		otherRoom.setReview(otherReview);

		List<Participant> pool = new Pool(slot).generatePoolForReviewers(review);

		assertFalse(pool.contains(c1));
		assertEquals(2, pool.size());
	}

	@Test
	public void reviewerPoolSkipsParticipantsWithAnOverlappingReview() {
		// b1 already reviews 09:30-10:30 somewhere else, b2 10:30-11:30,
		// c1 has a review that does not overlap (11:00-12:00)
		assignOtherReview(b1, "09:30", "10:30");
		assignOtherReview(b2, "10:30", "11:30");
		assignOtherReview(c1, "11:00", "12:00");

		List<Participant> pool = new Pool(slot).generatePoolForReviewers(review);

		assertFalse(pool.contains(b1));
		assertFalse(pool.contains(b2));
		assertTrue(pool.contains(c1));
	}

	@Test
	public void reviewOfAnAuthorWithoutRoomDoesNotCountAsOverlap() {
		// b1 is author of a review that has not been placed yet
		b1.addReview(new Review(b1));

		assertTrue(new Pool(slot).generatePoolForReviewers(review).contains(b1));
	}

	@Test
	public void removeParticipantShrinksTheReviewerPool() {
		Pool pool = new Pool(slot);
		pool.generatePoolForReviewers(review);
		assertEquals(3, pool.getCurrentReviewerPoolsize());

		pool.removeParticipant(b1);

		assertEquals(2, pool.getCurrentReviewerPoolsize());
		assertFalse(pool.getReviewers().contains(b1));
	}

	// ------------------------------------------------------ moderator pool

	@Test
	public void moderatorMayNotComeFromAReviewerGroupWhenRequested() {
		review.addReviewer(b1);
		ArrayList<Participant> reviewers = review.getReviewers();

		Pool pool = new Pool(slot);
		List<Participant> moderators = pool.generatePoolForModerator(review, true, reviewers);

		assertEquals(1, moderators.size());
		assertTrue(moderators.contains(c1));
		assertEquals(1, pool.getCurrentModeratorPoolsize());
		assertTrue(pool.getModerators().contains(c1));
	}

	@Test
	public void moderatorMayComeFromAReviewerGroupOtherwise() {
		review.addReviewer(b1);

		List<Participant> moderators = new Pool(slot).generatePoolForModerator(review,
				false, review.getReviewers());

		assertEquals(2, moderators.size());
		assertTrue(moderators.contains(b2));
		assertTrue(moderators.contains(c1));
	}

	@Test
	public void moderatorPoolAppliesTheSameBasicRulesAsTheReviewerPool() {
		c1.setNumberOfReviews(2);
		assignOtherReview(b2, "09:30", "10:30");

		for (boolean notReviewerGroup : new boolean[] { true, false }) {
			List<Participant> moderators = new Pool(slot).generatePoolForModerator(
					review, notReviewerGroup, new ArrayList<Participant>());
			assertEquals(1, moderators.size());
			assertTrue(moderators.contains(b1));
		}
	}

	@Test
	public void moderatorPoolSkipsParticipantsInTheReviewOrSlot() {
		review.setScribe(b1);
		Room otherRoom = AppState.room("B2", true, "10:00", "11:00");
		slot.addRoom(otherRoom);
		otherRoom.setReview(new Review(c1));

		for (boolean notReviewerGroup : new boolean[] { true, false }) {
			List<Participant> moderators = new Pool(slot).generatePoolForModerator(
					review, notReviewerGroup, new ArrayList<Participant>());
			assertEquals(1, moderators.size());
			assertTrue(moderators.contains(b2));
		}
	}

	// ------------------------------------------------- participant in slot

	@Test
	public void participantInSlotFindsEveryRole() {
		review.setModerator(b1);
		review.setScribe(b2);
		review.addReviewer(c1);

		assertTrue(Pool.ParticipantInSlot(author, slot));
		assertTrue(Pool.ParticipantInSlot(b1, slot));
		assertTrue(Pool.ParticipantInSlot(b2, slot));
		assertTrue(Pool.ParticipantInSlot(c1, slot));
		assertFalse(Pool.ParticipantInSlot(authorMate, slot));
	}

	@Test
	public void participantInSlotLooksAtAllRoomsEvenIfOneIsStillEmpty() {
		Slot twoRooms = AppState.slot("02.12.26", "08:00", "12:00");
		Room empty = AppState.room("E1", true, "08:00", "09:00");
		Room used = AppState.room("E2", true, "08:00", "09:00");
		twoRooms.addRoom(empty);
		twoRooms.addRoom(used);
		used.setReview(new Review(c1));

		assertTrue(Pool.ParticipantInSlot(c1, twoRooms));
	}

	private Participant add(String name, String group) {
		Participant p = AppState.participant(name, group);
		ParticipantTableModel.getInstance().addParticipant(p);
		return p;
	}

	private void assignOtherReview(Participant p, String begin, String end) {
		Slot otherSlot = AppState.slot("03.12.26", "08:00", "12:00");
		Room otherRoom = AppState.room("X", true, begin, end);
		otherSlot.addRoom(otherRoom);
		Review other = new Review(AppState.participant("Foreign", "9"));
		other.setAssignedRoom(otherRoom);
		p.addReview(other);
	}
}
