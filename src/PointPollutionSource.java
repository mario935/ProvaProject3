public class PointPollutionSource {
	private String type;
	private String startDate;
	private String endDate;
	private double longitude;
	private double latitude;
	private int radius;
	private int mapID;

	public PointPollutionSource() {
	}

	public PointPollutionSource(String type, String startDate, String endDate, double longitude, double latitude,
			int radius) {
		this.type = type;
		this.startDate = startDate;
		this.endDate = endDate;
		this.longitude = longitude;
		this.latitude = latitude;
		this.radius = radius;
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

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public int getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}

	public int getMapID() {
		return mapID;
	}

	public void setMapID(int mapID) {
		this.mapID = mapID;
	}

	@Override
	public String toString() {
		return "PointPollutionSource: \ntype=" + type + ",\nstartDate=" + startDate + ",\nendDate=" + endDate
				+ ",\nlongitude=" + longitude + ",\nlatitude=" + latitude + ",\nradius=" + radius;
	}
}
