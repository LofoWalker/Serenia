import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PrivacyPolicyComponent } from './privacy-policy.component';

/**
 * Tests unitaires pour le composant PrivacyPolicyComponent.
 *
 * Teste que le composant affiche correctement les informations de politique de confidentialité,
 * notamment les données du responsable de traitement, les droits RGPD, et les mesures de sécurité.
 */
describe('PrivacyPolicyComponent', () => {
  let component: PrivacyPolicyComponent;
  let fixture: ComponentFixture<PrivacyPolicyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrivacyPolicyComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(PrivacyPolicyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  it('devrait afficher les informations du responsable de traitement correctement', () => {
    expect(component.dataController.name).toBe('Tom Walker');
    expect(component.dataController.email).toBe('tom1997walker@gmail.com');
  });

  it('devrait afficher une date de dernière mise à jour valide', () => {
    expect(component.lastUpdated).toBeTruthy();
    expect(component.lastUpdated).toContain('2026');
  });

  it('devrait avoir un contact RGPD valide', () => {
    expect(component.rgpdContact).toBe('tom1997walker@gmail.com');
  });

  it('devrait avoir des mesures de sécurité définies', () => {
    expect(component.securityMeasures.length).toBeGreaterThan(0);
    expect(component.securityMeasures).toContain('Chiffrement AES-256-GCM des conversations');
  });

  it('devrait avoir des droits GDPR définis', () => {
    expect(component.gdprRights.length).toBeGreaterThan(0);
    expect(component.gdprRights[0]).toContain('accès');
  });

  it('devrait afficher le titre "Politique de Confidentialité" dans le template', () => {
    const titleElement = fixture.nativeElement.querySelector('.policy-header h1');
    expect(titleElement.textContent).toContain('Politique de Confidentialité');
  });

  it('devrait afficher un lien mailto pour le contact', () => {
    const emailLink = fixture.nativeElement.querySelector('a[href^="mailto:"]');
    expect(emailLink).toBeTruthy();
  });

  it('devrait afficher la liste des mesures de sécurité', () => {
    const listItems = fixture.nativeElement.querySelectorAll('.policy-list li');
    expect(listItems.length).toBeGreaterThan(0);
  });

  it('devrait avoir la classe standalone pour le composant', () => {
    const componentMetadata = (PrivacyPolicyComponent as any).ɵcmp;
    expect(componentMetadata.standalone).toBe(true);
  });

  it('devrait afficher 12 sections principales', () => {
    const sections = fixture.nativeElement.querySelectorAll('.policy-section');
    expect(sections.length).toBe(12);
  });

  it('devrait contenir des liens sécurisés (target blank avec rel noopener)', () => {
    const externalLinks = fixture.nativeElement.querySelectorAll('a[target="_blank"]');
    externalLinks.forEach((link: HTMLElement) => {
      expect(link.getAttribute('rel')).toContain('noopener');
    });
  });
});

