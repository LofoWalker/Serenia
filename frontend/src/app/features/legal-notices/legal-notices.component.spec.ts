import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LegalNoticesComponent } from './legal-notices.component';

/**
 * Tests unitaires pour le composant LegalNoticesComponent.
 *
 * Teste que le composant affiche correctement les informations légales,
 * notamment les données de l'éditeur, du directeur de publication et de l'hébergeur.
 */
describe('LegalNoticesComponent', () => {
  let component: LegalNoticesComponent;
  let fixture: ComponentFixture<LegalNoticesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LegalNoticesComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(LegalNoticesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  it('devrait afficher les informations de l\'éditeur correctement', () => {
    expect(component.editor.name).toBe('Tom Walker');
    expect(component.editor.status).toBe('Éditeur personne physique');
    expect(component.editor.email).toBe('tom1997walker@gmail.com');
  });

  it('devrait afficher les informations du directeur de publication correctement', () => {
    expect(component.publicationDirector.name).toBe('Tom Walker');
  });

  it('devrait afficher les informations de l\'hébergeur correctement', () => {
    expect(component.hosting.name).toBe('OVH');
    expect(component.hosting.website).toBe('https://www.ovh.com');
  });

  it('devrait afficher le titre "Mentions Légales" dans le template', () => {
    const titleElement = fixture.nativeElement.querySelector('.legal-header h1');
    expect(titleElement.textContent).toContain('Mentions Légales');
  });

  it('devrait afficher le nom de l\'éditeur dans le template', () => {
    const editorNameElement = fixture.nativeElement.querySelector('.legal-info');
    expect(editorNameElement.textContent).toContain('Tom Walker');
  });

  it('devrait afficher un lien mailto pour l\'email de l\'éditeur', () => {
    const emailLink = fixture.nativeElement.querySelector('a[href^="mailto:"]');
    expect(emailLink).toBeTruthy();
    expect(emailLink.getAttribute('href')).toBe('mailto:tom1997walker@gmail.com');
  });

  it('devrait afficher un lien vers le site d\'OVH', () => {
    const ovhLink = fixture.nativeElement.querySelector('a[href="https://www.ovh.com"]');
    expect(ovhLink).toBeTruthy();
  });

  it('devrait avoir la classe standalone pour le composant', () => {
    const componentMetadata = (LegalNoticesComponent as any).ɵcmp;
    expect(componentMetadata.standalone).toBe(true);
  });
});

