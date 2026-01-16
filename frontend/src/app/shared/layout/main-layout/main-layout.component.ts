import {ChangeDetectionStrategy, Component} from '@angular/core';
import {RouterOutlet, RouterLink} from '@angular/router';
import {HeaderComponent} from '../header/header.component';

@Component({
  selector: 'app-main-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, HeaderComponent],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.css'
})
export class MainLayoutComponent {}
