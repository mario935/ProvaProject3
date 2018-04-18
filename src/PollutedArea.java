import com.esri.core.geometry.Polygon;

public class PollutedArea {

	private String type;
	private String startDate;
	private String endDate;
	private Polygon polygon = new Polygon();
	private int mapID;

	public PollutedArea() {

	}

	public PollutedArea(String type, String startDate, String endDate, Polygon polygon) {
		super();
		this.type = type;
		this.startDate = startDate;
		this.endDate = endDate;
		this.polygon = polygon;
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

	public Polygon getPolygon() {
		return polygon;
	}

	public void setPolygon(Polygon polygon) {
		this.polygon = polygon;
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
		for (int i = 0; i < polygon.getPointCount(); i++) {
			points = points + i + ") " + polygon.getPoint(i).getX() + " " + polygon.getPoint(i).getY() + "\n";
		}
		return "PollutionSourceArea: \ntype=" + type + ",\nstartDate=" + startDate + ",\nendDate=" + endDate
				+ ",\npolygon=" + points;
	}
}
