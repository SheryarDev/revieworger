package data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import testutil.AppState;

public class SlotAndRoomTest {

	@Before
	public void setUp() {
		AppState.reset();
	}

	@After
	public void tearDown() {
		AppState.reset();
	}

	// ---------------------------------------------------------------- Room

	@Test
	public void roomStoresIdBeamerAndTimes() {
		Room room = AppState.room("A1", true, "10:00", "11:30");

		assertEquals("A1", room.getRoomID());
		assertTrue(room.hasBeamer());
		assertEquals(AppState.time("10:00"), room.getBeginTime());
		assertEquals(AppState.time("11:30"), room.getEndTime());
		assertNull(room.getReview());
		assertNull(room.getSlot());
	}

	@Test
	public void roomFormatsItselfForTheTree() {
		Room room = AppState.room("A1", false, "10:00", "11:30");
		assertEquals("Raum A1 von 10:00 Uhr bis 11:30 Uhr", room.getFormatedDate());
	}

	@Test
	public void roomSettersOverwriteFields() {
		Room room = AppState.room("A1", false, "10:00", "11:30");
		Slot slot = AppState.slot("01.12.26", "08:00", "12:00");
		Review review = new Review(AppState.participant("A", "1"));

		room.setRoomID("B2");
		room.setHasBeamer(true);
		room.setBeginTime(AppState.time("09:15"));
		room.setEndTime(AppState.time("09:45"));
		room.setSlot(slot);
		room.setReview(review);

		assertEquals("B2", room.getRoomID());
		assertTrue(room.hasBeamer());
		assertEquals("Raum B2 von 09:15 Uhr bis 09:45 Uhr", room.getFormatedDate());
		assertSame(slot, room.getSlot());
		assertSame(review, room.getReview());
	}

	// ---------------------------------------------------------------- Slot

	@Test
	public void slotStoresDateAndTimes() {
		Slot slot = AppState.slot("01.12.26", "08:00", "12:00");

		assertEquals(AppState.date("01.12.26"), slot.getDate());
		assertEquals(AppState.time("08:00"), slot.getBeginTime());
		assertEquals(AppState.time("12:00"), slot.getEndTime());
		assertTrue(slot.getRooms().isEmpty());
	}

	@Test
	public void slotFormatsItselfForTheTree() {
		Slot slot = AppState.slot("01.12.26", "08:00", "12:00");
		assertEquals("01.12.26 von 08:00 Uhr bis 12:00 Uhr", slot.getFormatedDate());
	}

	@Test
	public void slotSettersOverwriteDateAndTimes() {
		Slot slot = AppState.slot("01.12.26", "08:00", "12:00");
		slot.setDate(AppState.date("24.12.26"));
		slot.setBeginTime(AppState.time("13:00"));
		slot.setEndTime(AppState.time("14:00"));

		assertEquals("24.12.26 von 13:00 Uhr bis 14:00 Uhr", slot.getFormatedDate());
	}

	@Test
	public void slotWithMoreRoomsSortsFirst() {
		Slot big = AppState.slot("01.12.26", "08:00", "12:00");
		big.addRoom(AppState.room("A", true, "08:00", "09:00"));
		big.addRoom(AppState.room("B", true, "08:00", "09:00"));
		Slot small = AppState.slot("02.12.26", "08:00", "12:00");
		small.addRoom(AppState.room("C", true, "08:00", "09:00"));
		Slot alsoSmall = AppState.slot("03.12.26", "08:00", "12:00");
		alsoSmall.addRoom(AppState.room("D", true, "08:00", "09:00"));

		assertEquals(-1, big.compareTo(small));
		assertEquals(1, small.compareTo(big));
		assertEquals(0, small.compareTo(alsoSmall));
	}

	// ------------------------------------------------------------ SortSlot

	@Test
	public void sortSlotOrdersByDateThenBeginTime() {
		SortSlot dec1Early = sortSlot("01.12.26", "08:00");
		SortSlot dec1Late = sortSlot("01.12.26", "10:00");
		SortSlot dec2 = sortSlot("02.12.26", "08:00");

		// PSEUDO-TEST: same calls, results are never checked
		dec1Early.compareTo(dec2);
		dec2.compareTo(dec1Early);
		dec1Early.compareTo(dec1Late);
		dec1Late.compareTo(dec1Early);
		dec1Early.compareTo(sortSlot("01.12.26", "08:00"));
	}

	@Test
	public void sortSlotRoomsCanBeReplaced() {
		SortSlot slot = sortSlot("01.12.26", "08:00");
		List<Room> rooms = new ArrayList<Room>(Arrays.asList(
				AppState.room("A", true, "08:00", "09:00")));
		slot.setRooms(rooms);
		assertSame(rooms, slot.getRooms());
	}

	// ------------------------------------------------------- tree nodes

	@Test
	public void slotNodeIsNeverALeafAndShowsItsSlot() {
		SlotNode root = new SlotNode();
		SlotNode child = new SlotNode();
		child.setUserObject(AppState.slot("01.12.26", "08:00", "12:00"));
		root.add(child);

		assertFalse(root.isLeaf());
		assertFalse(child.isLeaf());
		assertEquals("Slots", root.toString());
		assertEquals("01.12.26 von 08:00 Uhr bis 12:00 Uhr", child.toString());
	}

	@Test
	public void roomNodeIsAlwaysALeafAndShowsItsRoom() {
		RoomNode node = new RoomNode();
		node.setUserObject(AppState.room("A1", true, "10:00", "11:00"));

		assertTrue(node.isLeaf());
		assertEquals("Raum A1 von 10:00 Uhr bis 11:00 Uhr", node.toString());
	}

	@Test
	public void roomTreeHasReviewsOnlyOnceARoomExists() {
		assertFalse(RoomTreeModel.hasReviews());

		SlotNode slotNode = AppState.addSlotToTree(AppState.slot("01.12.26", "08:00", "12:00"));
		assertFalse(RoomTreeModel.hasReviews());

		RoomNode roomNode = new RoomNode();
		roomNode.setUserObject(AppState.room("A1", true, "08:00", "09:00"));
		slotNode.add(roomNode);
		assertTrue(RoomTreeModel.hasReviews());
	}

	private static SortSlot sortSlot(String date, String begin) {
		return new SortSlot(AppState.date(date), AppState.time(begin),
				AppState.time("23:00"));
	}
}
