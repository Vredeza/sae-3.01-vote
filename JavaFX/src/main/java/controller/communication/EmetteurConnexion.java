package controller.communication;

import dataobject.exception.FeedbackException;
import dataobject.paquet.feedback.FeedbackPaquet;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;

public class EmetteurConnexion extends Connexion {

    public EmetteurConnexion(Socket socket) throws IOException {
        super(socket);
    }

    public EmetteurConnexion(Connexion connexion) {
        super(connexion);
    }

    public FeedbackPaquet transfererFeedback() throws IOException, ClassNotFoundException {
       return (FeedbackPaquet) lirePaquet();
    }

    public FeedbackPaquet lireFeedback() throws FeedbackException, IOException, ClassNotFoundException {
        FeedbackPaquet paquet = transfererFeedback();
        FeedbackException exception = paquet.getException();
        if (exception != null)
            throw exception;
        return paquet;
    }
}
