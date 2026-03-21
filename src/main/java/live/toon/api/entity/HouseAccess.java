package live.toon.api.entity;

public enum HouseAccess {
    /** Tout le monde peut entrer librement */
    OPEN,
    /** Mot de passe requis pour entrer */
    PASSWORD,
    /** Maison fermée, personne ne peut entrer (sauf le propriétaire) */
    CLOSED
}
