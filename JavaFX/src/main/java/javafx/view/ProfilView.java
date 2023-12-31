package javafx.view;

import controller.Client;
import app.AppClientGraphique;
import javafx.controller.ProfilController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.VBox;

import java.io.IOException;


public class ProfilView extends Node {




    private Scene scene;

    private ProfilController modifController;

    private ModifUtilisateurView modifUtilisateur;

    private VBox backgrounVBOX;
    private Client myClient;
    ColorAdjust flou;
    ColorAdjust net;

    private AppClientGraphique myAppli;
    public ProfilView() throws IOException {
        FXMLLoader fxml= new FXMLLoader(ModifUtilisateurView.class.getResource("/javafx/VueProfil.fxml"));
        scene = new Scene(fxml.load());
        scene.getStylesheets().add(getClass().getResource("/javafx/vueListeVote.css").toExternalForm());

        //this.setTitle("Profil");
        //this.setScene(scene);

        flou = new ColorAdjust(0, -0.9, -0.5, 0);
        GaussianBlur blur1 = new GaussianBlur(55);
        flou.setInput(blur1);
        net = new ColorAdjust(0, 0, 0, 0);
        GaussianBlur blur = new GaussianBlur(0);
        net.setInput(blur);


        modifController = fxml.getController();
        //modifController.setMyApp(mainApp);


        backgrounVBOX = (VBox) this.scene.getRoot();



    }

   /* public void afficher() {
        this.show();
    }*/


    public AppClientGraphique getMyApp() {
        return myAppli;
    }

    public void setFlou(){

        backgrounVBOX.setEffect(flou);
    }

    public void setDefloutage(){

        backgrounVBOX.setEffect(net);
    }


    public void afficheVueModif() {
        //modifUtilisateur = new ModifUtilisateurView(myAppli);
        modifUtilisateur.setterForController();
        setFlou();
        modifUtilisateur.afficher();
    }

    public void hideVueModif() {
        //modifUtilisateur = new ModifUtilisateurView(myAppli);
        modifUtilisateur.hide();
        setDefloutage();
    }


    public Client getClient() {
        return myClient;
    }
}
