async function copyToClipboard(id) {
    const field = document.getElementById("form:" + id);
    if (!field) {
        return;
    }

    try {
        await navigator.clipboard.writeText(field.value);
    } catch (error) {
        field.focus();
        field.select();
        if (!document.execCommand("copy")) {
            alert("Copie impossible. Utilisez Ctrl+C sur le texte sélectionné.");
        }
    }
}

function toutEffacer() {
    document.getElementById("form:question").value = "";
    document.getElementById("form:reponse").value = "";
}
