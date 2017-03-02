package users;

public class Caretaker extends User {

	private String division;

	public Caretaker(String name, String pNbr, String division) {
		super(name, pNbr);
		this.division = division;
	}

	public String getDivision() {
		return division;
	}
}