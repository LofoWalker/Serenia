import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { AlertComponent } from '../../../shared/ui/alert/alert.component';
@Component({
  selector: 'app-activate',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ButtonComponent, AlertComponent],
  templateUrl: './activate.component.html',
  styleUrl: './activate.component.css'
})
export class ActivateComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  protected readonly authState = inject(AuthStateService);
  protected readonly successMessage = signal('');
  protected readonly errorMessage = signal('');
  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.errorMessage.set("Token d'activation manquant ou invalide.");
      return;
    }
    this.authService.activate(token).subscribe({
      next: (response) => {
        this.successMessage.set(response.message || 'Votre compte a été activé avec succès !');
      },
      error: () => {
        this.errorMessage.set("Le lien d'activation est invalide ou a expiré. Veuillez vous réinscrire.");
      }
    });
  }
}
