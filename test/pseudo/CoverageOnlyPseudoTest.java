package pseudo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import data.Participant;
import data.ParticipantTableModel;
import data.Review;
import data.Room;
import data.Slot;
import data.SortSlot;
import io.FileProcessor;
import io.TableOutputter;
import logic.Matcher;
import logic.Pool;
import testutil.AppState;

/**
 * PSEUDO-TESTS (exercise 3, task 3) - deliberately bad on purpose.
 *
 * Every method below executes production code but asserts nothing.
 * A coverage tool such as JaCoCo counts all of these lines as "covered",
 * yet the tests can never fail, so PIT's mutants survive. Run them in
 * isolation with "ant pit-pseudo" and compare reports/pit-pseudo with
 * reports/pit: similar line coverage, very different mutation score.
 *
 * They are excluded from "ant coverage" and "ant pit" so they do not
 * inflate the real numbers.
 */
public class CoverageOnlyPseudoTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Before
	public void setUp() {
		AppState.reset();
	}

	@After
	public void tearDown() {
		AppState.reset();
	}

	@Test
	public void touchesTheDataModel() {
		Participant a = AppState.participant("A", "1");
		Participant b = AppState.participant("B", "2");
		a.compareTo(b);
		a.increaseParticipation();
		a.decreaseParticipation();

		Review review = new Review(a);
		review.addReviewer(b);
		review.addReviewer(b);
		review.removeReviewer(b);

		Slot slot = AppState.slot("01.12.26", "08:00", "12:00");
		Slot other = AppState.slot("02.12.26", "08:00", "12:00");
		slot.addRoom(AppState.room("A1", true, "08:00", "09:00"));
		slot.compareTo(other);
		other.compareTo(slot);
		slot.getFormatedDate();

		SortSlot s1 = new SortSlot(AppState.date("01.12.26"), AppState.time("08:00"), AppState.time("09:00"));
		SortSlot s2 = new SortSlot(AppState.date("01.12.26"), AppState.time("10:00"), AppState.time("11:00"));
		s1.compareTo(s2);
		s2.compareTo(s1);

		ParticipantTableModel model = ParticipantTableModel.getInstance();
		model.addParticipant(a);
		for (int col = 0; col < 5; col++) {
			model.getColumnName(col);
			if (col < 4) {
				model.getValueAt(0, col);
			}
		}
	}

	@Test
	public void runsTheMatcherAndIgnoresTheResult() {
		for (String group : new String[] { "1", "2", "3", "4" }) {
			AppState.addGroup(group, 3);
		}
		AppState.addSlotToTree(AppState.slot("01.12.26", "08:00", "10:00"),
				AppState.room("A1", true, "08:00", "09:00"),
				AppState.room("A2", false, "08:00", "09:00"));
		AppState.addSlotToTree(AppState.slot("01.12.26", "10:00", "12:00"),
				AppState.room("A1", true, "10:00", "11:00"),
				AppState.room("A2", false, "10:00", "11:00"));

		Matcher matcher = new Matcher("feste Anzahl", false, true);
		for (int i = 0; i < 50; i++) {
			matcher.MatchReview();
		}
		Matcher authorIsScribe = new Matcher("feste Anzahl", true, false);
		for (int i = 0; i < 50; i++) {
			authorIsScribe.MatchReview();
		}
	}

	@Test
	public void buildsPoolsWithoutLookingAtThem() {
		Participant author = AppState.participant("Author", "1");
		ParticipantTableModel.getInstance().addParticipant(author);
		AppState.addGroup("2", 2);
		Slot slot = AppState.slot("01.12.26", "08:00", "12:00");
		Room room = AppState.room("A1", true, "08:00", "09:00");
		slot.addRoom(room);
		Review review = new Review(author);
		room.setReview(review);
		review.setAssignedRoom(room);

		Pool pool = new Pool(slot);
		pool.generatePoolForReviewers(review);
		pool.generatePoolForModerator(review, true, review.getReviewers());
		pool.generatePoolForModerator(review, false, review.getReviewers());
		Pool.ParticipantInSlot(author, slot);
	}

	@Test
	public void writesOutputFilesNobodyReads() throws Exception {
		Slot slot = AppState.slot("01.12.26", "08:00", "12:00");
		Room room = AppState.room("A1", false, "08:00", "09:00");
		room.setSlot(slot);
		slot.addRoom(room);
		Review review = new Review(AppState.participant("Author", "5"));
		review.setModerator(AppState.participant("Mod", "2"));
		review.setScribe(AppState.participant("Scribe", "3"));
		review.addReviewer(AppState.participant("Rev", "4"));
		review.setAssignedRoom(room);
		room.setReview(review);
		review.setLetter("A");
		ArrayList<Review> reviews = new ArrayList<Review>(Arrays.asList(review));

		File out = new File(tmp.getRoot(), "result.txt");
		new FileProcessor().printResult(reviews, out.getPath());
		new FileProcessor().printResultNoMail(reviews, out.getPath());

		Matcher matcher = new Matcher("feste Anzahl", false, false);
		matcher.getSlots().add(slot);
		TableOutputter.createTable(new File(tmp.getRoot(), "plan.tex").getPath(), reviews, matcher);
	}
}
