import { Component, input } from '@angular/core';
import { NgClass } from '@angular/common';
import { DueStatus } from '../../../core/models/due-status.model';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [NgClass],
  template: `
    <span class="badge" [ngClass]="status()">{{ label() }}</span>
  `,
  styles: [`
    .badge {
      display: inline-block;
      padding: 3px 10px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .ok { background: #e8f5e9; color: #2e7d32; }
    .upcoming { background: #fff8e1; color: #f57f17; }
    .due { background: #fff3e0; color: #e65100; }
    .overdue { background: #ffebee; color: #c62828; }
    .never_performed { background: #eeeeee; color: #616161; }
  `]
})
export class StatusBadgeComponent {
  status = input.required<DueStatus>();

  label() {
    const map: Record<DueStatus, string> = {
      ok: 'OK',
      upcoming: 'Upcoming',
      due: 'Due',
      overdue: 'Overdue',
      never_performed: 'Never Done',
    };
    return map[this.status()];
  }
}
