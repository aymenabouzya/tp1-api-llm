
package ht.fds.mbds.aymenabouzya.jsf;

import ht.fds.mbds.aymenabouzya.llm.JsonAdapterPourGemini;
import ht.fds.mbds.aymenabouzya.llm.LlmInteraction;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Named
@ViewScoped
public class Bb implements Serializable {

    private static final long serialVersionUID = 1L;

    private String roleSysteme;
    private boolean roleSystemeChangeable = true;

    private List<SelectItem> listeRolesSysteme;

    private String question;
    private String reponse;

    private StringBuilder conversation = new StringBuilder();

    private boolean debug;

    private String texteRequeteJson;
    private String texteReponseJson;

    @Inject
    private JsonAdapterPourGemini jsonAdapter;

    @Inject
    private FacesContext facesContext;

    public Bb() {
    }

    public String getRoleSysteme() {
        return roleSysteme;
    }

    public void setRoleSysteme(String roleSysteme) {
        this.roleSysteme = roleSysteme;
    }

    public boolean isRoleSystemeChangeable() {
        return roleSystemeChangeable;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return reponse;
    }

    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getConversation() {
        return conversation.toString();
    }

    public void setConversation(String conversation) {
        this.conversation = new StringBuilder(
                conversation == null ? "" : conversation
        );
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getTexteRequeteJson() {
        return texteRequeteJson;
    }

    public void setTexteRequeteJson(String texteRequeteJson) {
        this.texteRequeteJson = texteRequeteJson;
    }

    public String getTexteReponseJson() {
        return texteReponseJson;
    }

    public void setTexteReponseJson(String texteReponseJson) {
        this.texteReponseJson = texteReponseJson;
    }

    /**
     * Envoie une question à Gemini.
     */
    public String envoyer() {

        if (question == null || question.isBlank()) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Texte question vide",
                    "Il manque le texte de la question."
            );

            facesContext.addMessage(null, message);
            return null;
        }

        // Définition du rôle système au début de la conversation
        if (conversation.isEmpty()) {

            roleSystemeChangeable = false;

            jsonAdapter.setSystemRole(roleSysteme);
        }

        try {

            LlmInteraction interaction =
                    jsonAdapter.envoyerRequete(question);

            reponse = interaction.reponseExtraite();

            texteRequeteJson = interaction.questionJson();

            texteReponseJson = interaction.reponseJson();

            // Ajout de l'échange uniquement si Gemini a répondu
            afficherConversation();

        } catch (Exception e) {

            reponse = "Erreur : " + e.getMessage();

            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Problème de connexion avec l'API Gemini",
                    "Erreur : " + e.getMessage()
                            + "\n"
                            + Arrays.toString(e.getStackTrace())
            );

            facesContext.addMessage(null, message);
        }

        // Préparation du champ pour la prochaine question
        question = null;

        return null;
    }

    /**
     * Commence une nouvelle conversation.
     */
    public String nouveauChat() {

        jsonAdapter.nouvelleConversation();

        conversation = new StringBuilder();

        question = null;
        reponse = null;

        texteRequeteJson = null;
        texteReponseJson = null;

        roleSystemeChangeable = true;

        return "index?faces-redirect=true";
    }

    /**
     * Affiche les échanges dans la conversation.
     */
    private void afficherConversation() {

        conversation.append("== User:\n")
                .append(question)
                .append("\n== Serveur:\n")
                .append(reponse)
                .append("\n");
    }

    /**
     * Liste des rôles système disponibles.
     */
    public List<SelectItem> getRolesSysteme() {

        if (listeRolesSysteme == null) {

            listeRolesSysteme = new ArrayList<>();

            String role = """
                    You are a helpful assistant.
                    You help the user to find the information they need.
                    If the user types a question, you answer it.
                    """;

            listeRolesSysteme.add(
                    new SelectItem(role, "Assistant")
            );

            role = """
                    You are an interpreter.
                    You translate from English to French and from French to English.
                    If the user types a French text, you translate it into English.
                    If the user types an English text, you translate it into French.
                    If the text contains only one to three words, give some examples of usage of these words in English.
                    """;

            listeRolesSysteme.add(
                    new SelectItem(role, "Traducteur Anglais-Français")
            );

            role = """
                    You are a travel guide.
                    If the user types the name of a country or a town,
                    you tell them what are the main places to visit in that country or town.
                    You also tell them the average price of a meal.
                    """;

            listeRolesSysteme.add(
                    new SelectItem(role, "Guide touristique")
            );
        }

        return listeRolesSysteme;
    }
}
