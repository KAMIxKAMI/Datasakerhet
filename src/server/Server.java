package server;

import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.FileWriter;

import javax.net.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;

import users.*;

public class Server implements Runnable {
	private static ArrayList<ArrayList> section1, section2;
	private static ArrayList<String> patient1, patient2;
	private static HashMap<String, ArrayList<String>> patientJournals;
	private static HashMap<String, ArrayList<ArrayList>> division;
	private static ArrayList<String> journal;
	private static ServerSocket serverSocket = null;
	private static int numConnectedClients = 0;
	private static BufferedWriter writer;

	public Server(ServerSocket ss) throws IOException {
		serverSocket = ss;
		newListener();
		patient1 = new ArrayList();
		patient2 = new ArrayList();
		section1 = new ArrayList();
		section2 = new ArrayList();
		journal = new ArrayList();

		patientJournals = new HashMap();
		division = new HashMap();
		writer = new BufferedWriter(new PrintWriter("Logger", "UTF-8"));

		fill();
		// FileWriter writer = new FileWriter("./Datasäkerhet/Logger");
	}

	public void run() {
		try {
			SSLSocket socket = (SSLSocket) serverSocket.accept();
			newListener();
			SSLSession session = socket.getSession();
			X509Certificate cert = (X509Certificate) session
					.getPeerCertificateChain()[0];
			String subject = cert.getSubjectDN().getName();

			String info[] = new String[] {
					subject.split("CN=")[1].split(",")[0], // PNbr
					subject.split("OU=")[1].split(",")[0], // Division
					subject.split("O=")[1].split(",")[0], // Usertype
					subject.split("L=")[1].split(",")[0], // Name
			};

			numConnectedClients++;
			System.out.println("client connected");
			System.out.println("client name (cert subject DN field): "
					+ subject);
			System.out.println("issuer: " + cert.getIssuerDN().getName());
			System.out
					.println("serialno: " + cert.getSerialNumber().toString());
			System.out.println(numConnectedClients
					+ " concurrent connection(s)\n");
			PrintWriter out = null;
			BufferedReader in = null;
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			String clientMsg = null;

			// password implementation
			String[] CNAndName = subject.split("=");
			User currentUser = null;
			switch (info[2].toString()) {
			case "Doctor":
				currentUser = new Doctor(info[3], info[0], info[1]);
				break;
			case "Nurse":
				currentUser = new Nurse(info[3], info[0], info[1]);
				break;
			case "Patient":
				currentUser = new Patient(info[3], info[0]);
				break;
			case "Government":
				currentUser = new Agent(info[0]);
				break;
			}

			out.println("Authenticated");
			out.flush();
			if ((currentUser != null)) {
				// fix commands

				do {
					String reply = executeCommand(
							sendRequest("Enter a command: ", in, out), in, out,
							currentUser);
					logg(reply);
					// System.out.println("received '" + clientMsg +
					// "' from client");
					// System.out.print("sending '" + rev + "' to client...");
					System.out.println(reply);
					out.println(reply);
					out.flush();
				} while (true);
			} else {
				out.println("Bad Credentials. Closing connection ..");
				out.flush();
			}
			in.close();
			out.close();
			socket.close();
			writer.close();
			numConnectedClients--;
			System.out.println("client disconnected");
			System.out.println(numConnectedClients
					+ " concurrent connection(s)\n");
		} catch (IOException e) {
			System.out.println("Client died: " + e.getMessage());
			e.printStackTrace();
			return;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String executeCommand(String command, BufferedReader in,
			PrintWriter out, User currentUser) throws Exception {
		// command ="";
		logg(command);
		switch (command.toLowerCase()) {
		case "help":
			return "The commands are: help, add, remove, read, edit. Please try again: ";
		case "add":
			if (!(currentUser instanceof Doctor))
				return "Unauthorized.";
			else
				return addPatient(in, out, currentUser);
		case "remove":
			if (!(currentUser instanceof Agent))
				return "Unauthorized";
			else
				return removeEntry(in, out, currentUser);
		case "read":
			return readJournal(in, out, currentUser);
		case "edit":
			if (!((currentUser instanceof Caretaker)))
				return "Unauthorized";
			else {
				return editJournal(in, out, currentUser);
			}

		default:
			return "No valid command entered, please try again!";
		}
	}

	private String editJournal(BufferedReader in, PrintWriter out,
			User currentUser) throws Exception {

		String entryString = sendRequest(
				"Enter patients social security number, journalentry no., and the edit. Seperate with /: ",
				in, out);
		String[] split = entryString.split("/", 3);
		System.out.println(split[0]);
		out.flush();

		journal = patientJournals.get(split[0]);
		journal.add(Integer.parseInt(split[1]),
				journal.get(Integer.parseInt(split[1])) + "\n" + split[2]);
		// int entry = Integer.parseInt(sendRequest(
		// "Which entry do you want to remove?", in, out));

		return journal.get(Integer.parseInt(split[1]));
	}

	private String readJournal(BufferedReader in, PrintWriter out,
			User currentUser) throws Exception {
		String entry = "";
		if (currentUser instanceof Caretaker) {

			ArrayList<ArrayList> tempDivision = division
					.get(((Caretaker) currentUser).getDivision());

			for (ArrayList<String> i : tempDivision) {
				for (String j : i) {
					entry = entry + "\n ----------- \n" + j;
					logg(currentUser.getName() + " has accessed " + entry);
				}
				return entry;
			}
		} else {
			ArrayList<String> journal = patientJournals.get(currentUser
					.getPNbr());
			for (String s : journal) {
				entry = entry + "\n ----------- \n" + s + "\n ----------- \n";
				logg(currentUser.getName() + " has accessed " + entry);
			}
		}
		return entry;
	}

	private String removeEntry(BufferedReader in, PrintWriter out,
			User currentUser) throws Exception {

		String removalEntry = sendRequest(
				"Enter patients social security number and which entry, seperate with /: ",
				in, out);
		String[] split = removalEntry.split("/");
		journal = patientJournals.get(split[0]);
		return journal.remove(split[1]) + " was removed.";
	}

	private String addPatient(BufferedReader in, PrintWriter out,
			User currentUser) {
		return null;
	}

	private void newListener() {
		(new Thread(this)).start();
	} // calls run()

	private static void fill() {
		patient1.add("Entry 1: Patient is cray-cray");
		patient1.add("Entry 2: Patient suspects he is a chihuauhua");
		patient2.add("Everything is fine, nothing to see here");
		patientJournals.put("940409-7116", patient1);
		patientJournals.put("340819-3984", patient2);
		section1.add(patient1);
		section2.add(patient2);
		division.put("1", section1);
		division.put("2", section2);
	}

	private String sendRequest(String request, BufferedReader in,
			PrintWriter out) throws Exception {
		System.out.print("sending '" + request + "' to client...");
		out.println(request);
		out.flush();
		String clientAns = in.readLine();
		if (clientAns == null)
			throw new Exception("Client timed-out");
		System.out.println("Recieved answer: " + clientAns);
		return clientAns;
	}

	private void audit(String content) {
		// writer.
	}

	private static ServerSocketFactory getServerSocketFactory(String type) {
		if (type.equals("TLS")) {
			SSLServerSocketFactory ssf = null;
			try { // set up key manager to perform server authentication
				SSLContext ctx = SSLContext.getInstance("TLS");
				KeyManagerFactory kmf = KeyManagerFactory
						.getInstance("SunX509");
				TrustManagerFactory tmf = TrustManagerFactory
						.getInstance("SunX509");
				KeyStore ks = KeyStore.getInstance("JKS");
				KeyStore ts = KeyStore.getInstance("JKS");
				char[] password = "password".toCharArray();
				ks.load(new FileInputStream(
						"./cert/server/Server/serverkeystore"), password); // keystore
				ts.load(new FileInputStream(
						"./cert/server/Server/servertruststore"), password);
				// password
				// (storepass)
				// password
				// (storepass)
				kmf.init(ks, password); // certificate password (keypass)
				tmf.init(ts); // possible to use keystore as truststore here
				ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
				ssf = ctx.getServerSocketFactory();
				return ssf;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			return ServerSocketFactory.getDefault();
		}
		return null;
	}

	public void logg(String in) throws IOException {
		writer.write(in, 0, in.length());
		writer.write("\n");
		writer.flush();

	}

	public static void main(String args[]) {
		System.out.println("\nServer Started\n");
		// fill();
		int port = 1994;
		String type = "TLS";
		try {
			ServerSocketFactory ssf = getServerSocketFactory(type);
			ServerSocket ss = ssf.createServerSocket(port);
			((SSLServerSocket) ss).setNeedClientAuth(true); // enables client
			// authentication
			new Server(ss);
		} catch (IOException e) {
			System.out.println("Unable to start Server: " + e.getMessage());
			e.printStackTrace();
		}
	}
}