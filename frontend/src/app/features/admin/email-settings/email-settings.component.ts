import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { EmailSettings, EmailSettingsUpdate } from '../../../core/models/email-settings.model';
import { AdminService } from '../../../core/services/admin.service';

const EMAIL_RE = /^[^@\s,;]+@[^@\s,;]+\.[^@\s,;]+$/;

function splitAddresses(raw: string | null): string[] {
  return (raw ?? '').split(/[,;\s]+/).filter(a => a);
}

function addressListValidator(control: AbstractControl): ValidationErrors | null {
  const invalid = splitAddresses(control.value).filter(a => !EMAIL_RE.test(a));
  return invalid.length ? { addressList: invalid.join(', ') } : null;
}

const GMAIL_DEFAULTS = { smtp_host: 'smtp.gmail.com', smtp_port: 587, smtp_security: 'starttls' } as const;

@Component({
  selector: 'app-email-settings',
  standalone: true,
  imports: [
    DatePipe, ReactiveFormsModule,
    MatButtonModule, MatCardModule, MatFormFieldModule, MatIconModule, MatInputModule,
    MatProgressBarModule, MatRadioModule, MatSelectModule, MatSlideToggleModule, MatTooltipModule,
  ],
  template: `
    @if (loading()) {
      <mat-progress-bar mode="indeterminate" />
    }
    <div class="page-content">
      <h1>Admin</h1>

      @if (settings()) {
        <mat-card>
          <mat-card-header>
            <mat-icon mat-card-avatar>mail</mat-icon>
            <mat-card-title>Email Reminders</mat-card-title>
            <mat-card-subtitle>
              One email listing maintenance items across all vehicles, using the same statuses as the Status Report.
            </mat-card-subtitle>
          </mat-card-header>

          <mat-card-content>
            <form [formGroup]="form">
              <mat-slide-toggle formControlName="enabled">Send reminder emails</mat-slide-toggle>

              <h3>Reminder rules</h3>
              <div class="section">
                <mat-radio-group formControlName="threshold" class="radio-group">
                  <mat-radio-button value="overdue">Overdue items only</mat-radio-button>
                  <mat-radio-button value="due">Overdue and due-soon items</mat-radio-button>
                </mat-radio-group>
                <mat-form-field appearance="outline" class="narrow">
                  <mat-label>Send at most every</mat-label>
                  <input matInput type="number" formControlName="frequency_days" min="1" max="30" />
                  <span matTextSuffix>&nbsp;day(s)</span>
                  <mat-hint>1 = daily, 7 = weekly, 30 = monthly</mat-hint>
                  @if (form.controls.frequency_days.invalid) {
                    <mat-error>Enter a whole number from 1 to 30</mat-error>
                  }
                </mat-form-field>
                <p class="muted">
                  No email is sent when nothing matches. Once something matches, an email goes out at the
                  next hourly check, then repeats at this interval while items still match.
                </p>
              </div>

              <div class="section-header">
                <h3>Email account</h3>
                <button mat-stroked-button type="button" (click)="useGmailDefaults()">
                  <mat-icon>auto_fix_high</mat-icon> Use Gmail defaults
                </button>
              </div>
              <div class="grid">
                <mat-form-field appearance="outline">
                  <mat-label>From address</mat-label>
                  <input matInput formControlName="from_address" placeholder="you@gmail.com" />
                  @if (form.controls.from_address.hasError('required')) {
                    <mat-error>Required when reminders are enabled</mat-error>
                  } @else if (form.controls.from_address.invalid) {
                    <mat-error>Enter a valid email address</mat-error>
                  }
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>Send to</mat-label>
                  <input matInput formControlName="to_addresses" placeholder="you@gmail.com, partner@example.com" />
                  <mat-hint>Separate multiple addresses with commas</mat-hint>
                  @if (form.controls.to_addresses.hasError('required')) {
                    <mat-error>Required when reminders are enabled</mat-error>
                  } @else if (form.controls.to_addresses.hasError('addressList')) {
                    <mat-error>Invalid: {{ form.controls.to_addresses.getError('addressList') }}</mat-error>
                  }
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>SMTP host</mat-label>
                  <input matInput formControlName="smtp_host" placeholder="smtp.example.com" />
                  @if (form.controls.smtp_host.invalid) {
                    <mat-error>Required when reminders are enabled</mat-error>
                  }
                </mat-form-field>
                <div class="port-row">
                  <mat-form-field appearance="outline">
                    <mat-label>Port</mat-label>
                    <input matInput type="number" formControlName="smtp_port" />
                    @if (form.controls.smtp_port.invalid) {
                      <mat-error>1–65535</mat-error>
                    }
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>Security</mat-label>
                    <mat-select formControlName="smtp_security">
                      <mat-option value="starttls">STARTTLS (usually 587)</mat-option>
                      <mat-option value="ssl">SSL/TLS (usually 465)</mat-option>
                      <mat-option value="none">None</mat-option>
                    </mat-select>
                  </mat-form-field>
                </div>
                <mat-form-field appearance="outline">
                  <mat-label>SMTP username</mat-label>
                  <input matInput formControlName="smtp_username" />
                  <mat-hint>Leave blank if the server doesn't require login</mat-hint>
                </mat-form-field>
                <div class="password-status">
                  @if (settings()!.password_configured) {
                    <mat-icon class="ok">check_circle</mat-icon>
                    <span>SMTP password is set via the <code>SMTP_PASSWORD</code> environment variable.</span>
                  } @else {
                    <mat-icon class="warn">warning</mat-icon>
                    <span>
                      SMTP password not set. Set the <code>SMTP_PASSWORD</code> environment variable and restart the app.
                    </span>
                  }
                  @if (isGmail()) {
                    <span class="muted">
                      Gmail requires an <a href="https://myaccount.google.com/apppasswords" target="_blank" rel="noopener">app password</a>,
                      not your regular Google password.
                    </span>
                  }
                </div>
              </div>
            </form>

            <p class="muted">
              @if (settings()!.last_sent_at) {
                Last scheduled reminder sent {{ settings()!.last_sent_at | date:'medium' }}.
              } @else {
                No scheduled reminder has been sent yet.
              }
            </p>
          </mat-card-content>

          <mat-card-actions align="end">
            <span [matTooltip]="form.dirty ? 'Save your changes first' : ''">
              <button mat-button type="button" [disabled]="form.dirty" (click)="preview()">
                <mat-icon>open_in_new</mat-icon> Preview email
              </button>
            </span>
            <span [matTooltip]="form.dirty ? 'Save your changes first' : 'Sends now if any items match; does not change the schedule'">
              <button mat-button type="button" [disabled]="form.dirty || sending()" (click)="sendNow()">
                <mat-icon>send</mat-icon> Send email now
              </button>
            </span>
            <button mat-flat-button color="primary" type="button" [disabled]="form.invalid || !form.dirty || saving()" (click)="save()">
              Save
            </button>
          </mat-card-actions>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    form { padding-top: 16px; }
    h3 { margin: 24px 0 12px; font-weight: 500; }
    .section { display: flex; flex-direction: column; gap: 12px; }
    .section-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
    .section-header h3 { margin-bottom: 24px; }
    .radio-group { display: flex; flex-direction: column; gap: 4px; }
    .narrow { max-width: 260px; }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); column-gap: 16px; row-gap: 8px; }
    .grid mat-form-field { width: 100%; }
    .port-row { display: grid; grid-template-columns: 120px 1fr; gap: 12px; }
    .password-status { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; font-size: 14px; padding-bottom: 20px; }
    .password-status .ok { color: #2e7d32; }
    .password-status .warn { color: #e65100; }
    .muted { color: #757575; font-size: 14px; margin: 0; }
    mat-card-content > .muted { margin-top: 16px; }
    mat-card-actions { flex-wrap: wrap; gap: 8px; }
  `]
})
export class EmailSettingsComponent implements OnInit {
  private admin = inject(AdminService);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  loading = signal(true);
  saving = signal(false);
  sending = signal(false);
  settings = signal<EmailSettings | null>(null);

  form = this.fb.nonNullable.group({
    enabled: false,
    threshold: this.fb.nonNullable.control<EmailSettingsUpdate['threshold']>('due'),
    frequency_days: [7, [Validators.required, Validators.min(1), Validators.max(30), Validators.pattern(/^\d+$/)]],
    from_address: ['', Validators.email],
    to_addresses: ['', addressListValidator],
    smtp_host: [''],
    smtp_port: [587, [Validators.required, Validators.min(1), Validators.max(65535)]],
    smtp_security: this.fb.nonNullable.control<EmailSettingsUpdate['smtp_security']>('starttls'),
    smtp_username: [''],
  });

  ngOnInit() {
    // SMTP fields are only required once reminders are enabled, so users can
    // turn reminders off without filling anything in.
    this.form.controls.enabled.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(enabled => this.updateRequiredValidators(enabled));

    this.admin.getEmailSettings().subscribe({
      next: s => { this.applySettings(s); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  isGmail(): boolean {
    return this.form.controls.smtp_host.value.trim().toLowerCase() === GMAIL_DEFAULTS.smtp_host;
  }

  useGmailDefaults() {
    const { from_address, smtp_username } = this.form.getRawValue();
    this.form.patchValue({
      ...GMAIL_DEFAULTS,
      // Gmail authenticates with the full address it sends from
      smtp_username: smtp_username || from_address,
      from_address: from_address || smtp_username,
    });
    this.form.markAsDirty();
  }

  save() {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.admin.updateEmailSettings(this.form.getRawValue()).subscribe({
      next: s => {
        this.applySettings(s);
        this.saving.set(false);
        this.snackBar.open('Email settings saved.', 'Dismiss', { duration: 3000 });
      },
      error: () => this.saving.set(false),
    });
  }

  preview() {
    this.admin.openEmailPreview();
  }

  sendNow() {
    this.sending.set(true);
    this.admin.sendReminderNow().subscribe({
      next: r => {
        this.sending.set(false);
        this.snackBar.open(r.message, 'Dismiss', { duration: 6000 });
      },
      error: () => this.sending.set(false),
    });
  }

  private applySettings(s: EmailSettings) {
    this.settings.set(s);
    this.form.reset({
      enabled: s.enabled,
      threshold: s.threshold,
      frequency_days: s.frequency_days,
      from_address: s.from_address,
      to_addresses: s.to_addresses,
      smtp_host: s.smtp_host,
      smtp_port: s.smtp_port,
      smtp_security: s.smtp_security,
      smtp_username: s.smtp_username,
    });
    this.updateRequiredValidators(s.enabled);
  }

  private updateRequiredValidators(enabled: boolean) {
    const { from_address, to_addresses, smtp_host } = this.form.controls;
    for (const control of [from_address, to_addresses, smtp_host]) {
      if (enabled) {
        control.addValidators(Validators.required);
      } else {
        control.removeValidators(Validators.required);
      }
      control.updateValueAndValidity({ emitEvent: false });
    }
    // Show "required" errors immediately when the user flips the toggle on
    if (enabled) {
      [from_address, to_addresses, smtp_host].forEach(c => c.markAsTouched());
    }
  }
}
