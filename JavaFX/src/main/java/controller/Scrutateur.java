package controller;

import controller.communication.Connexion;
import controller.communication.RecepteurConnexion;
import controller.config.Conf;
import controller.stockage.*;
import dataobject.Chiffre;
import dataobject.ClePublique;
import dataobject.exception.BulletinInvalideException;
import dataobject.exception.VoteInexistantException;
import dataobject.paquet.*;
import dataobject.paquet.feedback.ClePubliqueFeedbackPaquet;
import dataobject.paquet.feedback.CreerVoteFeedbackPaquet;
import dataobject.paquet.feedback.DechiffrerFeedbackPaquet;
import dataobject.Chiffrement;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.sql.SQLException;

public class Scrutateur {
    private IStockageScrutateur stockageScrutateur;

    /**
     * La longueur en nombre de bits des clés générées.
     */
    private int l;
    private RecepteurConnexion serveur;


    /**
     * @param l La longueur en nombre de bits des clés générées.
     */
    public Scrutateur(int l) throws IOException, ClassNotFoundException, SQLException {


        // init BDD
        System.out.println("Connection à la BDD en cours");
        while (stockageScrutateur == null) {
            try {
                stockageScrutateur = new StockageScrutateurMySQL();
            } catch (SQLException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException z) {}
            }
        }

        this.l = l;

        System.out.println("Connexion au serveur...");
        if (Conf.UTILISE_SSL){
            System.setProperty("javax.net.ssl.trustStore", getClass().getResource("/ssl/saeTrustStore.jts").getPath());

            System.setProperty("javax.net.ssl.trustStorePassword", "caracal");
            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            serveur = new RecepteurConnexion((SSLSocket) sslSocketFactory.createSocket(Conf.IP, Conf.PORT));
        }else {
            serveur = new RecepteurConnexion(new Socket(Conf.IP, Conf.PORT));
        }

        // identification
        serveur.ecrirePaquet(new IdentificationPaquet(Connexion.Source.SCRUTATEUR));
    }

    //Construcytor for tests
    public Scrutateur(int l, IStockageScrutateur stockageScrutateur, RecepteurConnexion serveur) throws IOException, ClassNotFoundException, SQLException {
        this.stockageScrutateur = stockageScrutateur;
        this.l = l;
        this.serveur = serveur;
    }

    //duplication de code run pour processer un seul paquet a la fois necessaire pour les tests
    public void processPacket(Paquet paquet) throws IOException {
        ClePublique clePublique;
        BigInteger clePrivee;
        switch (paquet.getType()) {
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
                else {
                    int total;
                    if (Conf.DECHIFFREMENT_EXHAUSTIF)
                        total = Chiffrement.decrypt(
                                dechiffrerPaquet.getChiffre(),
                                Integer.MAX_VALUE,
                                clePublique, clePrivee);
                    else
                        total = Chiffrement.decrypt(
                                dechiffrerPaquet.getChiffre(),
                                dechiffrerPaquet.getNbBulletins(),
                                clePublique, clePrivee);
                    serveur.ecrirePaquet(new DechiffrerFeedbackPaquet(total / (double) dechiffrerPaquet.getNbBulletins()
                    ));
                }
                break;
        }
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
                            else {
                                int total;
                                if (Conf.DECHIFFREMENT_EXHAUSTIF)
                                    total = Chiffrement.decrypt(
                                            dechiffrerPaquet.getChiffre(),
                                            Integer.MAX_VALUE,
                                            clePublique, clePrivee);
                                else
                                    total = Chiffrement.decrypt(
                                            dechiffrerPaquet.getChiffre(),
                                            dechiffrerPaquet.getNbBulletins(),
                                            clePublique, clePrivee);
                                serveur.ecrirePaquet(new DechiffrerFeedbackPaquet(total / (double) dechiffrerPaquet.getNbBulletins()
                                ));
                            }
                            break;

                        /* case BULLETIN:
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
                            break; */
                    }
            }
        } catch (IOException | ClassNotFoundException ignored) {}
    }
}