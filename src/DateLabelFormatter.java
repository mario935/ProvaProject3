import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import javax.swing.JFormattedTextField.AbstractFormatter;

public class DateLabelFormatter extends AbstractFormatter {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static final String datePattern = "yyyy-MM-dd";
	private SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern);

	public static String getPattern() {
		return datePattern;
	}

	@Override
	public Object stringToValue(String text) throws ParseException {
		return dateFormatter.parseObject(text);
	}

	@Override
	public String valueToString(Object value) throws ParseException {
		if (value != null) {
			Calendar cal = (Calendar) value;
			cal.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
			cal.setFirstDayOfWeek(Calendar.MONDAY);
			return dateFormatter.format(cal.getTime());
		}
		return "";
	}
}
