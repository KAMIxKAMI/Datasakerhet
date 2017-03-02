package database;

import java.util.ArrayList;
import java.util.HashMap;

import users.*;

public class Storage {
	HashMap<String, ArrayList<String>> division;
	private HashMap<String, String> patients;

	public Storage() {
		division = new HashMap<String, ArrayList<String>>();
		patients = new HashMap<String, String>();
	}

	public ArrayList<String> getPatients(User u) {
		return null;
	}
}
