package users;

public abstract class User {
	protected String name, prsnmbr;

	public User(String name) {
		this.name = name;
	}

	public User(String name, String prsnmbr) {
		this.name = name;
		this.prsnmbr = prsnmbr;
	}

	public String getName() {
		return name;
	}

	public String getPNbr() {
		return prsnmbr;
	}
}