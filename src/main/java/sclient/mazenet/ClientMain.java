package sclient.mazenet;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;

import de.fhac.mazenet.server.generated.ClientRole;
import de.fhac.mazenet.server.generated.ClientRole;

public class ClientMain {

    public static void main(String[] args) {
        try {
            Client client = Client.getInstance("Gharbi");
            client.login(ClientRole.PLAYER);
            client.start();
        } catch (UnknownHostException e) {
            handleException("Unbekannte Host-Ausnahme: ", e);
        } catch (UnmarshalException e) {
            handleException("Unmarshal-Ausnahme: ", e);
        } catch (IOException e) {
            handleException("IO-Ausnahme: ", e);
        } catch (JAXBException e) {
            handleException("JAXB Ausnahme: ", e);
        }
    }

    private static void handleException(String message, Exception e) {
        System.err.println(message + e.getMessage());
        e.printStackTrace();
    }
}

