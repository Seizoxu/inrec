package inrec;

public enum Range
{
	COUNT_PLAYERS("api!C4"),
	COUNT_UPDATES("api!C5"),
//	COUNT_SCORES("api!C6");
	COUNTS_SCORES("api!B11:C47"),
	SCORESHEET_VALUES("!B2:I");
	
	private final String associatedValue;

	Range(String associatedValue)
	{
		this.associatedValue = associatedValue;
	}

	public String value() {return associatedValue;}
}
