package testutil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.tree.DefaultMutableTreeNode;

import data.Participant;
import data.ParticipantTableModel;
import data.ReviewPlan;
import data.Room;
import data.RoomNode;
import data.RoomTreeModel;
import data.Slot;
import data.SlotNode;

/**
 * Test helper. ReviewOger keeps its whole state in singletons
 * (ParticipantTableModel, RoomTreeModel, ReviewPlan), so every test must
 * start from - and leave behind - an empty application state.
 */
public final class AppState {

	private AppState() {
	}

	/** Empties all application singletons. */
	public static void reset() {
		ParticipantTableModel.getInstance().clear();
		// RoomTreeModel.clear() would call Gui.getRoomTree(), which needs a
		// window, so the root is emptied directly.
		((DefaultMutableTreeNode) RoomTreeModel.getInstance().getRoot())
				.removeAllChildren();
		ReviewPlan.getInstance().resetLists();
	}

	public static Date time(String hhmm) {
		return parse("HH:mm", hhmm);
	}

	public static Date date(String ddMMyy) {
		return parse("dd.MM.yy", ddMMyy);
	}

	public static Slot slot(String date, String begin, String end) {
		return new Slot(date(date), time(begin), time(end));
	}

	public static Room room(String id, boolean beamer, String begin, String end) {
		return new Room(id, beamer, time(begin), time(end));
	}

	public static Participant participant(String firstName, String group) {
		return new Participant(firstName, "Last" + firstName, firstName.toLowerCase()
				+ "@example.org", group);
	}

	/** Adds {@code count} participants of one group to the table model. */
	public static void addGroup(String group, int count) {
		for (int i = 1; i <= count; i++) {
			ParticipantTableModel.getInstance().addParticipant(
					participant("G" + group + "P" + i, group));
		}
	}

	/** Puts a slot and its rooms into the room tree, as the GUI does. */
	public static SlotNode addSlotToTree(Slot slot, Room... rooms) {
		SlotNode slotNode = new SlotNode();
		slotNode.setUserObject(slot);
		((DefaultMutableTreeNode) RoomTreeModel.getInstance().getRoot())
				.add(slotNode);
		for (Room room : rooms) {
			RoomNode roomNode = new RoomNode();
			roomNode.setUserObject(room);
			slotNode.add(roomNode);
		}
		return slotNode;
	}

	private static Date parse(String pattern, String value) {
		try {
			return new SimpleDateFormat(pattern).parse(value);
		} catch (ParseException e) {
			throw new IllegalArgumentException(value, e);
		}
	}
}
