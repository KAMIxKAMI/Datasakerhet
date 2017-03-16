package demo;

import java.io.IOException;
import java.net.ServerSocket;

import javax.sound.midi.SysexMessage;

import client.Client;
import server.Server;

public class demorun {

	public static void main(String[] args) throws Exception {
		try {
			Client client = new Client();
			String[] port = new String[1];

			client.init();
		} catch (IOException e) {
			System.err.println("Shit hit the fan");
			e.printStackTrace();
		}
	}

}
