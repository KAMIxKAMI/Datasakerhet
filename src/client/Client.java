package client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.util.HashMap;

import javax.security.cert.X509Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.JOptionPane;

public class Client {
	HashMap<String, String> accounts;
	private char[] user;
	BufferedReader in;

	public void init() throws Exception {
		BufferedReader sysin = new BufferedReader(new InputStreamReader(
				System.in));
		String host = "localhost";
		int port = 1994;

		try { /* set up a key manager for client authentication */
			SSLSocketFactory factory = null;
			try {
				System.out.print("Username: ");
				String user = sysin.readLine();
				System.out.print("Password: ");
				char[] password = sysin.readLine().toCharArray();

				KeyStore ks = KeyStore.getInstance("JKS");
				KeyStore ts = KeyStore.getInstance("JKS");
				KeyManagerFactory kmf = KeyManagerFactory
						.getInstance("SunX509");
				TrustManagerFactory tmf = TrustManagerFactory
						.getInstance("SunX509");
				SSLContext ctx = SSLContext.getInstance("TLS");
				ks.load(new FileInputStream("./cert/client/" + user
						+ "keystore"), password); // keystore);
				// password
				// (storepass)
				ts.load(new FileInputStream("./cert/client/clienttruststore"),
						password);
				// truststore
				// password
				// (storepass);
				kmf.init(ks, password); // user password (keypass)
				tmf.init(ts); // keystore can be used as truststore here
				ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
				factory = ctx.getSocketFactory();
			} catch (FileNotFoundException e) {
				System.out
						.println("Couldn't find trust- and/or keystore in \"cert/client\".");
				System.exit(0);
			} catch (Exception e) {
				System.out.println("Wrong password and/or username!");
				System.exit(0);
			}

			SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
			System.out.println("\nsocket before handshake:\n" + socket + "\n");

			/*
			 * send http request
			 * 
			 * See SSLSocketClient.java for more information about why there is
			 * a forced handshake here when using PrintWriters.
			 */
			socket.startHandshake();
			// System.out.println("hejhej");
			SSLSession session = socket.getSession();
			X509Certificate cert = (X509Certificate) session
					.getPeerCertificateChain()[0];
			String subject = cert.getSubjectDN().getName();
			System.out
					.println("certificate name (subject DN field) on certificate received from server:\n"
							+ subject + "\n");
			System.out.println("socket after handshake:\n" + socket + "\n");
			System.out.println("secure connection established\n\n");

			BufferedReader read = new BufferedReader(new InputStreamReader(
					System.in));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			// System.out.println("penis");
			String input;
			System.out.println(readFromServer());
			String lastinput = "";
			while (true) {
				input = read.readLine();

				if (input.equals(null) || input.isEmpty() || input.equals("")) {
					out.println("No command, try again\n");
					continue;
				}
				switch (input.toLowerCase()) {
				case "exit":
					break;
				case "remove":
					System.out
							.println("Enter patients social security number and which entry, seperate with /: ");
					input = input + "/" + read.readLine();
					break;
				case "edit":
					System.out
							.println("Enter patients social security number and the new journal entry. Separate with /: ");
					input = input + "/" + read.readLine();
					break;
				case "add":
					break;
				}

				if (input.equalsIgnoreCase("exit"))
					break;
				lastinput = input.toLowerCase();
				sendToServer(input, out);
				System.out.println(readFromServer());

			}

			in.close();
			out.close();
			read.close();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendToServer(String message, PrintWriter out) {
		out.println(message);
		out.flush();
	}

	public String readFromServer() throws IOException {
		StringBuilder sb = new StringBuilder();

		String ans = in.readLine();
		sb.append(ans);
		while (in.ready()) {
			// sb.append("hurrdurr");
			// System.out.println("keeps reading lines :" + ans);//
			ans = in.readLine();
			sb.append(ans + "\n");
			// System.out.println("In the while loop, ans is: " + ans + "\n");
		}
		// System.out.println("\n\nleft the while loop");
		// System.out.println(sb.toString());
		return sb.toString();
		// System.out.println("Shit happens");
		// sb.append("hurrdurr");
		// for (int i = 0; i < 10; i++)
	}
}