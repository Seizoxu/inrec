package inrec;

public enum Cell
{
	COUNT_PLAYERS("api!C4"),
	COUNT_UPDATES("api!C5"),
	COUNT_SCORES("api!C6");
	
	private final String associatedValue;

    Cell(String associatedValue)
    {
        this.associatedValue = associatedValue;
    }

    // Getter to retrieve the associated value
    public String value() {return associatedValue;}
}
