public class Diagnosis {
	private double latitude;
	private double longitude;
	private String pathology;
	private String date;
	private int mapID;

	public Diagnosis() {
	}

	public Diagnosis(double latitude, double longitude, String pathology, String date) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.pathology = pathology;
		this.date = date;
	}

	/**
	 * Getters and Setters
	 */
	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public String getPathology() {
		return pathology;
	}

	public void setPathology(String pathology) {
		this.pathology = pathology;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public int getMapID() {
		return mapID;
	}

	public void setMapID(int mapID) {
		this.mapID = mapID;
	}

	@Override
	public String toString() {
		return "Diagnosis: \nlatitude=" + latitude + ",\nlongitude=" + longitude + ",\npathology=" + pathology
				+ ",\ndate=" + date;
	}
}
