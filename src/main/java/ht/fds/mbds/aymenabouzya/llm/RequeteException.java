
package ht.fds.mbds.aymenabouzya.llm;

import java.io.Serial;

public class RequeteException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    private int status;
    private String requeteJson;

    // Constructeur avec le code HTTP
    public RequeteException(int status) {
        super("Erreur HTTP : " + status);
        this.status = status;
    }

    // Constructeur avec un message
    public RequeteException(String message) {
        super(message);
    }

    // Constructeur avec un message et le JSON envoyé
    public RequeteException(String message, String requeteJson) {
        super(message);
        this.requeteJson = requeteJson;
    }

    // Constructeur avec code HTTP, message et JSON envoyé
    public RequeteException(int status, String message, String requeteJson) {
        super(message);
        this.status = status;
        this.requeteJson = requeteJson;
    }

    // Constructeur vide
    public RequeteException() {
        super();
    }

    public int getStatus() {
        return status;
    }

    public String getRequeteJson() {
        return requeteJson;
    }
}
