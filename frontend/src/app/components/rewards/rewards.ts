import { Component, OnInit, signal, ViewChild, ElementRef } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { RewardService, RewardSummary } from '../../services/reward.service';

@Component({
  selector: 'app-rewards',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './rewards.html',
  styleUrl: './rewards.css',
})
export class RewardsComponent implements OnInit {
  rewardSummary = signal<RewardSummary | null>(null);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string | null>(null);

  // Scratch Card Modal states
  isModalOpen = signal<boolean>(false);
  activeCardTier = signal<number>(500);
  revealedPrize = signal<string | null>(null);
  isRedeeming = signal<boolean>(false);
  scratchPercentage = signal<number>(0);
  hasScratchedEnough = signal<boolean>(false);

  private _canvas?: HTMLCanvasElement;
  isDrawing = false;

  @ViewChild('scratchCanvas') set canvasRef(ref: ElementRef<HTMLCanvasElement> | undefined) {
    if (ref) {
      this._canvas = ref.nativeElement;
      this.setupCanvas();
    }
  }

  constructor(
    private authService: AuthService,
    private rewardService: RewardService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.authService.showToast('No active session. Please log in.', 'danger');
      this.router.navigate(['/login']);
      return;
    }

    this.fetchRewards();
  }

  fetchRewards(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.rewardService.getMyRewards().subscribe({
      next: (summary) => {
        this.rewardSummary.set(summary);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set('Failed to fetch rewards details. Please try again.');
        console.error(err);
      },
    });
  }

  openScratchCardModal(points: number): void {
    const currentPoints = this.rewardSummary()?.totalPoints || 0;
    if (currentPoints < points) {
      this.authService.showToast(`Requires at least ${points} points to scratch!`, 'danger');
      return;
    }
    this.activeCardTier.set(points);
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.revealedPrize.set(null);
    this.hasScratchedEnough.set(false);
  }

  setupCanvas(): void {
    const canvas = this._canvas;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    this.hasScratchedEnough.set(false);
    this.revealedPrize.set(null);
    this.scratchPercentage.set(0);

    const width = canvas.width = 300;
    const height = canvas.height = 180;

    const isGold = this.activeCardTier() === 1000;
    const grad = ctx.createLinearGradient(0, 0, width, height);
    if (isGold) {
      grad.addColorStop(0, '#ffd700');
      grad.addColorStop(0.5, '#f5c518');
      grad.addColorStop(1, '#b8860b');
    } else {
      grad.addColorStop(0, '#d3d3d3');
      grad.addColorStop(0.5, '#a9a9a9');
      grad.addColorStop(1, '#808080');
    }

    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, width, height);

    ctx.fillStyle = 'rgba(255, 255, 255, 0.1)';
    for (let i = 0; i < 200; i++) {
      const x = Math.random() * width;
      const y = Math.random() * height;
      const r = Math.random() * 20 + 5;
      ctx.beginPath();
      ctx.arc(x, y, r, 0, Math.PI * 2);
      ctx.fill();
    }

    ctx.strokeStyle = isGold ? '#b8860b' : '#555';
    ctx.lineWidth = 4;
    ctx.strokeRect(2, 2, width - 4, height - 4);

    ctx.fillStyle = '#0b0c10';
    ctx.font = 'bold 16px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('SCRATCH HERE', width / 2, height / 2 - 10);
    ctx.font = '12px sans-serif';
    ctx.fillText('to reveal your prize!', width / 2, height / 2 + 15);
  }

  onMouseDown(event: MouseEvent): void {
    this.isDrawing = true;
    this.scratch(event.offsetX, event.offsetY);
  }

  onMouseMove(event: MouseEvent): void {
    if (!this.isDrawing) return;
    this.scratch(event.offsetX, event.offsetY);
  }

  onMouseUp(): void {
    this.isDrawing = false;
  }

  onTouchStart(event: TouchEvent): void {
    event.preventDefault();
    this.isDrawing = true;
    const rect = this._canvas?.getBoundingClientRect();
    if (rect && event.touches.length > 0) {
      const x = event.touches[0].clientX - rect.left;
      const y = event.touches[0].clientY - rect.top;
      this.scratch(x, y);
    }
  }

  onTouchMove(event: TouchEvent): void {
    event.preventDefault();
    if (!this.isDrawing) return;
    const rect = this._canvas?.getBoundingClientRect();
    if (rect && event.touches.length > 0) {
      const x = event.touches[0].clientX - rect.left;
      const y = event.touches[0].clientY - rect.top;
      this.scratch(x, y);
    }
  }

  onTouchEnd(): void {
    this.isDrawing = false;
  }

  scratch(x: number, y: number): void {
    const canvas = this._canvas;
    if (!canvas || this.hasScratchedEnough() || this.isRedeeming()) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.globalCompositeOperation = 'destination-out';
    ctx.beginPath();
    ctx.arc(x, y, 20, 0, Math.PI * 2);
    ctx.fill();

    this.checkScratchPercentage();
  }

  checkScratchPercentage(): void {
    const canvas = this._canvas;
    if (!canvas || this.hasScratchedEnough()) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const width = canvas.width;
    const height = canvas.height;
    const imgData = ctx.getImageData(0, 0, width, height);
    const data = imgData.data;
    let transparentCount = 0;

    for (let i = 0; i < data.length; i += 4) {
      if (data[i + 3] === 0) {
        transparentCount++;
      }
    }

    const pct = Math.round((transparentCount / (width * height)) * 100);
    this.scratchPercentage.set(pct);

    if (pct >= 45) {
      this.hasScratchedEnough.set(true);
      ctx.clearRect(0, 0, width, height);
      this.triggerRedeem();
    }
  }

  triggerRedeem(): void {
    this.isRedeeming.set(true);
    const tier = this.activeCardTier();

    this.rewardService.redeemRewards(tier).subscribe({
      next: (res) => {
        this.isRedeeming.set(false);
        if (res.success) {
          this.revealedPrize.set(res.rewardItem);
          this.authService.showToast('Reward claimed successfully!', 'success');
          this.fetchRewards();
        } else {
          this.authService.showToast('Failed to claim reward. Please try again.', 'danger');
          this.closeModal();
        }
      },
      error: (err) => {
        this.isRedeeming.set(false);
        this.authService.showToast(err.error?.message || 'Error claiming reward.', 'danger');
        this.closeModal();
      }
    });
  }

  get pointsLeftToNextTier(): number {
    const points = this.rewardSummary()?.totalPoints || 0;
    if (points < 500) {
      return 500 - points;
    } else if (points < 1000) {
      return 1000 - points;
    }
    return 0;
  }
}
