package io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import testutil.AppState;

public class FileProcessorTest {

	/** ReviewOger writes Character.LINE_SEPARATOR (13) as line break */
	private static final String CR = "\r";

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private final FileProcessor processor = new FileProcessor();

	@Before
	public void setUp() {
		AppState.reset();
	}

	@After
	public void tearDown() {
		AppState.reset();
	}

	// --------------------------------------------------------- CSV import

	@Test
	public void processLineCreatesATrimmedParticipant() {
		processor.processLine(" Max ; Mustermann ;max@example.org ; 05 ");

		List<Participant> participants = ParticipantTableModel.getInstance().getParticipants();
		assertEquals(1, participants.size());
		Participant p = participants.get(0);
		assertEquals("Max", p.getFirstName());
		assertEquals("Mustermann", p.getLastName());
		assertEquals("max@example.org", p.geteMailAdress());
		// "05" and "5" must be the same group
		assertEquals("5", p.getGroupNumber());
	}

	@Test
	public void processLineIgnoresBlankLines() {
		processor.processLine("");
		processor.processLine("   ");
		assertTrue(ParticipantTableModel.getInstance().isEmpty());
	}

	@Test
	public void processLineRejectsMissingFields() {
		assertThrows(ArrayIndexOutOfBoundsException.class,
				() -> processor.processLine("Max;Mustermann;max@example.org"));
	}

	@Test
	public void processLineRejectsNonNumericGroups() {
		assertThrows(NumberFormatException.class,
				() -> processor.processLine("Max;Mustermann;max@example.org;five"));
	}

	@Test
	public void readParticipantListImportsEveryLine() throws IOException {
		File csv = csv("Max;Mustermann;max@example.org;5",
				"",
				"Sabine;Musterfrau;sabine@example.org;4",
				"Erika;Mustermann;erika@example.org;4");

		processor.readParticipantList(csv.getPath());

		List<Participant> participants = ParticipantTableModel.getInstance().getParticipants();
		assertEquals(3, participants.size());
		assertEquals("Sabine", participants.get(1).getFirstName());
		assertEquals("4", participants.get(2).getGroupNumber());
		assertNoLogFile();
	}

	@Test
	public void brokenLinesAreLoggedWithTheirLineNumber() throws IOException {
		// at the end the user is told about the log in a dialog, which is
		// only testable (as HeadlessException) without a display
		assumeTrue(GraphicsEnvironment.isHeadless());
		File csv = csv("Max;Mustermann;max@example.org;5",
				"Sabine;Musterfrau",
				"Erika;Mustermann;erika@example.org;vier");

		assertThrows(HeadlessException.class,
				() -> processor.readParticipantList(csv.getPath()));

		assertEquals(1, ParticipantTableModel.getInstance().getRowCount());
		File[] logs = logFiles();
		assertEquals(1, logs.length);
		String log = new String(Files.readAllBytes(logs[0].toPath()), StandardCharsets.UTF_8);
		assertTrue(log, log.contains("Fehler in Zeile 2 Zu wenig Argumente"));
		assertTrue(log, log.contains("Fehler in Zeile 3 Gruppennummer keine Zahl"));
	}

	// ------------------------------------------------------ result output

	@Test
	public void printResultListsEveryRoleWithMailAddress() throws IOException {
		File out = new File(tmp.getRoot(), "result.txt");

		processor.printResult(new ArrayList<Review>(Arrays.asList(fullReview(false))), out.getPath());

		assertEquals(Arrays.asList(
				"Review A: Gruppe 5 am 01.12.26 von 08:00 Uhr bis 12:00 Uhr in Raum A1",
				"Beamer benötigt",
				"Autor: Author LastAuthor author@example.org Gruppe 5",
				"Moderator: Mod LastMod mod@example.org Gruppe 2",
				"Notar: Scribe LastScribe scribe@example.org Gruppe 3",
				"Gutachter: Rev1 LastRev1 rev1@example.org Gruppe 4",
				"Gutachter: Rev2 LastRev2 rev2@example.org Gruppe 6",
				"*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x"),
				lines(out));
	}

	@Test
	public void printResultNoMailWritesASecondFileWithoutAddresses() throws IOException {
		File out = new File(tmp.getRoot(), "result.txt");

		processor.printResultNoMail(new ArrayList<Review>(Arrays.asList(fullReview(false))),
				out.getPath());

		assertFalse(out.exists());
		File noMail = new File(tmp.getRoot(), "result-no-mail.txt");
		assertEquals(Arrays.asList(
				"Review A: Gruppe 5 am 01.12.26 von 08:00 Uhr bis 12:00 Uhr in Raum A1",
				"Beamer benötigt",
				"Autor: Author LastAuthor  Gruppe 5",
				"Moderator: Mod LastMod  Gruppe 2",
				"Notar: Scribe LastScribe  Gruppe 3",
				"Gutachter: Rev1 LastRev1  Gruppe 4",
				"Gutachter: Rev2 LastRev2  Gruppe 6",
				"*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x*x"),
				lines(noMail));
	}

	@Test
	public void roomWithBeamerAndMissingRolesAreLeftOut() throws IOException {
		Review review = bareReview(true);
		File out = new File(tmp.getRoot(), "result.txt");

		processor.printResult(new ArrayList<Review>(Arrays.asList(review)), out.getPath());
		processor.printResultNoMail(new ArrayList<Review>(Arrays.asList(review)), out.getPath());

		for (File f : new File[] { out, new File(tmp.getRoot(), "result-no-mail.txt") }) {
			List<String> lines = lines(f);
			assertEquals(3, lines.size());
			assertFalse(lines.contains("Beamer benötigt"));
			assertTrue(lines.get(1).startsWith("Autor: "));
		}
	}

	@Test
	public void namesAreWrittenWithoutSpaces() throws IOException {
		Review review = bareReview(true);
		review.getAuthor().setFirstName("Anna Lena");
		review.getAuthor().setLastName("von Berg");
		File out = new File(tmp.getRoot(), "result.txt");

		processor.printResult(new ArrayList<Review>(Arrays.asList(review)), out.getPath());

		assertEquals("Autor: AnnaLena vonBerg author@example.org Gruppe 5", lines(out).get(1));
	}

	// ------------------------------------------------------ error dialogs
	// Without a display, "the user is shown an error dialog" surfaces as a
	// HeadlessException, which makes the error handling testable in CI.

	@Test
	public void missingCsvFileIsReportedToTheUser() {
		assumeTrue(GraphicsEnvironment.isHeadless());
		String missing = new File(tmp.getRoot(), "missing.csv").getPath();

		assertThrows(HeadlessException.class, () -> processor.readParticipantList(missing));
	}

	@Test
	public void unwritableResultFileIsReportedToTheUser() {
		assumeTrue(GraphicsEnvironment.isHeadless());
		ArrayList<Review> reviews = new ArrayList<Review>(Arrays.asList(fullReview(true)));
		// a directory cannot be opened as a file
		String directory = tmp.getRoot().getPath();

		assertThrows(HeadlessException.class, () -> processor.printResult(reviews, directory));
		assertThrows(HeadlessException.class,
				() -> processor.printResultNoMail(reviews, directory + "/sub/x.txt"));
	}

	// ------------------------------------------------------------ helpers

	private static Review fullReview(boolean beamer) {
		Review review = bareReview(beamer);
		review.setModerator(AppState.participant("Mod", "2"));
		review.setScribe(AppState.participant("Scribe", "3"));
		review.addReviewer(AppState.participant("Rev1", "4"));
		review.addReviewer(AppState.participant("Rev2", "6"));
		return review;
	}

	private static Review bareReview(boolean beamer) {
		Slot slot = AppState.slot("01.12.26", "08:00", "12:00");
		Room room = AppState.room("A1", beamer, "08:00", "09:00");
		room.setSlot(slot);
		Review review = new Review(AppState.participant("Author", "5"));
		review.setAssignedRoom(room);
		review.setLetter("A");
		return review;
	}

	private File csv(String... lines) throws IOException {
		File file = tmp.newFile("participants.csv");
		Files.write(file.toPath(), Arrays.asList(lines), StandardCharsets.UTF_8);
		return file;
	}

	private static List<String> lines(File file) throws IOException {
		String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		return Arrays.asList(content.split(CR));
	}

	private File[] logFiles() {
		return tmp.getRoot().listFiles((dir, name) -> name.startsWith("log") && name.endsWith(".txt"));
	}

	private void assertNoLogFile() {
		assertEquals(0, logFiles().length);
	}
}
