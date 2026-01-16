import { Component } from '@angular/core';

/**
 * Composant pour afficher la Politique de Confidentialité du site Serenia.
 *
 * Ce composant affiche les informations relatives à la protection des données personnelles,
 * conformément au Règlement Général sur la Protection des Données (RGPD) et à la loi
 * Informatique et Libertés française.
 */
@Component({
  selector: 'app-privacy-policy',
  templateUrl: './privacy-policy.component.html',
  styleUrls: ['./privacy-policy.component.css'],
  standalone: true
})
export class PrivacyPolicyComponent {
  /**
   * Informations du responsable du traitement des données.
   */
  dataController = {
    name: 'Tom Walker',
    email: 'tom1997walker@gmail.com'
  };

  /**
   * Date de la dernière mise à jour de la politique.
   */
  lastUpdated = new Date('2026-01-16').toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });

  /**
   * Contact pour les demandes RGPD.
   */
  rgpdContact = 'tom1997walker@gmail.com';

  /**
   * Informations sur les mesures de sécurité techniques.
   */
  securityMeasures = [
    'Chiffrement AES-256-GCM avec clé unique par utilisateur (HKDF)',
    'Isolation cryptographique : chaque utilisateur dispose de sa propre clé de chiffrement',
    'Clés RSA 2048 bits pour la signature JWT',
    'Transport sécurisé via HTTPS/TLS',
    'Infrastructure PostgreSQL sécurisée',
    'Accès restreint aux systèmes'
  ];

  /**
   * Droits GDPR disponibles pour les utilisateurs.
   */
  gdprRights = [
    'Droit d\'accès à vos données',
    'Droit de rectification de vos données',
    'Droit à l\'effacement (droit à l\'oubli)',
    'Droit à la limitation du traitement',
    'Droit à la portabilité des données'
  ];
}

