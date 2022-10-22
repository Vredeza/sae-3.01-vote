import java.io.*;
import java.net.*;
import java.math.BigInteger;

public class Serveur {
    private Vote vote;
    private Chiffre somme;

    private ObjectOutputStream outputScrutateur;
    private ObjectInputStream inputScrutateur;

    private ObjectOutputStream outputClient;
    private ObjectInputStream inputClient;

    ServerSocket serverSocket;

    public Serveur() {
        try {
            // initialisation à 0
            somme = new Chiffre(BigInteger.ONE, BigInteger.ONE);

            // ouvre le serveur
            serverSocket = new ServerSocket(2999);

            // attend la connexion du scrutateur
            Socket scrutateur = serverSocket.accept();
            outputScrutateur = new ObjectOutputStream(scrutateur.getOutputStream());
            inputScrutateur = new ObjectInputStream(scrutateur.getInputStream());


            /*
            // attend la connexion du client
            Socket client = serverSocket.accept();
            outputClient = new ObjectOutputStream(client.getOutputStream());
            inputClient = new ObjectInputStream(client.getInputStream());

             */

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            try {

                // attend la connexion du client
                Socket socket = serverSocket.accept();

                //quand une connexion est reçue, la traite dans un Thread différent
                new Thread(new ConnexionVersClient(socket)).start();

            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ClePublique demanderClePublique() throws IOException, ClassNotFoundException {
        outputScrutateur.writeObject(Requete.SERVEUR_DEMANDER_CLE_PUBLIQUE);
        return (ClePublique) inputScrutateur.readObject();
    }

    public void creerVote(String intitule, String option1, String option2) {
        try {
            vote = new Vote(intitule, option1, option2);
            outputScrutateur.writeObject(Requete.SERVEUR_CREER_VOTE);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void agreger(Chiffre c) {
        try {
            ClePublique clePublique = demanderClePublique();
            BigInteger p = clePublique.getP();

            // def Chiffré agrégé
            somme = new Chiffre(somme.getU().multiply(c.getU()).mod(p), somme.getV().multiply(c.getV()).mod(p));

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private class ConnexionVersClient implements Runnable{
        private Socket socketClient;
        private ObjectInputStream inputClient;
        private ObjectOutputStream outputClient;

        public ConnexionVersClient(Socket clientSocket) throws IOException {
            this.socketClient = clientSocket;
            this.inputClient = new ObjectInputStream(clientSocket.getInputStream());
            this.outputClient = new ObjectOutputStream(clientSocket.getOutputStream());
            System.out.println("Connexion au client " + socketClient.getPort());
        }

        @Override
        public void run() {
            try {
                while (true){
                    // attend une requête du client
                    Requete requete = (Requete) inputClient.readObject();
                    System.out.println(requete); // debug

                    // traîte la requête
                    switch (requete) {
                        case CLIENT_DEMANDER_CLE_PUBLIQUE:
                            ClePublique clePublique = demanderClePublique();
                            outputClient.writeObject(clePublique);
                            break;
                        case CLIENT_VOTER:
                            agreger((Chiffre) inputClient.readObject());
                            break;
                        case TEST_FINIR_VOTE:
                            outputScrutateur.writeObject(Requete.TEST_FINIR_VOTE);
                            outputScrutateur.writeObject(somme);
                            break;
                    }
                }
            }catch (IOException e){
                System.out.println("Client " + socketClient.getPort() + " déconnecté");
            }catch (ClassNotFoundException e){
                System.out.println("CLASSNOTFOUND");
            }
        }
    }
}