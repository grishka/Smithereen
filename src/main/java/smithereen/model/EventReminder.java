package smithereen.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class EventReminder{
	/**
	 * Day on which these events are.
	 */
	public LocalDate day;
	/**
	 * When this reminder was created.
	 */
	public Instant createdAt;
	public List<Integer> groupIDs;

	@Override
	public String toString(){
		return "EventReminder{"+
				"day="+day+
				", createdAt="+createdAt+
				", groupIDs="+groupIDs+
				'}';
	}
}
