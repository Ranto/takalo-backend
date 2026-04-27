package ara.project.takalo.purchase.domain.exception;

import ara.project.takalo.purchase.domain.model.ImportFormat;

public class UnsupportedImportFormatException extends RuntimeException {
    public UnsupportedImportFormatException(ImportFormat format) {
        super("Format non supporté " + format.name());
    }

    public UnsupportedImportFormatException(String filename) {
        super("Format non supporté pour le fichier : " + (filename == null ? "(inconnu)" : filename));
    }
}
