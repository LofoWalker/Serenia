import { Component } from '@angular/core';

/**
 * Composant pour afficher les Mentions Légales du site Serenia.
 *
 * Ce composant affiche les informations légales requises par la loi française,
 * notamment les informations de l'éditeur, du directeur de la publication
 * et de l'hébergeur.
 */
@Component({
  selector: 'app-legal-notices',
  templateUrl: './legal-notices.component.html',
  styleUrls: ['./legal-notices.component.css'],
  standalone: true
})
export class LegalNoticesComponent {
  /**
   * Informations de l'éditeur du site.
   */
  editor = {
    name: 'Tom Walker',
    status: 'Éditeur personne physique',
    email: 'tom1997walker@gmail.com'
  };

  /**
   * Informations du directeur de la publication.
   */
  publicationDirector = {
    name: 'Tom Walker'
  };

  /**
   * Informations de l'hébergeur.
   */
  hosting = {
    name: 'OVH',
    website: 'https://www.ovh.com'
  };
}

