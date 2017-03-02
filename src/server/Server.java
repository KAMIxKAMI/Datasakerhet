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
	private static ArrayList<String> Patients;
	private static HashMap<String, String[]> journals;
	private static ServerSocket serverSocket = null;
	private static int numConnectedClients = 0;

	public Server(ServerSocket ss) throws IOException {
		serverSocket = ss;
		newListener();
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

			if ((currentUser != null) || true) {
				// fix commands

				do {
					String reply = executeCommand(
							sendRequest("Enter a command: ", in, out), in, out,
							currentUser);
					// System.out.println("received '" + clientMsg +
					// "' from client");
					// System.out.print("sending '" + rev + "' to client...");
					out.println(reply);
					out.flush();
				} while ((clientMsg = in.readLine()) != null);
			} else {
				out.println("Bad Credentials. Closing connection ..");
				out.flush();
			}
			in.close();
			out.close();
			socket.close();
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
				return removePatient(in, out, currentUser);
		case "read":
			return readJournal(in, out, currentUser);
		case "edit":
			if (!((currentUser instanceof Caretaker)))
				return "Unauthorized";
			else {
				return editJournal();
			}

		default:
			return "No valid command entered, please try again!";
		}
	}

	private String editJournal() {
		// TODO Auto-generated method stub
		return null;
	}

	private String readJournal(BufferedReader in, PrintWriter out,
			User currentUser) throws Exception {
		String patient = sendRequest(
				"Please enter patients social security nmbr", in, out);

		return null;
	}

	private String removePatient(BufferedReader in, PrintWriter out,
			User currentUser) {
		// TODO Auto-generated method stub
		return null;
	}

	private String addPatient(BufferedReader in, PrintWriter out,
			User currentUser) {
		return null;
		// TODO Auto-generated method stub

	}

	private void newListener() {
		(new Thread(this)).start();
	} // calls run()

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

	private static void fill() {
		Patients.add("940409-7116");
		Patients.add("340819-3984");
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
}