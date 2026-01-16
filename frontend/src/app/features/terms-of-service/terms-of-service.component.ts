import { Component } from '@angular/core';

/**
 * Composant pour afficher les Conditions Générales d'Utilisation de Serenia.
 *
 * Ce composant affiche les conditions d'utilisation du service, les limitations,
 * les responsabilités et les droits applicables conformément au droit français.
 */
@Component({
  selector: 'app-terms-of-service',
  templateUrl: './terms-of-service.component.html',
  styleUrls: ['./terms-of-service.component.css'],
  standalone: true
})
export class TermsOfServiceComponent {
  /**
   * Date de la dernière mise à jour des conditions.
   */
  lastUpdated = new Date('2026-01-16').toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });

  /**
   * Numéro d'urgence pour les situations de crise.
   */
  crisisHotline = '3114';

  /**
   * Informations du service.
   */
  serviceInfo = {
    name: 'Serenia',
    purpose: 'espace de discussion visant à permettre aux utilisateurs d\'exprimer leurs ressentis dans un cadre bienveillant',
    notMedical: true,
    notPsychological: true,
    notTherapeutic: true
  };

  /**
   * Limitations du service.
   */
  serviceLimitations = [
    'Les réponses sont générées automatiquement par une intelligence artificielle',
    'Elles ne constituent en aucun cas un avis médical, psychologique ou professionnel',
    'Elles ne remplacent pas l\'intervention d\'un professionnel de santé',
    'Le service est fourni « en l\'état », sans garantie de disponibilité continue'
  ];

  /**
   * Informations relatives à la crise.
   */
  crisisInfo = {
    hotline: '3114',
    country: 'France',
    available: '24h/24, 7 jours sur 7',
    free: true
  };
}

