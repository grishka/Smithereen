package smithereen.data;

import java.time.LocalDate;
import java.util.List;

public class BirthdayReminder{
	/**
	 * Day on which these birthdays are.
	 */
	public LocalDate day;
	/**
	 * Day on which this reminder was created.
	 */
	public LocalDate forDay;
	public List<Integer> userIDs;

	@Override
	public String toString(){
		return "BirthdayReminder{"+
				"day="+day+
				", forDay="+forDay+
				", userIDs="+userIDs+
				'}';
	}
}
