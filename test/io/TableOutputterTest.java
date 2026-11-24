package io;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import data.Review;
import data.Room;
import data.Slot;
import data.SortSlot;
import logic.Matcher;
import testutil.AppState;

public class TableOutputterTest {

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
	public void createArrayPlacesReviewLettersInTheirRoomColumn() {
		SortSlot first = sortSlot("01.12.26",
				roomWithReview("A1", true, "A"), roomWithReview("B2", false, null));
		SortSlot second = sortSlot("02.12.26", roomWithReview("B2", false, "B"));

		String[][] table = TableOutputter.createArray(
				new ArrayList<String>(Arrays.asList("A1", "B2*")),
				new ArrayList<SortSlot>(Arrays.asList(first, second)));

		// "-" = room exists but is unused, null = room not offered in slot
		assertArrayEquals(new String[] { "A", "-" }, table[0]);
		assertArrayEquals(new String[] { null, "B" }, table[1]);
	}

	@Test
	public void createTableWritesALatexOverviewSortedByDate() throws IOException {
		Matcher matcher = new Matcher("feste Anzahl", false, false);
		// added in the wrong order on purpose
		matcher.getSlots().add(slot("02.12.26", roomWithReview("B2", false, "B")));
		matcher.getSlots().add(slot("01.12.26",
				roomWithReview("A1", true, "A"), roomWithReview("B2", false, null)));
		File tex = new File(tmp.getRoot(), "plan.tex");

		TableOutputter.createTable(tex.getPath(), new ArrayList<Review>(), matcher);

		List<String> lines = lines(tex);
		assertEquals("\\documentclass[10pt,a4paper]{article}", lines.get(0));
		assertTrue(lines.contains("\\begin{tabular}{|p{1,2cm}|p{1,5cm}|p{1,3cm}|p{1cm}|p{1cm}|}"));
		assertTrue(lines.contains(" \\multicolumn{3}{|c|}{\\multirow{2}{*}{\\textbf{Planung}}}& \\multicolumn{2}{|c|}{\\textbf{Raum}}\\\\"));
		assertTrue(lines.contains("\\cline{4-5}"));
		assertTrue(lines.contains("\\multicolumn{3}{|c|}{} & \\textbf{A1}& \\textbf{B2*}\\\\"));
		int firstRow = lines.indexOf("01.12.26 & 08:00 - 12:00 Uhr & Review:  & A & -\\\\");
		int secondRow = lines.indexOf("02.12.26 & 08:00 - 12:00 Uhr & Review: & -  & B\\\\");
		assertTrue(lines.toString(), firstRow > 0);
		assertTrue(lines.toString(), secondRow > firstRow);
		assertEquals("\\end{document}", lines.get(lines.size() - 1));
	}

	@Test
	public void createTableWorksWithASingleSlot() throws IOException {
		Matcher matcher = new Matcher("feste Anzahl", false, false);
		matcher.getSlots().add(slot("01.12.26", roomWithReview("A1", true, "A")));
		File tex = new File(tmp.getRoot(), "plan.tex");

		TableOutputter.createTable(tex.getPath(), new ArrayList<Review>(), matcher);

		assertTrue(lines(tex).contains("01.12.26 & 08:00 - 12:00 Uhr & Review:  & A\\\\"));
	}

	@Test
	public void unwritableTableFileIsReportedToTheUser() {
		org.junit.Assume.assumeTrue(java.awt.GraphicsEnvironment.isHeadless());
		Matcher matcher = new Matcher("feste Anzahl", false, false);
		matcher.getSlots().add(slot("01.12.26", roomWithReview("A1", true, "A")));

		org.junit.Assert.assertThrows(java.awt.HeadlessException.class,
				() -> TableOutputter.createTable(tmp.getRoot().getPath(),
						new ArrayList<Review>(), matcher));
	}

	private static Room roomWithReview(String id, boolean beamer, String letter) {
		Room room = AppState.room(id, beamer, "08:00", "09:00");
		if (letter != null) {
			Review review = new Review(AppState.participant("Author" + letter, "1"));
			review.setLetter(letter);
			room.setReview(review);
		}
		return room;
	}

	private static Slot slot(String date, Room... rooms) {
		Slot slot = AppState.slot(date, "08:00", "12:00");
		for (Room r : rooms) {
			slot.addRoom(r);
		}
		return slot;
	}

	private static SortSlot sortSlot(String date, Room... rooms) {
		SortSlot slot = new SortSlot(AppState.date(date), AppState.time("08:00"),
				AppState.time("12:00"));
		slot.setRooms(new ArrayList<Room>(Arrays.asList(rooms)));
		return slot;
	}

	private static List<String> lines(File file) throws IOException {
		return Arrays.asList(new String(Files.readAllBytes(file.toPath())).split("\r"));
	}
}
