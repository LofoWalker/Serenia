import {
  ChangeDetectionStrategy,
  Component,
  inject,
  NgZone,
  OnDestroy,
  OnInit,
  PLATFORM_ID,
  signal,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-cursor-glow',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './cursor-glow.component.html',
  styleUrl: './cursor-glow.component.css',
})
export class CursorGlowComponent implements OnInit, OnDestroy {
  private readonly ngZone = inject(NgZone);
  private readonly platformId = inject(PLATFORM_ID);

  protected readonly x = signal(0);
  protected readonly y = signal(0);
  protected readonly visible = signal(false);

  private animationFrameId: number | null = null;
  private targetX = 0;
  private targetY = 0;
  private currentX = 0;
  private currentY = 0;
  private readonly smoothing = 0.1;

  private boundMouseMove = this.onMouseMove.bind(this);
  private boundMouseLeave = this.onMouseLeave.bind(this);
  private boundMouseEnter = this.onMouseEnter.bind(this);

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    this.ngZone.runOutsideAngular(() => {
      document.addEventListener('mousemove', this.boundMouseMove, { passive: true });
      document.addEventListener('mouseleave', this.boundMouseLeave);
      document.addEventListener('mouseenter', this.boundMouseEnter);
      this.animate();
    });
  }

  ngOnDestroy(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    document.removeEventListener('mousemove', this.boundMouseMove);
    document.removeEventListener('mouseleave', this.boundMouseLeave);
    document.removeEventListener('mouseenter', this.boundMouseEnter);

    if (this.animationFrameId !== null) {
      cancelAnimationFrame(this.animationFrameId);
    }
  }

  private onMouseMove(event: MouseEvent): void {
    this.targetX = event.clientX;
    this.targetY = event.clientY;

    if (!this.visible()) {
      this.ngZone.run(() => this.visible.set(true));
    }
  }

  private onMouseLeave(): void {
    this.ngZone.run(() => this.visible.set(false));
  }

  private onMouseEnter(): void {
    this.ngZone.run(() => this.visible.set(true));
  }

  private animate(): void {
    this.currentX += (this.targetX - this.currentX) * this.smoothing;
    this.currentY += (this.targetY - this.currentY) * this.smoothing;

    this.x.set(Math.round(this.currentX));
    this.y.set(Math.round(this.currentY));

    this.animationFrameId = requestAnimationFrame(() => this.animate());
  }
}
