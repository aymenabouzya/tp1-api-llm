
package ht.fds.mbds.aymenabouzya.llm;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ViewScoped
public class JsonAdapterPourGemini implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private LlmClientPourGemini llmClient;

    private String systemRole;

    // Les messages sont conservés sous forme de chaînes JSON sérialisables.
    private List<String> historique = new ArrayList<>();

    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
    }

    private JsonObject creerRequeteJson() {

        JsonObjectBuilder requestBuilder = Json.createObjectBuilder();

        if (systemRole != null && !systemRole.isBlank()) {
            JsonObject systemInstruction = Json.createObjectBuilder()
                    .add("parts", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("text", systemRole)))
                    .build();

            requestBuilder.add("systemInstruction", systemInstruction);
        }

        JsonArrayBuilder contentsBuilder = Json.createArrayBuilder();

        for (String messageJson : historique) {
            try (JsonReader reader = Json.createReader(
                    new StringReader(messageJson))) {
                contentsBuilder.add(reader.readObject());
            }
        }

        requestBuilder.add("contents", contentsBuilder);

        return requestBuilder.build();
    }

    private JsonObject ajouteQuestionDansJsonRequete(
            JsonObject requete, String question) {

        JsonArrayBuilder contentsBuilder = Json.createArrayBuilder();

        JsonArray ancienContenu = requete.getJsonArray("contents");

        for (JsonObject message :
                ancienContenu.getValuesAs(JsonObject.class)) {
            contentsBuilder.add(message);
        }

        JsonObject userMessage = Json.createObjectBuilder()
                .add("role", "user")
                .add("parts", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("text", question)))
                .build();

        contentsBuilder.add(userMessage);

        JsonObjectBuilder nouveauBuilder = Json.createObjectBuilder();

        if (requete.containsKey("systemInstruction")) {
            nouveauBuilder.add(
                    "systemInstruction",
                    requete.getJsonObject("systemInstruction")
            );
        }

        nouveauBuilder.add("contents", contentsBuilder);

        return nouveauBuilder.build();
    }

    public LlmInteraction envoyerRequete(String question)
            throws RequeteException {

        if (question == null || question.isBlank()) {
            throw new RequeteException("La question est vide.");
        }

        JsonObject requete = creerRequeteJson();
        requete = ajouteQuestionDansJsonRequete(requete, question);

        String requeteJsonString = prettyPrinting(requete.toString());

        Entity<String> entity = Entity.entity(
                requete.toString(),
                MediaType.APPLICATION_JSON_TYPE
        );

        String reponseJsonString;

        try (Response response = llmClient.envoyerRequete(entity)) {

            int status = response.getStatus();
            reponseJsonString = response.readEntity(String.class);

            if (status != 200) {
                throw new RequeteException(
                        status,
                        "Erreur HTTP Gemini : " + status
                                + " - " + reponseJsonString,
                        requeteJsonString
                );
            }

        } catch (RequeteException e) {
            throw e;

        } catch (Exception e) {
            throw new RequeteException(
                    "Erreur lors de la communication avec Gemini : "
                            + e.getMessage(),
                    requeteJsonString
            );
        }

        String texteReponse = extractReponse(reponseJsonString);

        JsonObject userMessage = Json.createObjectBuilder()
                .add("role", "user")
                .add("parts", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("text", question)))
                .build();

        JsonObject modelMessage = Json.createObjectBuilder()
                .add("role", "model")
                .add("parts", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("text", texteReponse)))
                .build();

        historique.add(userMessage.toString());
        historique.add(modelMessage.toString());

        return new LlmInteraction(
                requeteJsonString,
                prettyPrinting(reponseJsonString),
                texteReponse
        );
    }

    private String extractReponse(String jsonReponse)
            throws RequeteException {

        try (JsonReader reader = Json.createReader(
                new StringReader(jsonReponse))) {

            JsonObject root = reader.readObject();

            JsonArray candidates = root.getJsonArray("candidates");

            if (candidates == null || candidates.isEmpty()) {
                throw new RequeteException(
                        "Gemini n'a retourné aucun candidat : "
                                + jsonReponse
                );
            }

            JsonObject content = candidates.getJsonObject(0)
                    .getJsonObject("content");

            if (content == null) {
                throw new RequeteException(
                        "La réponse Gemini ne contient pas de contenu."
                );
            }

            JsonArray parts = content.getJsonArray("parts");

            if (parts == null || parts.isEmpty()) {
                throw new RequeteException(
                        "La réponse Gemini ne contient aucun texte."
                );
            }

            StringBuilder resultat = new StringBuilder();

            for (JsonObject part :
                    parts.getValuesAs(JsonObject.class)) {

                if (part.containsKey("text")) {
                    resultat.append(part.getString("text"));
                }
            }

            if (resultat.isEmpty()) {
                throw new RequeteException(
                        "Aucun texte exploitable dans la réponse Gemini."
                );
            }

            return resultat.toString();

        } catch (RequeteException e) {
            throw e;

        } catch (Exception e) {
            throw new RequeteException(
                    "Erreur d'analyse du JSON Gemini : "
                            + e.getMessage()
            );
        }
    }

    private String prettyPrinting(String json) {

        try (JsonReader reader = Json.createReader(
                new StringReader(json));
             StringWriter stringWriter = new StringWriter()) {

            JsonObject jsonObject = reader.readObject();

            Map<String, Object> config = new HashMap<>();
            config.put(JsonGenerator.PRETTY_PRINTING, true);

            JsonWriterFactory writerFactory =
                    Json.createWriterFactory(config);

            try (JsonWriter writer =
                         writerFactory.createWriter(stringWriter)) {
                writer.writeObject(jsonObject);
            }

            return stringWriter.toString();

        } catch (Exception e) {
            return json;
        }
    }

    public void nouvelleConversation() {
        historique.clear();
        systemRole = null;
    }
}
