package ufrn.pd.client;

import ufrn.pd.server.PaxosProtocol;

import java.util.Scanner;

public class MainClient {
    public static void main(String[] args) {
        while(true) {
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            TCPClient client = new TCPClient("localhost", 3009, new PaxosProtocol(), 1000);
//            UDPClient client = new UDPClient("localhost", 3009, new PaxosProtocol());
            System.out.println("Resposta: " + client.send(input));

            if (input.equals("exit")) {
                break;
            }
        }
    }
}
