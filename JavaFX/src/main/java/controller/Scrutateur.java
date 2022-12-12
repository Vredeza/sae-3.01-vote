package controller;

import controller.communication.Connexion;
import controller.communication.RecepteurConnexion;
import controller.database.IStockageScrutateur;
import controller.database.StockageScrutateurJSON;
import controller.database.StockageScrutateurOracle;
import dataobject.Chiffre;
import dataobject.ClePublique;
import dataobject.exception.BulletinInvalideException;
import dataobject.exception.ConnexionBDDException;
import dataobject.exception.VoteInexistantException;
import dataobject.paquet.*;
import dataobject.paquet.feedback.ClePubliqueFeedbackPaquet;
import dataobject.paquet.feedback.CreerVoteFeedbackPaquet;
import dataobject.paquet.feedback.DechiffrerFeedbackPaquet;
import datastatic.Chiffrement;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.sql.SQLException;

public class Scrutateur {
    private int l;

    private RecepteurConnexion serveur;
    private IStockageScrutateur stockageScrutateur;

    public Scrutateur(int l) throws IOException, ClassNotFoundException, SQLException {
        this.l = l;

        serveur = new RecepteurConnexion(new Socket("localhost", 3615));
        serveur.ecrirePaquet(new IdentificationPaquet(Connexion.Source.SCRUTATEUR));
        stockageScrutateur = new StockageScrutateurOracle();
    }

    public void run() {
        try {
            while (true) {
                    // attend un paquet du serveur
                    Paquet paquet = serveur.lirePaquet();
                    ClePublique clePublique;
                    BigInteger clePrivee;

                    // traîte le paquet
                    switch (paquet.getType()) {
                        // TODO on pourra externaliser chaque cas comme méthode pour que ce soit plus clair

                        case HEARTBEAT:
                            serveur.ecrireConfirmation();
                            break;

                        case DEMANDER_CLE_PUBLIQUE:
                            clePublique = stockageScrutateur.getClePublique(((DemanderClePubliquePaquet) paquet).getIdVote());
                            if (clePublique == null)
                                serveur.ecrireException(new VoteInexistantException());
                            else
                                serveur.ecrirePaquet(new ClePubliqueFeedbackPaquet(clePublique));
                            break;

                        case CREER_VOTE:
                            BigInteger[] cles = Chiffrement.keygen(l);
                            int idVote = stockageScrutateur.insererVote(cles[0], cles[1], cles[2], cles[3]);
                            Chiffre urneZero = Chiffrement.encrypt(0, new ClePublique(cles[0], cles[1], cles[2]));
                            serveur.ecrirePaquet(new CreerVoteFeedbackPaquet(idVote, urneZero));
                            break;

                        case DECHIFFRER:
                            DechiffrerPaquet dechiffrerPaquet = (DechiffrerPaquet) paquet;
                            clePublique = stockageScrutateur.getClePublique(dechiffrerPaquet.getIdVote());
                            clePrivee = stockageScrutateur.getClePrivee(dechiffrerPaquet.getIdVote());
                            if (clePublique == null)
                                serveur.ecrireException(new VoteInexistantException());
                            else
                                serveur.ecrirePaquet(new DechiffrerFeedbackPaquet(Chiffrement.decrypt(
                                        dechiffrerPaquet.getChiffre(),
                                        dechiffrerPaquet.getNbBulletins(),
                                        clePublique, clePrivee) / (double) dechiffrerPaquet.getNbBulletins()
                                ));
                            break;

                        case BULLETIN:
                            BulletinPaquet bulPaquet = (BulletinPaquet) paquet;
                            clePublique = stockageScrutateur.getClePublique(bulPaquet.getIdVote());
                            clePrivee = stockageScrutateur.getClePrivee(bulPaquet.getIdVote());
                            if (clePublique == null)
                                serveur.ecrireException(new VoteInexistantException());
                            else {
                                int bulletin = Chiffrement.decrypt(bulPaquet.getBulletin(), 1, clePublique, clePrivee);
                                if (bulletin != 0 && bulletin != 1)
                                    serveur.ecrireException(new BulletinInvalideException());
                                else
                                    serveur.ecrirePaquet(new ClePubliqueFeedbackPaquet(clePublique));
                            }
                            break;
                    }
            }
        } catch (IOException | ClassNotFoundException ignored) {}
    }
}