package controller;

import dataobject.Utilisateur;
import dataobject.Vote;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CBDServeur extends AbstractCBD{

    public CBDServeur() throws SQLException {
        super("bouazzatiy", "Azertyuiop");
    }

    /**
     *
     * @param vote Le vote à insérer dans la BD (intitulé, option1, option2).
     * @return L'id du vote créé.
     */
    public int creerVote(Vote vote) throws SQLException {

        PreparedStatement statement = super.getConnection().prepareStatement(
                "CALL SAECREERVOTE(?, ?, ?)"
        );

        statement.setString(1, vote.getIntitule());
        statement.setString(2, vote.getOption1());
        statement.setString(3, vote.getOption2());

        statement.executeQuery();

        //choppe le dernier vote créer, c'est sale, mais j'ai pas d'autre solution pour le moment
        ResultSet result = super.getConnection().createStatement().executeQuery(
                "SELECT MAX(IDVOTE)" +
                        "FROM SAEVOTES"
        );
        result.next();

        return result.getInt(1);
    }

    /**
     * @param utilisateur L'utilisateur à insérer dans la BD (login, mdp)
     */
    public void creerUtilisateur(Utilisateur utilisateur) throws SQLException {
        PreparedStatement statement = super.getConnection().prepareStatement(
                "CALL SAECREERUTILISATEUR(?, ?, ?)"
        );

        statement.setString(1, utilisateur.getIdentifiant());
        statement.setString(2, utilisateur.getMotDePasseHache());

        statement.executeQuery();
    }

    /**
     * @param utilisateur L'utilisateur dont on souhaite le mot de passe.
     * @return Le mot de passe haché de l'utilisateur.
     */
    public String getMotDePasseUtilisateur(Utilisateur utilisateur) throws SQLException {
        PreparedStatement statement = super.getConnection().prepareStatement(
                "SELECT SAEGETMOTDEPASSEUTILISATEUR(?)" +
                        "FROM DUAL"
        );

        statement.setString(1, utilisateur.getIdentifiant());

        ResultSet result = statement.executeQuery();
        result.next();

        return result.getString(1);
    }
}
