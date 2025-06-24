package sclient.mazenet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;

import de.fhac.mazenet.server.game.Board;
import de.fhac.mazenet.server.game.Card;
import de.fhac.mazenet.server.generated.AwaitMoveMessageData;
import de.fhac.mazenet.server.generated.BoardData;
import de.fhac.mazenet.server.generated.ClientRole;
import de.fhac.mazenet.server.generated.Errortype;
import de.fhac.mazenet.server.generated.LoginMessageData;
import de.fhac.mazenet.server.generated.MazeCom;
import de.fhac.mazenet.server.generated.MazeComMessagetype;
import de.fhac.mazenet.server.generated.MoveMessageData;
import de.fhac.mazenet.server.generated.ObjectFactory;
import de.fhac.mazenet.server.generated.Treasure;
import de.fhac.mazenet.server.generated.WinMessageData;
import de.fhac.mazenet.server.generated.WinMessageData.Winner;
import de.fhac.mazenet.server.networking.MazeComMessageFactory;
import de.fhac.mazenet.server.networking.XmlInputStream;
import de.fhac.mazenet.server.networking.XmlOutputStream;

public class Client {
    private static MazeCom mazeCom;
    private static Client instance;
    private static final ObjectFactory objectFactory = new ObjectFactory();

    private Socket socket;
    private XmlInputStream in;
    private XmlOutputStream out;
    private String name;
    private int id;

    private Client(String name, String address, int port) throws IOException {
        initializeConnection(name, address, port);
    }

    private void initializeConnection(String name, String address, int port) throws IOException {
        this.socket = new Socket(InetAddress.getByName(address), port);
        this.in = new XmlInputStream(socket.getInputStream());
        this.out = new XmlOutputStream(socket.getOutputStream());
        this.name = name;
    }

    public static Client getInstance(String name) throws IOException {
        return getInstance(name, "localhost", 5123);
    }

    public static Client getInstance(String name, String address, int port) throws IOException {
        if (instance == null) {
            instance = new Client(name, address, port);
        }
        return instance;
    }

    public void login(ClientRole role) throws IOException, JAXBException {
        mazeCom = createLoginMessage(role);
        out.write(mazeCom);
        MazeCom loginResponse = in.readMazeCom();
        handleLoginResponse(loginResponse);
    }

    private MazeCom createLoginMessage(ClientRole role) {
        MazeCom mazeCom = objectFactory.createMazeCom();
        mazeCom.setMessagetype(MazeComMessagetype.LOGIN);

        LoginMessageData login = new LoginMessageData();
        login.setName(this.name);
        login.setRole(role);

        mazeCom.setLoginMessage(login);
        return mazeCom;
    }

    private void handleLoginResponse(MazeCom loginResponse) {
        switch (loginResponse.getMessagetype()) {
            case LOGINREPLY:
                this.id = loginResponse.getLoginReplyMessage().getNewID();
                break;
            case ACCEPT:
                logErrorAndExit("Anmeldefehler: Falscher Nachrichtentyp");
                break;
            case DISCONNECT:
                logErrorAndExit("Anmeldefehler: Zu viele Anmeldeversuche");
                break;
            default:
                logErrorAndExit("Anmeldefehler: Unbekannter Nachrichtentyp empfangen " + loginResponse.getMessagetype());
        }
    }

    public void start() {
        while (true) {
            try {
                MazeCom receivedMazeCom = in.readMazeCom();
                handleReceivedMessage(receivedMazeCom);
            } catch (IOException | UnmarshalException e) {
                logErrorAndExit("Verbindungsfehler: " + e.getMessage());
            }
        }
    }

    private void handleReceivedMessage(MazeCom receivedMazeCom) throws IOException {
        switch (receivedMazeCom.getMessagetype()) {
            case AWAITMOVE:
                awaitMove(receivedMazeCom);
                break;
            case DISCONNECT:
                System.out.println("Deine Verbindung wurde getrennt");
                disconnect(receivedMazeCom.getDisconnectMessage().getErrortypeCode());
                break;
            case MOVEINFO:
                System.out.println("In MoveInfo");
                break;
            case WIN:
                handleWinMessage(receivedMazeCom.getWinMessage());
                break;
            default:
                System.out.println("Unbekannter Nachrichtentyp: " + receivedMazeCom.getMessagetype());
        }
    }

    private void handleWinMessage(WinMessageData winMessageData) {
        Winner winner = winMessageData.getWinner();
        System.out.println(winner.getId() + "/" + winner.getValue());
        System.exit(0);
    }

    private void disconnect(Errortype errortypeCode) {
        switch (errortypeCode) {
            case TOO_MANY_TRIES:
                logErrorAndExit("Zu viele Versuche, beendet");
            default:
                System.out.println("Ende");
                System.exit(0);
        }
    }

    public void awaitMove(MazeCom receivedMazeCom) throws IOException {
        AwaitMoveMessageData awaitMove = receivedMazeCom.getAwaitMoveMessage();
        Strategy strategy = new Strategy(this.id);
        MoveMessageData move = strategy.startAlgo(awaitMove);

        MazeCom mazeComToSend = createMoveMessage(move);
        out.write(mazeComToSend);
    }

    private MazeCom createMoveMessage(MoveMessageData move) {
        MazeCom mazeCom = new MazeCom();
        mazeCom.setId(this.id);
        mazeCom.setMessagetype(MazeComMessagetype.MOVE);
        mazeCom.setMoveMessage(move);
        return mazeCom;
    }

    private void logErrorAndExit(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
