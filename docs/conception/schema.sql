CREATE TABLE role (
  id_role       INT PRIMARY KEY AUTO_INCREMENT,
  code_role     VARCHAR(30) NOT NULL UNIQUE,
  libelle_role  VARCHAR(60) NOT NULL
);

CREATE TABLE utilisateur (
  id_utilisateur      INT PRIMARY KEY AUTO_INCREMENT,
  email               VARCHAR(100) NOT NULL UNIQUE,
  mot_de_passe_hache  VARCHAR(255) NOT NULL,
  nom_complet         VARCHAR(100) NOT NULL,
  actif               BOOLEAN NOT NULL DEFAULT TRUE,
  date_creation       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_role             INT NOT NULL,
  FOREIGN KEY (id_role) REFERENCES role(id_role)
);

CREATE TABLE categorie (
  id_categorie   INT PRIMARY KEY AUTO_INCREMENT,
  nom_categorie  VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE produit (
  id_produit      INT PRIMARY KEY AUTO_INCREMENT,
  reference       VARCHAR(30) NOT NULL UNIQUE,
  designation     VARCHAR(100) NOT NULL,
  description     TEXT,
  prix_unitaire   DECIMAL(12,2) NOT NULL CHECK (prix_unitaire >= 0),
  quantite_stock  INT NOT NULL DEFAULT 0 CHECK (quantite_stock >= 0),
  seuil_alerte    INT NOT NULL DEFAULT 0 CHECK (seuil_alerte >= 0),
  id_categorie    INT NOT NULL,
  FOREIGN KEY (id_categorie) REFERENCES categorie(id_categorie)
);

CREATE TABLE client (
  id_client      INT PRIMARY KEY AUTO_INCREMENT,
  nom_client     VARCHAR(100) NOT NULL,
  prenom_client  VARCHAR(100),
  telephone      VARCHAR(20),
  email_client   VARCHAR(100),
  adresse        VARCHAR(150)
);

CREATE TABLE commande (
  id_commande     INT PRIMARY KEY AUTO_INCREMENT,
  date_commande   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  statut          ENUM('EN_COURS','VALIDEE','ANNULEE') NOT NULL DEFAULT 'EN_COURS',
  id_client       INT NOT NULL,
  id_utilisateur  INT NOT NULL,
  FOREIGN KEY (id_client)      REFERENCES client(id_client),
  FOREIGN KEY (id_utilisateur) REFERENCES utilisateur(id_utilisateur)
);

CREATE TABLE contenir (
  id_commande          INT NOT NULL,
  id_produit           INT NOT NULL,
  quantite             INT NOT NULL CHECK (quantite > 0),
  prix_unitaire_vente  DECIMAL(12,2) NOT NULL CHECK (prix_unitaire_vente >= 0),
  PRIMARY KEY (id_commande, id_produit),
  FOREIGN KEY (id_commande) REFERENCES commande(id_commande) ON DELETE CASCADE,
  FOREIGN KEY (id_produit)  REFERENCES produit(id_produit)
);

CREATE TABLE facture (
  id_facture      INT PRIMARY KEY AUTO_INCREMENT,
  numero_facture  VARCHAR(30) NOT NULL UNIQUE,
  date_facture    DATE NOT NULL,
  montant_total   DECIMAL(12,2) NOT NULL CHECK (montant_total >= 0),
  id_commande     INT NOT NULL UNIQUE,
  FOREIGN KEY (id_commande) REFERENCES commande(id_commande)
);

CREATE TABLE mouvement_stock (
  id_mouvement    INT PRIMARY KEY AUTO_INCREMENT,
  date_mouvement  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  type_mouvement  ENUM('ENTREE','SORTIE') NOT NULL,
  quantite        INT NOT NULL CHECK (quantite > 0),
  motif           VARCHAR(100),
  id_produit      INT NOT NULL,
  id_utilisateur  INT NOT NULL,
  id_commande     INT NULL,
  FOREIGN KEY (id_produit)     REFERENCES produit(id_produit),
  FOREIGN KEY (id_utilisateur) REFERENCES utilisateur(id_utilisateur),
  FOREIGN KEY (id_commande)    REFERENCES commande(id_commande)
);

INSERT INTO role (code_role, libelle_role) VALUES
  ('ADMINISTRATEUR',    'Administrateur'),
  ('GESTIONNAIRE_STOCK','Gestionnaire de stock'),
  ('COMMERCIAL',        'Commercial');
