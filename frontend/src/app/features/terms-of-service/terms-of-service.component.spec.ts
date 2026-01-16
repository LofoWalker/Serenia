import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TermsOfServiceComponent } from './terms-of-service.component';

/**
 * Tests unitaires pour le composant TermsOfServiceComponent.
 *
 * Teste que le composant affiche correctement les conditions générales d'utilisation,
 * notamment les limitations du service, les informations de crise et les responsabilités.
 */
describe('TermsOfServiceComponent', () => {
  let component: TermsOfServiceComponent;
  let fixture: ComponentFixture<TermsOfServiceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TermsOfServiceComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TermsOfServiceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  it('devrait afficher les informations du service correctement', () => {
    expect(component.serviceInfo.name).toBe('Serenia');
    expect(component.serviceInfo.notMedical).toBe(true);
    expect(component.serviceInfo.notPsychological).toBe(true);
    expect(component.serviceInfo.notTherapeutic).toBe(true);
  });

  it('devrait afficher une date de dernière mise à jour valide', () => {
    expect(component.lastUpdated).toBeTruthy();
    expect(component.lastUpdated).toContain('2026');
  });

  it('devrait avoir le numéro de crise 3114', () => {
    expect(component.crisisHotline).toBe('3114');
  });

  it('devrait avoir des limitations du service définies', () => {
    expect(component.serviceLimitations.length).toBeGreaterThan(0);
    expect(component.serviceLimitations[0]).toContain('intelligence artificielle');
  });

  it('devrait avoir des informations de crise correctes', () => {
    expect(component.crisisInfo.hotline).toBe('3114');
    expect(component.crisisInfo.country).toBe('France');
    expect(component.crisisInfo.free).toBe(true);
  });

  it('devrait afficher le titre "Conditions Générales d\'Utilisation" dans le template', () => {
    const titleElement = fixture.nativeElement.querySelector('.terms-header h1');
    expect(titleElement.textContent).toContain('Conditions Générales d\'Utilisation');
  });

  it('devrait afficher le numéro de crise dans le template', () => {
    const crisisNumber = fixture.nativeElement.querySelector('.crisis-number');
    expect(crisisNumber).toBeTruthy();
    expect(crisisNumber.textContent).toContain('3114');
  });

  it('devrait afficher la liste des limitations du service', () => {
    const listItems = fixture.nativeElement.querySelectorAll('.terms-list li');
    expect(listItems.length).toBeGreaterThan(0);
  });

  it('devrait avoir la classe standalone pour le composant', () => {
    const componentMetadata = (TermsOfServiceComponent as any).ɵcmp;
    expect(componentMetadata.standalone).toBe(true);
  });

  it('devrait afficher 12 sections principales', () => {
    const sections = fixture.nativeElement.querySelectorAll('.terms-section');
    expect(sections.length).toBe(12);
  });

  it('devrait contenir un lien mailto pour le contact', () => {
    const emailLink = fixture.nativeElement.querySelector('a[href^="mailto:"]');
    expect(emailLink).toBeTruthy();
    expect(emailLink.textContent).toContain('tom1997walker@gmail.com');
  });

  it('devrait afficher un avertissement sur la non-conformité médicale', () => {
    const warningSection = fixture.nativeElement.querySelector('.terms-warning');
    expect(warningSection).toBeTruthy();
    expect(warningSection.textContent).toContain('Important');
  });

  it('devrait afficher une section de crise avec informations d\'urgence', () => {
    const crisisSection = fixture.nativeElement.querySelector('.terms-crisis');
    expect(crisisSection).toBeTruthy();
    expect(crisisSection.textContent).toContain('3114');
  });
});

