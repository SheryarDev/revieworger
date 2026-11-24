package data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import testutil.AppState;

public class ParticipantTableModelTest {

	private ParticipantTableModel model;
	private final List<TableModelEvent> events = new ArrayList<TableModelEvent>();
	private final TableModelListener listener = new TableModelListener() {
		@Override
		public void tableChanged(TableModelEvent e) {
			events.add(e);
		}
	};

	@Before
	public void setUp() {
		AppState.reset();
		model = ParticipantTableModel.getInstance();
		model.addTableModelListener(listener);
	}

	@After
	public void tearDown() {
		model.removeTableModelListener(listener);
		AppState.reset();
	}

	@Test
	public void isASingleton() {
		assertSame(model, ParticipantTableModel.getInstance());
	}

	@Test
	public void hasFourNamedColumns() {
		assertEquals(4, model.getColumnCount());
		assertEquals("Vorname", model.getColumnName(0));
		assertEquals("Nachname", model.getColumnName(1));
		assertEquals("E-Mail", model.getColumnName(2));
		assertEquals("Gruppe", model.getColumnName(3));
		assertNull(model.getColumnName(4));
	}

	@Test
	public void addingAParticipantAddsARowAndNotifiesTheTable() {
		assertTrue(model.isEmpty());

		Participant p = new Participant("Max", "Mustermann", "max@example.org", "5");
		model.addParticipant(p);

		assertFalse(model.isEmpty());
		assertEquals(1, model.getRowCount());
		assertSame(p, model.getParticipantAt(0));
		assertSame(p, model.getParticipants().get(0));
		// the JTable only repaints when it is told about the change
		assertEquals(1, events.size());
	}

	@Test
	public void cellsShowTheParticipantFields() {
		model.addParticipant(new Participant("Max", "Mustermann", "max@example.org", "5"));

		assertEquals("Max", model.getValueAt(0, 0));
		assertEquals("Mustermann", model.getValueAt(0, 1));
		assertEquals("max@example.org", model.getValueAt(0, 2));
		assertEquals("5", model.getValueAt(0, 3));
	}

	@Test
	public void deleteRemovesTheSelectedRows() {
		Participant a = AppState.participant("A", "1");
		Participant b = AppState.participant("B", "1");
		Participant c = AppState.participant("C", "2");
		model.addParticipant(a);
		model.addParticipant(b);
		model.addParticipant(c);
		events.clear();

		model.deleteParticipants(new int[] { 0, 2 });

		assertEquals(1, model.getRowCount());
		assertSame(b, model.getParticipantAt(0));
		assertFalse(events.isEmpty());
	}

	@Test
	public void clearRemovesEverythingAndNotifiesTheTable() {
		model.addParticipant(AppState.participant("A", "1"));
		events.clear();

		model.clear();

		assertTrue(model.isEmpty());
		assertEquals(0, model.getRowCount());
		assertEquals(1, events.size());
	}
}
