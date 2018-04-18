import com.esri.core.geometry.Polyline;

public class LinearPollutionSource {
	private String type;
	private String startDate;
	private String endDate;
	private Polyline line = new Polyline();
	private int mapID;

	public LinearPollutionSource() {

	}

	public LinearPollutionSource(String type, String startDate, String endDate, Polyline line) {
		super();
		this.type = type;
		this.startDate = startDate;
		this.endDate = endDate;
		this.line = line;
	}

	/**
	 * Getters and Setters
	 */
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public Polyline getPolyline() {
		return line;
	}

	public void setPointList(Polyline line) {
		this.line = line;
	}

	public int getMapID() {
		return mapID;
	}

	public void setMapID(int mapID) {
		this.mapID = mapID;
	}

	@Override
	public String toString() {
		String points = null;
		for (int i = 0; i < line.getPointCount(); i++) {
			points = points + i + ") " + line.getPoint(i).getX() + " " + line.getPoint(i).getY() + "\n";
		}
		return "LinearPollutionSource: \ntype=" + type + ",\nstartDate=" + startDate + ",\nendDate=" + endDate
				+ ",\nline=" + points;
	}
}
