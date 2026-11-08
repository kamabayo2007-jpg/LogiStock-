export type Role = 'ADMINISTRATEUR' | 'GESTIONNAIRE_STOCK' | 'COMMERCIAL';

export const ROLES: { code: Role; libelle: string }[] = [
  { code: 'ADMINISTRATEUR', libelle: 'Administrateur' },
  { code: 'GESTIONNAIRE_STOCK', libelle: 'Gestionnaire de stock' },
  { code: 'COMMERCIAL', libelle: 'Commercial' },
];

export function libelleRole(code: string): string {
  return ROLES.find((r) => r.code === code)?.libelle ?? code;
}

export interface Utilisateur {
  id: number;
  nom: string;
  email: string;
  role: Role;
  actif?: boolean;
}

export interface Categorie {
  id: number;
  nom: string;
}

export interface Produit {
  id: number;
  reference: string;
  designation: string;
  description: string | null;
  prix: number;
  quantite: number;
  seuilAlerte: number;
  categorieId: number;
  categorieNom: string;
}

export interface Mouvement {
  id: number;
  date: string;
  type: 'ENTREE' | 'SORTIE';
  quantite: number;
  motif: string | null;
  produitId: number;
  utilisateurId: number;
  commandeId: number | null;
}

export interface Client {
  id: number;
  nom: string;
  prenom: string | null;
  telephone: string | null;
  email: string | null;
  adresse: string | null;
}

export interface LigneCommande {
  produitId: number;
  reference: string;
  designation: string;
  quantite: number;
  prixUnitaire: number;
  total: number;
}

export type StatutCommande = 'EN_COURS' | 'VALIDEE' | 'ANNULEE';

export interface Commande {
  id: number;
  date: string;
  statut: StatutCommande;
  clientId: number;
  clientNom: string;
  total: number;
  lignes: LigneCommande[];
}
