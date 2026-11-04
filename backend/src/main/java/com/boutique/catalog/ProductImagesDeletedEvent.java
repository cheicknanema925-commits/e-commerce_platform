package com.boutique.catalog;

import java.util.List;

// Photos retirées de la base : leurs fichiers seront effacés APRÈS le commit (jamais si la transaction échoue)
record ProductImagesDeletedEvent(List<String> filenames) {
}
